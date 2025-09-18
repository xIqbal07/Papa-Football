package com.papa.fr.football.presentation.seasons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.papa.fr.football.domain.usecase.GetSeasonsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SeasonsViewModel(
    private val getSeasonsUseCase: GetSeasonsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeasonsUiState())
    val uiState: StateFlow<SeasonsUiState> = _uiState.asStateFlow()

    fun loadSeasons(uniqueTournamentId: Int) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            runCatching { getSeasonsUseCase(uniqueTournamentId) }
                .onSuccess { seasons ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            seasons = seasons,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message.orEmpty()
                        )
                    }
                }
        }
    }
}
