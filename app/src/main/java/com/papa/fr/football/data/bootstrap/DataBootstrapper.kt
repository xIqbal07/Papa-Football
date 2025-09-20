package com.papa.fr.football.data.bootstrap

import com.papa.fr.football.common.league.LeagueCatalog
import com.papa.fr.football.common.schedule.ScheduleConfig
import com.papa.fr.football.domain.repository.MatchRepository
import com.papa.fr.football.domain.repository.SeasonRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class DataBootstrapper(
    private val seasonRepository: SeasonRepository,
    private val matchRepository: MatchRepository,
    private val leagueCatalog: LeagueCatalog,
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
        leagueCatalog.leagues.forEach { league ->
            val seasons = runCatching {
                seasonRepository.getUniqueTournamentSeasons(league.id, forceRefresh)
            }.getOrElse { return@forEach }

            val futureSeasons = seasons.take(ScheduleConfig.FUTURE_SEASON_LIMIT)
            futureSeasons.forEach { season ->
                runCatching {
                    matchRepository.warmUpcomingMatches(league.id, season.id, forceRefresh)
                }
            }

            val pastSeasons = seasons.take(ScheduleConfig.PAST_SEASON_LIMIT)
            pastSeasons.forEach { season ->
                runCatching {
                    matchRepository.warmRecentMatches(league.id, season.id, forceRefresh)
                }
            }
        }

        runCatching {
            matchRepository.warmLiveMatches(ScheduleConfig.LIVE_SPORT_ID, forceRefresh)
        }
    }
}
