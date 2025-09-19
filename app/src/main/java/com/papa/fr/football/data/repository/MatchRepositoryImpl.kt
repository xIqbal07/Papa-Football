package com.papa.fr.football.data.repository

import android.util.Base64
import com.papa.fr.football.data.mapper.toDomain
import com.papa.fr.football.data.remote.SeasonApiService
import com.papa.fr.football.data.remote.TeamApiService
import com.papa.fr.football.data.remote.TeamLogoRaw
import com.papa.fr.football.data.remote.dto.TeamLogoResponseDto
import com.papa.fr.football.domain.model.Match
import com.papa.fr.football.domain.repository.MatchRepository
import io.ktor.http.ContentType
import io.ktor.http.match
import io.ktor.http.withoutParameters
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class MatchRepositoryImpl(
    private val seasonApiService: SeasonApiService,
    private val teamApiService: TeamApiService,
) : MatchRepository {

    private val teamLogoCache = ConcurrentHashMap<Int, String>()
    private val logoMutex = Mutex()
    @Volatile
    private var lastLogoRequestAt: Long = 0L

    override suspend fun getUpcomingMatches(uniqueTournamentId: Int, seasonId: Int): List<Match> =
        coroutineScope {
            val events = seasonApiService
                .getSeasonEvents(uniqueTournamentId, seasonId)
                .data
                .events

            if (events.isEmpty()) {
                return@coroutineScope emptyList()
            }

            val teamIds = events.flatMap { event ->
                listOfNotNull(event.homeTeam?.id, event.awayTeam?.id)
            }.toSet()

            val logos = teamIds.associateWith { teamId ->
                async { fetchTeamLogo(teamId) }
            }.mapValues { (_, deferred) -> deferred.await() }

            events.map { event ->
                event.toDomain(
                    homeLogoBase64 = event.homeTeam?.id?.let { logos[it] },
                    awayLogoBase64 = event.awayTeam?.id?.let { logos[it] },
                )
            }
        }

    private suspend fun fetchTeamLogo(teamId: Int): String {
        teamLogoCache[teamId]?.let { return it }

        val sanitizedLogo = logoMutex.withLock {
            teamLogoCache[teamId]?.let { return it }

            val elapsed = System.currentTimeMillis() - lastLogoRequestAt
            if (elapsed < LOGO_REQUEST_INTERVAL_MS) {
                delay(LOGO_REQUEST_INTERVAL_MS - elapsed)
            }

            val sanitized = runCatching {
                teamApiService.getTeamLogo(teamId).toSanitizedBase64()
            }.getOrElse { "" }

            lastLogoRequestAt = System.currentTimeMillis()

            if (sanitized.isNotBlank()) {
                teamLogoCache[teamId] = sanitized
            }

            sanitized
        }

        return sanitizedLogo
    }

    private fun String?.sanitizeBase64(): String {
        if (this.isNullOrBlank()) return ""
        val delimiter = "base64,"
        val index = indexOf(delimiter)
        val trimmed = if (index >= 0) {
            substring(index + delimiter.length)
        } else {
            this
        }
        return trimmed.trim()
    }

    private fun TeamLogoRaw.toSanitizedBase64(): String {
        val normalizedContentType = contentType?.withoutParameters()

        if (normalizedContentType != null && normalizedContentType.match(ContentType.Image.Any)) {
            return body.encodeToBase64()
        }

        val rawText = body.decodeToString().trim()
        if (rawText.isEmpty()) return ""

        if (normalizedContentType == ContentType.Application.Json || rawText.startsWith("{")) {
            val base64FromJson = runCatching {
                json.decodeFromString<TeamLogoResponseDto>(rawText).data
            }.getOrNull()
            base64FromJson?.sanitizeBase64()?.takeIf { it.isLikelyBase64() }?.let { return it }
        }

        val sanitized = rawText.sanitizeBase64()
        if (sanitized.isLikelyBase64()) {
            return sanitized
        }

        return body.encodeToBase64()
    }

    private fun ByteArray.encodeToBase64(): String {
        if (isEmpty()) return ""
        return Base64.encodeToString(this, Base64.NO_WRAP)
    }

    private fun String.isLikelyBase64(): Boolean {
        if (isBlank() || length < MIN_BASE64_LENGTH) return false
        val candidate = replace("\n", "").replace("\r", "")
        if (candidate.any { !it.isBase64Char() }) return false
        return true
    }

    private fun Char.isBase64Char(): Boolean {
        return isLetterOrDigit() || this == '+' || this == '/' || this == '=' || this == '-' || this == '_'
    }

    private companion object {
        private const val LOGO_REQUEST_INTERVAL_MS = 650L
        private const val MIN_BASE64_LENGTH = 32
        private val json = Json { ignoreUnknownKeys = true }
    }
}
