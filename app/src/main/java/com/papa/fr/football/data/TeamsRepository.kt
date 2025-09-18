package com.papa.fr.football.data

import com.papa.fr.football.R
import com.papa.fr.football.common.dropdown.LeagueItem

object TeamsRepository {

    private val premierLeagueTeams = listOf(
        Team(
            id = "chelsea",
            name = "Chelsea",
            emblemRes = null,
            players = listOf(
                Player("robert_sanchez", 1, "Robert Sánchez", 27, 0, 0, 0),
                Player("filip_jorgensen", 12, "Filip Jörgensen", 23, 0, 0, 1),
                Player("marcus_bettinelli", 37, "Marcus Bettinelli", 31, 0, 1, 0),
                Player("eddie_beach", 8, "Eddie Beach", 20, 0, 0, 0)
            )
        ),
        Team(
            id = "manchester_city",
            name = "Manchester City",
            emblemRes = null,
            players = listOf(
                Player("erling_haaland", 9, "Erling Haaland", 24, 8, 2, 1),
                Player("phil_foden", 47, "Phil Foden", 25, 4, 5, 0),
                Player("kevin_de_bruyne", 17, "Kevin De Bruyne", 33, 3, 7, 2)
            )
        ),
        Team(
            id = "arsenal",
            name = "Arsenal",
            emblemRes = null,
            players = listOf(
                Player("bukayo_saka", 7, "Bukayo Saka", 23, 5, 4, 1),
                Player("declan_rice", 41, "Declan Rice", 26, 2, 3, 3),
                Player("gabriel_martinelli", 11, "Gabriel Martinelli", 24, 3, 2, 0)
            )
        ),
        Team(
            id = "tottenham",
            name = "Tottenham",
            emblemRes = null,
            players = listOf(
                Player("son_heung_min", 7, "Son Heung-min", 32, 6, 3, 1),
                Player("maddison", 10, "James Maddison", 28, 3, 6, 2),
                Player("christian_romero", 17, "Cristian Romero", 27, 2, 1, 5)
            )
        ),
        Team(
            id = "liverpool",
            name = "Liverpool",
            emblemRes = null,
            players = listOf(
                Player("mohamed_salah", 11, "Mohamed Salah", 33, 7, 5, 0),
                Player("virgil_van_dijk", 4, "Virgil van Dijk", 34, 1, 1, 2),
                Player("trent_alexander_arnold", 66, "Trent Alexander-Arnold", 26, 1, 6, 1)
            )
        ),
        Team(
            id = "aston_villa",
            name = "Aston Villa",
            emblemRes = null,
            players = listOf(
                Player("ollie_watkins", 11, "Ollie Watkins", 29, 5, 3, 1),
                Player("moussa_diaby", 19, "Moussa Diaby", 25, 3, 2, 0),
                Player("emiliano_martinez", 1, "Emiliano Martínez", 33, 0, 0, 2)
            )
        ),
        Team(
            id = "brighton",
            name = "Brighton",
            emblemRes = null,
            players = listOf(
                Player("lewis_dunk", 5, "Lewis Dunk", 33, 2, 1, 4),
                Player("karou_mitoma", 22, "Kaoru Mitoma", 27, 4, 2, 2),
                Player("joao_pedro", 9, "João Pedro", 23, 5, 1, 3)
            )
        ),
        Team(
            id = "brentford",
            name = "Brentford",
            emblemRes = null,
            players = listOf(
                Player("ivan_toney", 17, "Ivan Toney", 29, 6, 2, 5),
                Player("bryan_mbeumo", 19, "Bryan Mbeumo", 26, 4, 4, 1),
                Player("mark_flekken", 1, "Mark Flekken", 32, 0, 0, 2)
            )
        )
    )

    private val laLigaTeams = listOf(
        Team(
            id = "real_madrid",
            name = "Real Madrid",
            emblemRes = null,
            players = listOf(
                Player("vinicius_junior", 7, "Vinícius Júnior", 25, 6, 5, 2),
                Player("jude_bellingham", 5, "Jude Bellingham", 22, 9, 3, 3),
                Player("rodrygo", 11, "Rodrygo", 24, 4, 4, 1)
            )
        ),
        Team(
            id = "barcelona",
            name = "Barcelona",
            emblemRes = null,
            players = listOf(
                Player("lewandowski", 9, "Robert Lewandowski", 36, 8, 3, 2),
                Player("pedri", 8, "Pedri", 23, 2, 6, 1),
                Player("raphinha", 11, "Raphinha", 29, 3, 4, 2)
            )
        ),
        Team(
            id = "girona",
            name = "Girona",
            emblemRes = null,
            players = listOf(
                Player("artem_dovbyk", 9, "Artem Dovbyk", 27, 7, 2, 1),
                Player("savio", 16, "Sávio", 20, 4, 5, 3),
                Player("viktor_tsiganov", 21, "Viktor Tsygankov", 27, 3, 3, 2)
            )
        )
    )

    private val serieATeams = listOf(
        Team(
            id = "inter",
            name = "Inter",
            emblemRes = null,
            players = listOf(
                Player("lautaro_martinez", 10, "Lautaro Martínez", 28, 9, 2, 1),
                Player("nicolo_barella", 23, "Nicolò Barella", 28, 2, 6, 3),
                Player("federico_dimarco", 32, "Federico Dimarco", 27, 3, 4, 2)
            )
        ),
        Team(
            id = "juventus",
            name = "Juventus",
            emblemRes = null,
            players = listOf(
                Player("dusan_vlahovic", 9, "Dušan Vlahović", 25, 7, 1, 2),
                Player("federico_chiesa", 7, "Federico Chiesa", 28, 4, 3, 1),
                Player("adrien_rabiot", 25, "Adrien Rabiot", 30, 2, 4, 4)
            )
        ),
        Team(
            id = "ac_milan",
            name = "AC Milan",
            emblemRes = null,
            players = listOf(
                Player("rafael_leao", 10, "Rafael Leão", 26, 5, 4, 2),
                Player("olivier_giroud", 9, "Olivier Giroud", 38, 6, 3, 1),
                Player("theo_hernandez", 19, "Theo Hernández", 28, 3, 5, 4)
            )
        )
    )

    private val teamsByLeague = mapOf(
        PREMIER_LEAGUE to premierLeagueTeams,
        LA_LIGA to laLigaTeams,
        SERIE_A to serieATeams
    )

    fun getSeasons(): List<LeagueItem> = listOf(
        LeagueItem("2024_2025", "2024/2025", R.drawable.ic_season),
        LeagueItem("2023_2024", "2023/2024", R.drawable.ic_season),
        LeagueItem("2022_2023", "2022/2023", R.drawable.ic_season)
    )

    fun getLeagues(): List<LeagueItem> = listOf(
        LeagueItem(PREMIER_LEAGUE, "Premier League", R.drawable.ic_league_premier),
        LeagueItem(LA_LIGA, "La Liga", R.drawable.ic_league_laliga),
        LeagueItem(SERIE_A, "Serie A", R.drawable.ic_league_seriea)
    )

    fun getTeamsForLeague(leagueId: String): List<Team> = teamsByLeague[leagueId].orEmpty()

    fun getTeam(leagueId: String, teamId: String): Team? =
        teamsByLeague[leagueId]?.firstOrNull { it.id == teamId }

    const val PREMIER_LEAGUE = "premier_league"
    const val LA_LIGA = "la_liga"
    const val SERIE_A = "serie_a"
}
