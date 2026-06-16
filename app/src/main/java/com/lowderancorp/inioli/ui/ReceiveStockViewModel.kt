package com.lowderancorp.inioli.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lowderancorp.inioli.data.stockjourney.DEFAULT_RECEIVE_STOCK_MOVEMENT_TYPE
import com.lowderancorp.inioli.data.stockjourney.MovementType
import com.lowderancorp.inioli.data.stockjourney.StockJourneyItem
import com.lowderancorp.inioli.data.stockjourney.StockJourneyRepository
import com.lowderancorp.inioli.data.stockjourney.resolveMovementTypeCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReceiveStockUiState(
    val movementTypes: List<MovementType> = emptyList(),
    val selectedMovementTypeCode: String = DEFAULT_RECEIVE_STOCK_MOVEMENT_TYPE,
    val items: List<StockJourneyItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val selectedMovementType: MovementType?
        get() = movementTypes.firstOrNull { movementType ->
            movementType.code == selectedMovementTypeCode
        }
}

class ReceiveStockViewModel(
    private val repository: StockJourneyRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReceiveStockUiState())
    val uiState: StateFlow<ReceiveStockUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        loadStockData(
            requestedMovementTypeCode = _uiState.value.selectedMovementTypeCode,
            reloadMovementTypes = true,
            clearItems = false
        )
    }

    fun onMovementTypeSelected(movementTypeCode: String) {
        val currentState = _uiState.value
        if (currentState.isLoading || movementTypeCode == currentState.selectedMovementTypeCode) {
            return
        }

        loadStockData(
            requestedMovementTypeCode = movementTypeCode,
            reloadMovementTypes = false,
            clearItems = true
        )
    }

    private fun loadStockData(
        requestedMovementTypeCode: String,
        reloadMovementTypes: Boolean,
        clearItems: Boolean
    ) {
        if (_uiState.value.isLoading) return

        _uiState.update { state ->
            state.copy(
                selectedMovementTypeCode = requestedMovementTypeCode,
                items = if (clearItems) emptyList() else state.items,
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val movementTypes = if (reloadMovementTypes || _uiState.value.movementTypes.isEmpty()) {
                    repository.getMovementTypes()
                } else {
                    _uiState.value.movementTypes
                }

                val resolvedMovementTypeCode = resolveMovementTypeCode(
                    requestedMovementTypeCode = requestedMovementTypeCode,
                    movementTypes = movementTypes
                )

                val items = repository.getStockJourneyByMovementType(
                    movementTypeCode = resolvedMovementTypeCode
                )
                _uiState.update { state ->
                    state.copy(
                        movementTypes = movementTypes,
                        selectedMovementTypeCode = resolvedMovementTypeCode,
                        items = items,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (exception: Throwable) {
                if (exception is CancellationException) throw exception
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = exception.toStockJourneyUserMessage(
                            targetLabel = "stock movements"
                        )
                    )
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ReceiveStockViewModel(
                    repository = inioliApplication().container.stockJourneyRepository
                )
            }
        }
    }
}
