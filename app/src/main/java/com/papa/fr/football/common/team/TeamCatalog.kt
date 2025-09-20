package com.papa.fr.football.common.team

import androidx.annotation.DrawableRes
import com.papa.fr.football.R

data class TeamDescriptor(
    val id: Int,
    val leagueId: Int,
    val name: String,
    @DrawableRes val logoRes: Int? = null,
)

interface TeamCatalog {
    fun teamsForLeague(leagueId: Int): List<TeamDescriptor>
    fun findTeam(teamId: Int): TeamDescriptor?
}

class StaticTeamCatalog : TeamCatalog {

    private val teamsByLeague: Map<Int, List<TeamDescriptor>> = listOf(
        TeamDescriptor(id = 541, leagueId = 8, name = "Real Madrid"),
        TeamDescriptor(id = 529, leagueId = 8, name = "Barcelona"),
        TeamDescriptor(id = 530, leagueId = 8, name = "Atlético Madrid"),
        TeamDescriptor(id = 548, leagueId = 8, name = "Real Sociedad"),
        TeamDescriptor(id = 50, leagueId = 17, name = "Manchester City"),
        TeamDescriptor(id = 42, leagueId = 17, name = "Arsenal"),
        TeamDescriptor(id = 40, leagueId = 17, name = "Liverpool"),
        TeamDescriptor(id = 38, leagueId = 17, name = "Chelsea"),
        TeamDescriptor(id = 157, leagueId = 35, name = "Bayern München"),
        TeamDescriptor(id = 165, leagueId = 35, name = "Borussia Dortmund"),
        TeamDescriptor(id = 173, leagueId = 35, name = "RB Leipzig"),
        TeamDescriptor(id = 168, leagueId = 35, name = "Bayer Leverkusen"),
        TeamDescriptor(id = 85, leagueId = 34, name = "Paris Saint-Germain"),
        TeamDescriptor(id = 81, leagueId = 34, name = "Olympique Marseille"),
        TeamDescriptor(id = 80, leagueId = 34, name = "Olympique Lyonnais"),
        TeamDescriptor(id = 91, leagueId = 34, name = "AS Monaco"),
        TeamDescriptor(id = 113, leagueId = 23, name = "Juventus"),
        TeamDescriptor(id = 98, leagueId = 23, name = "AC Milan"),
        TeamDescriptor(id = 110, leagueId = 23, name = "Inter"),
        TeamDescriptor(id = 115, leagueId = 23, name = "Napoli"),
        TeamDescriptor(id = 194, leagueId = 37, name = "Ajax"),
        TeamDescriptor(id = 197, leagueId = 37, name = "PSV"),
        TeamDescriptor(id = 195, leagueId = 37, name = "Feyenoord"),
        TeamDescriptor(id = 190, leagueId = 37, name = "AZ Alkmaar"),
    ).groupBy { it.leagueId }.mapValues { entry ->
        entry.value.map { team ->
            team.copy(logoRes = team.logoRes ?: R.drawable.ic_team_placeholder)
        }
    }

    private val teamsById: Map<Int, TeamDescriptor> = teamsByLeague.values.flatten().associateBy { it.id }

    override fun teamsForLeague(leagueId: Int): List<TeamDescriptor> {
        return teamsByLeague[leagueId].orEmpty()
    }

    override fun findTeam(teamId: Int): TeamDescriptor? {
        return teamsById[teamId]
    }
}
