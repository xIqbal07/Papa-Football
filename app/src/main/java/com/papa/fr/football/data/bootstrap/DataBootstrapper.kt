package com.papa.fr.football.data.bootstrap

import com.papa.fr.football.common.league.LeagueCatalog
import com.papa.fr.football.common.schedule.ScheduleConfig
import com.papa.fr.football.domain.repository.MatchRepository
import com.papa.fr.football.domain.repository.SeasonRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.collections.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean

class DataBootstrapper(
    private val seasonRepository: SeasonRepository,
    private val matchRepository: MatchRepository,
    private val leagueCatalog: LeagueCatalog,
    private val matchPrefetchQueue: MatchPrefetchQueue,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    private val hasStarted = AtomicBoolean(false)

    fun prefetchAllData(forceRefresh: Boolean = false) {
        if (!forceRefresh && !hasStarted.compareAndSet(false, true)) {
            return
        }

        scope.launch {
            runCatching { warmCaches(forceRefresh) }
        }
    }

    private suspend fun warmCaches(forceRefresh: Boolean) {
        val prioritizedSeasons = mutableListOf<LeagueSeason>()
        val futureSeasonQueues = mutableListOf<LeagueSeasonQueue>()
        val pastSeasonQueues = mutableListOf<LeagueSeasonQueue>()

        leagueCatalog.leagues.forEach { league ->
            val seasons = runCatching {
                seasonRepository.getUniqueTournamentSeasons(league.id, forceRefresh)
            }.getOrElse { return@forEach }

            val prioritizedSeason = seasons.firstOrNull()

            if (prioritizedSeason != null) {
                prioritizedSeasons += LeagueSeason(league.id, prioritizedSeason.id)
            }

            val remainingSeasons = if (prioritizedSeason != null) {
                seasons.filterNot { it.id == prioritizedSeason.id }
            } else {
                seasons
            }

            val futureSeasons = remainingSeasons.take(ScheduleConfig.FUTURE_SEASON_LIMIT)
            if (futureSeasons.isNotEmpty()) {
                futureSeasonQueues += LeagueSeasonQueue(
                    leagueId = league.id,
                    seasonIds = ArrayDeque(futureSeasons.map { it.id }),
                )
            }

            val pastSeasons = remainingSeasons.take(ScheduleConfig.PAST_SEASON_LIMIT)
            if (pastSeasons.isNotEmpty()) {
                pastSeasonQueues += LeagueSeasonQueue(
                    leagueId = league.id,
                    seasonIds = ArrayDeque(pastSeasons.map { it.id }),
                )
            }
        }

        prioritizedSeasons.forEach { prioritized ->
            runCatching {
                matchRepository.warmUpcomingMatches(
                    uniqueTournamentId = prioritized.leagueId,
                    seasonId = prioritized.seasonId,
                    forceRefresh = forceRefresh,
                    prefetchLogos = true,
                )
            }
        }

        prioritizedSeasons.forEach { prioritized ->
            runCatching {
                matchRepository.warmRecentMatches(
                    uniqueTournamentId = prioritized.leagueId,
                    seasonId = prioritized.seasonId,
                    forceRefresh = forceRefresh,
                    prefetchLogos = true,
                )
            }
        }

        enqueueRoundRobin(futureSeasonQueues) { leagueId, seasonId ->
            matchPrefetchQueue.enqueueUpcoming(
                leagueId = leagueId,
                seasonId = seasonId,
                forceRefresh = forceRefresh,
            )
        }

        enqueueRoundRobin(pastSeasonQueues) { leagueId, seasonId ->
            matchPrefetchQueue.enqueuePast(
                leagueId = leagueId,
                seasonId = seasonId,
                forceRefresh = forceRefresh,
            )
        }

        runCatching {
            matchRepository.warmLiveMatches(
                ScheduleConfig.LIVE_SPORT_ID,
                forceRefresh = forceRefresh,
                prefetchLogos = false,
            )
        }
    }

    private suspend fun enqueueRoundRobin(
        queues: List<LeagueSeasonQueue>,
        enqueue: suspend (leagueId: Int, seasonId: Int) -> Unit,
    ) {
        if (queues.isEmpty()) {
            return
        }

        var didSchedule: Boolean
        do {
            didSchedule = false
            queues.forEach { queue ->
                val seasonId = queue.seasonIds.removeFirstOrNull() ?: return@forEach

                try {
                    enqueue(queue.leagueId, seasonId)
                } catch (_: Throwable) {
                    // Ignore enqueue failures so other leagues continue scheduling.
                }

                didSchedule = true
            }
        } while (didSchedule)
    }

    private data class LeagueSeasonQueue(
        val leagueId: Int,
        val seasonIds: ArrayDeque<Int>,
    )

    private data class LeagueSeason(
        val leagueId: Int,
        val seasonId: Int,
    )

    private fun <T> ArrayDeque<T>.removeFirstOrNull(): T? = if (isEmpty()) null else removeFirst()
}
