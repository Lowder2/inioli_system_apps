package com.lowderancorp.inioli.data.stockjourney

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

data class StockJourneyCloseDraft(
    val scannedQuantityByItemId: Map<Int, Int> = emptyMap(),
    val notes: String = "",
    val mutedOverscanProductIds: Set<Int> = emptySet()
)

class StockJourneyDraftStore {
    private val drafts = MutableStateFlow<Map<Int, StockJourneyCloseDraft>>(emptyMap())

    fun getDraft(stockJourneyId: Int): StockJourneyCloseDraft {
        return drafts.value[stockJourneyId] ?: StockJourneyCloseDraft()
    }

    fun replaceScannedQuantities(
        stockJourneyId: Int,
        scannedQuantityByItemId: Map<Int, Int>
    ) {
        updateDraft(stockJourneyId) { draft ->
            draft.copy(scannedQuantityByItemId = scannedQuantityByItemId)
        }
    }

    fun updateNotes(
        stockJourneyId: Int,
        notes: String
    ) {
        updateDraft(stockJourneyId) { draft ->
            draft.copy(notes = notes)
        }
    }

    fun replaceMutedOverscanProductIds(
        stockJourneyId: Int,
        mutedOverscanProductIds: Set<Int>
    ) {
        updateDraft(stockJourneyId) { draft ->
            draft.copy(mutedOverscanProductIds = mutedOverscanProductIds)
        }
    }

    fun clearDraft(stockJourneyId: Int) {
        drafts.update { currentDrafts ->
            currentDrafts - stockJourneyId
        }
    }

    private fun updateDraft(
        stockJourneyId: Int,
        transform: (StockJourneyCloseDraft) -> StockJourneyCloseDraft
    ) {
        drafts.update { currentDrafts ->
            val updatedDraft = transform(currentDrafts[stockJourneyId] ?: StockJourneyCloseDraft())
            if (updatedDraft == StockJourneyCloseDraft()) {
                currentDrafts - stockJourneyId
            } else {
                currentDrafts + (stockJourneyId to updatedDraft)
            }
        }
    }
}
