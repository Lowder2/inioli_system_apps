package com.lowderancorp.inioli.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lowderancorp.inioli.data.stockjourney.CloseStockJourneyPreview
import com.lowderancorp.inioli.data.stockjourney.StockJourneyDetail
import com.lowderancorp.inioli.data.stockjourney.StockJourneyDraftStore
import com.lowderancorp.inioli.data.stockjourney.StockJourneyDetailItem
import com.lowderancorp.inioli.data.stockjourney.StockJourneyRepository
import com.lowderancorp.inioli.data.stockjourney.buildCloseStockJourneyPreview
import com.lowderancorp.inioli.data.stockjourney.buildCloseStockJourneyRequest
import com.lowderancorp.inioli.data.stockjourney.willOverScan
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
    val isScannerArmed: Boolean = true,
    val closeNotes: String = "",
    val isClosing: Boolean = false,
    val closeErrorMessage: String? = null,
    val closeSuccessToken: Int = 0,
    val scanAcceptedToneToken: Int = 0,
    val overscanToneToken: Int = 0,
    val mutedOverscanProductIds: Set<Int> = emptySet(),
    val pendingOverscanItemId: Int? = null
) {
    val matchedItem: StockJourneyDetailItem?
        get() = detail?.items?.firstOrNull { item -> item.id == matchedItemId }

    val pendingOverscanItem: StockJourneyDetailItem?
        get() = detail?.items?.firstOrNull { item -> item.id == pendingOverscanItemId }

    val totalScannedQuantity: Int
        get() = scannedQuantityByItemId.values.sum()

    val closePreview: CloseStockJourneyPreview?
        get() = detail?.buildCloseStockJourneyPreview(
            scannedQuantityByItemId = scannedQuantityByItemId,
            notes = closeNotes
        )

    val canClose: Boolean
        get() = detail != null &&
            !isLoading &&
            !isClosing &&
            closePreview?.items?.isNotEmpty() == true

    val canReset: Boolean
        get() = detail != null &&
            !isLoading &&
            !isClosing &&
            (
                scannedQuantityByItemId.isNotEmpty() ||
                    closeNotes.isNotBlank() ||
                    mutedOverscanProductIds.isNotEmpty()
                )
}

class ReceiveStockDetailViewModel(
    private val repository: StockJourneyRepository,
    private val draftStore: StockJourneyDraftStore,
    private val stockJourneyId: Int
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        draftStore.getDraft(stockJourneyId).let { draft ->
            ReceiveStockDetailUiState(
                scannedQuantityByItemId = draft.scannedQuantityByItemId,
                closeNotes = draft.notes,
                mutedOverscanProductIds = draft.mutedOverscanProductIds
            )
        }
    )
    val uiState: StateFlow<ReceiveStockDetailUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_uiState.value.isLoading || _uiState.value.isClosing) return

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
        if (cleanBarcode.isBlank() || !currentState.isScannerArmed || currentState.isClosing) return

        val matchedItemId = findMatchingItemId(
            detail = currentState.detail,
            barcode = cleanBarcode
        )
        val matchedItem = currentState.detail
            ?.items
            ?.firstOrNull { item -> item.id == matchedItemId }

        if (matchedItem != null && matchedItem.willOverScan(
                currentScannedQuantity = currentState.scannedQuantityByItemId[matchedItem.id] ?: 0
            )
        ) {
            val shouldShowWarning = matchedItem.productId !in currentState.mutedOverscanProductIds
            if (!shouldShowWarning) {
                incrementScannedQuantity(
                    barcode = cleanBarcode,
                    matchedItemId = matchedItem.id,
                    useAcceptedTone = false,
                    useOverscanTone = true
                )
                return
            }

            _uiState.update { state ->
                state.copy(
                    scannedBarcode = cleanBarcode,
                    matchedItemId = matchedItem.id,
                    closeErrorMessage = null,
                    isScannerArmed = false,
                    overscanToneToken = state.overscanToneToken + 1,
                    pendingOverscanItemId = if (shouldShowWarning) matchedItem.id else null
                )
            }
            return
        }

        incrementScannedQuantity(
            barcode = cleanBarcode,
            matchedItemId = matchedItemId,
            useAcceptedTone = matchedItemId != null,
            useOverscanTone = false
        )
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

    fun onCloseNotesChange(notes: String) {
        _uiState.update { state ->
            state.copy(
                closeNotes = notes,
                closeErrorMessage = null
            )
        }
        draftStore.updateNotes(
            stockJourneyId = stockJourneyId,
            notes = notes
        )
    }

    fun confirmOverscanWarning(
        muteForProductInSession: Boolean
    ) {
        val currentState = _uiState.value
        val pendingOverscanItem = currentState.pendingOverscanItem
        if (pendingOverscanItem == null) return

        val updatedMutedProducts = if (muteForProductInSession) {
            currentState.mutedOverscanProductIds + pendingOverscanItem.productId
        } else {
            currentState.mutedOverscanProductIds
        }

        incrementScannedQuantity(
            barcode = currentState.scannedBarcode.orEmpty(),
            matchedItemId = pendingOverscanItem.id,
            useAcceptedTone = false,
            useOverscanTone = false,
            mutedOverscanProductIds = updatedMutedProducts,
            pendingOverscanItemId = null
        )

        if (updatedMutedProducts != currentState.mutedOverscanProductIds) {
            draftStore.replaceMutedOverscanProductIds(
                stockJourneyId = stockJourneyId,
                mutedOverscanProductIds = updatedMutedProducts
            )
        }
    }

    fun dismissOverscanWarning() {
        _uiState.update { state ->
            state.copy(
                pendingOverscanItemId = null
            )
        }
    }

    fun resetMovementDraft() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.isClosing) return

        draftStore.clearDraft(stockJourneyId)
        _uiState.update { state ->
            state.copy(
                scannedBarcode = null,
                matchedItemId = null,
                scannedQuantityByItemId = emptyMap(),
                isScannerArmed = false,
                closeNotes = "",
                closeErrorMessage = null,
                mutedOverscanProductIds = emptySet(),
                pendingOverscanItemId = null
            )
        }
    }

    fun closeStockJourney() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.isClosing) return

        val detail = currentState.detail ?: return
        val request = detail.buildCloseStockJourneyRequest(
            scannedQuantityByItemId = currentState.scannedQuantityByItemId,
            notes = currentState.closeNotes
        )

        if (request == null) {
            _uiState.update { state ->
                state.copy(
                    closeErrorMessage = "Scan at least one matching product before closing this movement."
                )
            }
            return
        }

        _uiState.update { state ->
            state.copy(
                isClosing = true,
                closeErrorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                repository.closeStockJourney(request)
                draftStore.clearDraft(stockJourneyId)
                _uiState.update { state ->
                    state.copy(
                        scannedBarcode = null,
                        matchedItemId = null,
                        scannedQuantityByItemId = emptyMap(),
                        isScannerArmed = true,
                        closeNotes = "",
                        isClosing = false,
                        closeErrorMessage = null,
                        closeSuccessToken = state.closeSuccessToken + 1,
                        mutedOverscanProductIds = emptySet(),
                        pendingOverscanItemId = null
                    )
                }
            } catch (exception: Throwable) {
                if (exception is CancellationException) throw exception
                _uiState.update { state ->
                    state.copy(
                        isClosing = false,
                        closeErrorMessage = exception.toStockJourneyCloseUserMessage()
                    )
                }
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

    private fun incrementScannedQuantity(
        barcode: String,
        matchedItemId: Int?,
        useAcceptedTone: Boolean,
        useOverscanTone: Boolean,
        mutedOverscanProductIds: Set<Int>? = null,
        pendingOverscanItemId: Int? = null
    ) {
        var updatedScannedQuantities = _uiState.value.scannedQuantityByItemId
        _uiState.update { state ->
            updatedScannedQuantities = if (matchedItemId == null) {
                state.scannedQuantityByItemId
            } else {
                state.scannedQuantityByItemId.increment(matchedItemId)
            }
            state.copy(
                scannedBarcode = barcode,
                matchedItemId = matchedItemId,
                scannedQuantityByItemId = updatedScannedQuantities,
                closeErrorMessage = null,
                scanAcceptedToneToken = if (useAcceptedTone) {
                    state.scanAcceptedToneToken + 1
                } else {
                    state.scanAcceptedToneToken
                },
                overscanToneToken = if (useOverscanTone) {
                    state.overscanToneToken + 1
                } else {
                    state.overscanToneToken
                },
                mutedOverscanProductIds = mutedOverscanProductIds ?: state.mutedOverscanProductIds,
                pendingOverscanItemId = pendingOverscanItemId,
                isScannerArmed = false
            )
        }
        draftStore.replaceScannedQuantities(
            stockJourneyId = stockJourneyId,
            scannedQuantityByItemId = updatedScannedQuantities
        )
    }

    companion object {
        fun Factory(
            stockJourneyId: Int
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ReceiveStockDetailViewModel(
                    repository = inioliApplication().container.stockJourneyRepository,
                    draftStore = inioliApplication().container.stockJourneyDraftStore,
                    stockJourneyId = stockJourneyId
                )
            }
        }
    }
}
