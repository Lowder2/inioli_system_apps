package com.lowderancorp.inioli.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lowderancorp.inioli.data.stockjourney.StockJourneyDetail
import com.lowderancorp.inioli.data.stockjourney.StockJourneyDetailItem
import com.lowderancorp.inioli.data.stockjourney.StockJourneyRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReceiveStockDetailUiState(
    val detail: StockJourneyDetail? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val scannedBarcode: String? = null,
    val matchedItemId: Int? = null,
    val scannedQuantityByItemId: Map<Int, Int> = emptyMap(),
    val isScannerArmed: Boolean = true
) {
    val matchedItem: StockJourneyDetailItem?
        get() = detail?.items?.firstOrNull { item -> item.id == matchedItemId }

    val totalScannedQuantity: Int
        get() = scannedQuantityByItemId.values.sum()
}

class ReceiveStockDetailViewModel(
    private val repository: StockJourneyRepository,
    private val stockJourneyId: Int
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReceiveStockDetailUiState())
    val uiState: StateFlow<ReceiveStockDetailUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_uiState.value.isLoading) return

        _uiState.update { state ->
            state.copy(
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val detail = repository.getStockJourneyDetail(
                    stockJourneyId = stockJourneyId
                )
                _uiState.update { state ->
                    state.copy(
                        detail = detail,
                        isLoading = false,
                        errorMessage = null,
                        matchedItemId = findMatchingItemId(
                            detail = detail,
                            barcode = state.scannedBarcode
                        )
                    )
                }
            } catch (exception: Throwable) {
                if (exception is CancellationException) throw exception
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = exception.toStockJourneyUserMessage(
                            targetLabel = "stock movement detail"
                        )
                    )
                }
            }
        }
    }

    fun onBarcodeScanned(barcode: String) {
        val cleanBarcode = barcode.trim()
        val currentState = _uiState.value
        if (cleanBarcode.isBlank() || !currentState.isScannerArmed) return

        val matchedItemId = findMatchingItemId(
            detail = currentState.detail,
            barcode = cleanBarcode
        )

        _uiState.update { state ->
            state.copy(
                scannedBarcode = cleanBarcode,
                matchedItemId = matchedItemId,
                scannedQuantityByItemId = if (matchedItemId == null) {
                    state.scannedQuantityByItemId
                } else {
                    state.scannedQuantityByItemId.increment(matchedItemId)
                },
                isScannerArmed = false
            )
        }
    }

    fun onBarcodeCleared() {
        _uiState.update { state ->
            if (state.isScannerArmed) {
                state
            } else {
                state.copy(isScannerArmed = true)
            }
        }
    }

    private fun findMatchingItemId(
        detail: StockJourneyDetail?,
        barcode: String?
    ): Int? {
        val cleanBarcode = barcode?.trim().orEmpty()
        if (cleanBarcode.isBlank()) return null

        return detail?.items?.firstOrNull { item ->
            item.barcode.equals(cleanBarcode, ignoreCase = true)
        }?.id
    }

    private fun Map<Int, Int>.increment(itemId: Int): Map<Int, Int> {
        return this + (itemId to ((this[itemId] ?: 0) + 1))
    }

    companion object {
        fun Factory(
            stockJourneyId: Int
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ReceiveStockDetailViewModel(
                    repository = inioliApplication().container.stockJourneyRepository,
                    stockJourneyId = stockJourneyId
                )
            }
        }
    }
}
