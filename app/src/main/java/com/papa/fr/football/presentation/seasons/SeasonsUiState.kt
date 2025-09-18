package com.papa.fr.football.presentation.seasons

import com.papa.fr.football.domain.model.Season

data class SeasonsUiState(
    val isLoading: Boolean = false,
    val seasons: List<Season> = emptyList(),
    val errorMessage: String? = null
)
