package com.papa.fr.football.common.schedule

/**
 * Centralizes configurable values used by the schedule feature so that data and presentation
 * layers share the same assumptions when prefetching content.
 */
object ScheduleConfig {
    const val LIVE_SPORT_ID: Int = 1
    const val FUTURE_SEASON_LIMIT: Int = 1
    const val PAST_SEASON_LIMIT: Int = 5
}
