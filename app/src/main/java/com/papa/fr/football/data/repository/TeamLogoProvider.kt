package com.papa.fr.football.data.repository

import android.util.Base64
import com.papa.fr.football.data.remote.TeamApiService
import com.papa.fr.football.data.remote.TeamLogoRaw
import com.papa.fr.football.data.remote.dto.TeamLogoResponseDto
import io.ktor.http.ContentType
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class TeamLogoProvider(
    private val teamApiService: TeamApiService,
    private val timeProvider: () -> Long = System::currentTimeMillis,
) {

    private val teamLogoCache = ConcurrentHashMap<Int, String>()
    private val logoMutex = Mutex()

    @Volatile
    private var nextLogoRequestAt: Long = 0L

    suspend fun getTeamLogo(teamId: Int): String {
        teamLogoCache[teamId]?.let { return it }

        var cachedLogo: String? = null
        var waitDurationMs = 0L

        logoMutex.withLock {
            cachedLogo = teamLogoCache[teamId]
            if (cachedLogo != null) {
                return@withLock
            }

            val now = timeProvider()
            val scheduledStart = maxOf(now, nextLogoRequestAt)
            waitDurationMs = (scheduledStart - now).coerceAtLeast(0L)
            nextLogoRequestAt = scheduledStart + LOGO_REQUEST_INTERVAL_MS
        }

        cachedLogo?.let { return it }

        if (waitDurationMs > 0L) {
            delay(waitDurationMs)
        }

        val sanitizedLogo = runCatching {
            teamApiService.getTeamLogo(teamId).toSanitizedBase64()
        }.getOrElse { "" }

        if (sanitizedLogo.isNotBlank()) {
            teamLogoCache[teamId] = sanitizedLogo
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
        return !candidate.any { !it.isBase64Char() }
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
