package com.papa.fr.football.data.bootstrap

import com.papa.fr.football.data.local.dao.MatchPrefetchDao
import com.papa.fr.football.data.local.entity.MatchPrefetchTaskEntity
import com.papa.fr.football.data.local.entity.MatchTypeEntity
import com.papa.fr.football.domain.repository.MatchRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Coordinates match warm requests so heavy bootstrap passes can be spread over time. Tasks are
 * persisted which allows the queue to resume where it left off after process death while still
 * letting foreground requests bypass the queue when the user needs fresh data immediately.
 */
class MatchPrefetchQueue(
    private val matchRepository: MatchRepository,
    private val matchPrefetchDao: MatchPrefetchDao,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val idleDelayMillis: Long = 1_000,
    private val spacingDelayMillis: Long = 150,
    private val baseRetryDelayMillis: Long = 5_000,
) {
    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) {
            return
        }

        scope.launch { processQueue() }
    }

    suspend fun enqueueUpcoming(leagueId: Int, seasonId: Int, forceRefresh: Boolean) {
        enqueue(
            MatchPrefetchTaskEntity(
                leagueId = leagueId,
                seasonId = seasonId,
                type = MatchTypeEntity.UPCOMING,
                forceRefresh = forceRefresh,
                priority = if (forceRefresh) FORCE_PRIORITY else DEFAULT_PRIORITY,
            ),
        )
    }

    suspend fun enqueuePast(leagueId: Int, seasonId: Int, forceRefresh: Boolean) {
        enqueue(
            MatchPrefetchTaskEntity(
                leagueId = leagueId,
                seasonId = seasonId,
                type = MatchTypeEntity.PAST,
                forceRefresh = forceRefresh,
                priority = if (forceRefresh) FORCE_PRIORITY else DEFAULT_PRIORITY,
            ),
        )
    }

    private suspend fun enqueue(task: MatchPrefetchTaskEntity) {
        val inserted = matchPrefetchDao.enqueue(task)
        if (inserted == -1L) {
            matchPrefetchDao.promote(
                leagueId = task.leagueId,
                seasonId = task.seasonId,
                type = task.type,
                forceRefresh = task.forceRefresh,
                priority = task.priority,
                availableAfter = task.availableAfter,
            )
        }
    }

    private suspend fun processQueue() {
        while (scope.isActive) {
            val now = clock()
            val task = matchPrefetchDao.peekReady(now)
            if (task == null) {
                delay(idleDelayMillis)
                continue
            }

            try {
                execute(task)
                matchPrefetchDao.delete(task.id)
            } catch (cancellation: Throwable) {
                if (cancellation is CancellationException) {
                    throw cancellation
                }

                val nextDelay = baseRetryDelayMillis * (task.attempts.coerceAtLeast(0) + 1)
                matchPrefetchDao.reschedule(
                    id = task.id,
                    availableAfter = clock() + nextDelay,
                    attempts = task.attempts + 1,
                )
            }

            delay(spacingDelayMillis)
        }
    }

    private suspend fun execute(task: MatchPrefetchTaskEntity) {
        when (task.type) {
            MatchTypeEntity.UPCOMING -> matchRepository.warmUpcomingMatches(
                uniqueTournamentId = task.leagueId,
                seasonId = task.seasonId,
                forceRefresh = task.forceRefresh,
                prefetchLogos = false,
            )

            MatchTypeEntity.PAST -> matchRepository.warmRecentMatches(
                uniqueTournamentId = task.leagueId,
                seasonId = task.seasonId,
                forceRefresh = task.forceRefresh,
                prefetchLogos = false,
            )
        }
    }

    private companion object {
        const val FORCE_PRIORITY = 1
        const val DEFAULT_PRIORITY = 0
    }
}
