package com.lowderancorp.inioli.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lowderancorp.inioli.InioliApplication
import com.lowderancorp.inioli.data.stockjourney.MovementType
import com.lowderancorp.inioli.data.stockjourney.StockJourneyException
import com.lowderancorp.inioli.data.stockjourney.StockJourneyItem
import com.lowderancorp.inioli.data.stockjourney.StockJourneyRepository
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val RECEIVE_STOCK_MOVEMENT_TYPE = "WAREHOUSE_TO_STORE"

data class ReceiveStockUiState(
    val movementTypes: List<MovementType> = emptyList(),
    val selectedMovementTypeCode: String = RECEIVE_STOCK_MOVEMENT_TYPE,
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
    private val repository: StockJourneyRepository,
    private val accessToken: String
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
                    repository.getMovementTypes(accessToken = accessToken)
                } else {
                    _uiState.value.movementTypes
                }

                val resolvedMovementTypeCode = resolveMovementTypeCode(
                    requestedMovementTypeCode = requestedMovementTypeCode,
                    movementTypes = movementTypes
                )

                val items = repository.getStockJourneyByMovementType(
                    accessToken = accessToken,
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
                        errorMessage = exception.toUserMessage()
                    )
                }
            }
        }
    }

    private fun resolveMovementTypeCode(
        requestedMovementTypeCode: String,
        movementTypes: List<MovementType>
    ): String {
        return when {
            movementTypes.any { it.code == requestedMovementTypeCode } -> requestedMovementTypeCode
            movementTypes.any { it.code == RECEIVE_STOCK_MOVEMENT_TYPE } -> RECEIVE_STOCK_MOVEMENT_TYPE
            else -> movementTypes.firstOrNull()?.code ?: requestedMovementTypeCode
        }
    }

    private fun Throwable.toUserMessage(): String {
        return when (this) {
            is SocketTimeoutException -> {
                "The stock movement server took too long to respond. Please try again."
            }

            is StockJourneyException -> {
                message ?: "Unable to load stock movements."
            }

            is IOException -> {
                "Unable to reach the stock movement server. When testing on the Android emulator, use 10.0.2.2 instead of localhost."
            }

            else -> {
                "Something went wrong while loading stock movements. Please try again."
            }
        }
    }

    companion object {
        fun Factory(accessToken: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ReceiveStockViewModel(
                    repository = inioliApplication().container.stockJourneyRepository,
                    accessToken = accessToken
                )
            }
        }
    }
}

private fun CreationExtras.inioliApplication(): InioliApplication {
    return this[APPLICATION_KEY] as InioliApplication
}
