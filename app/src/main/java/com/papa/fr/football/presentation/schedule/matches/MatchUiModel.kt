package com.papa.fr.football.presentation.schedule.matches

sealed class MatchUiModel(open val id: String) {

    abstract fun withFavoriteTeams(favoriteTeamIds: Set<Int>): MatchUiModel

    data class Future(
        override val id: String,
        val homeTeamId: Int,
        val homeTeamName: String,
        val awayTeamId: Int,
        val awayTeamName: String,
        val startTimestamp: Long,
        val startDateLabel: String,
        val startTimeLabel: String,
        val isToday: Boolean,
        val homeLogoBase64: String,
        val awayLogoBase64: String,
        val isHomeTeamFavorite: Boolean,
        val isAwayTeamFavorite: Boolean,
        val odds: Odds? = null,
    ) : MatchUiModel(id) {

        override fun withFavoriteTeams(favoriteTeamIds: Set<Int>): Future {
            return copy(
                isHomeTeamFavorite = favoriteTeamIds.contains(homeTeamId),
                isAwayTeamFavorite = favoriteTeamIds.contains(awayTeamId),
            )
        }
    }

    data class Live(
        override val id: String,
        val homeTeamId: Int,
        val homeTeamName: String,
        val awayTeamId: Int,
        val awayTeamName: String,
        val homeScore: Int,
        val awayScore: Int,
        val homeLogoBase64: String,
        val awayLogoBase64: String,
        val statusLabel: String,
        val isHomeTeamFavorite: Boolean,
        val isAwayTeamFavorite: Boolean,
    ) : MatchUiModel(id) {

        val scoreLabel: String
            get() = "$homeScore:$awayScore"

        override fun withFavoriteTeams(favoriteTeamIds: Set<Int>): Live {
            return copy(
                isHomeTeamFavorite = favoriteTeamIds.contains(homeTeamId),
                isAwayTeamFavorite = favoriteTeamIds.contains(awayTeamId),
            )
        }
    }

    data class Past(
        override val id: String,
        val homeTeamId: Int,
        val homeTeamName: String,
        val awayTeamId: Int,
        val awayTeamName: String,
        val startDateLabel: String,
        val scoreLabel: String,
        val homeLogoBase64: String,
        val awayLogoBase64: String,
        val isHomeTeamFavorite: Boolean,
        val isAwayTeamFavorite: Boolean,
    ) : MatchUiModel(id) {

        override fun withFavoriteTeams(favoriteTeamIds: Set<Int>): Past {
            return copy(
                isHomeTeamFavorite = favoriteTeamIds.contains(homeTeamId),
                isAwayTeamFavorite = favoriteTeamIds.contains(awayTeamId),
            )
        }
    }

    data class Odds(
        val home: String,
        val draw: String,
        val away: String,
    )
}
