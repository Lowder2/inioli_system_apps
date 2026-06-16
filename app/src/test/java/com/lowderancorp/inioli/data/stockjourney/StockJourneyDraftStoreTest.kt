package com.lowderancorp.inioli.data.stockjourney

import org.junit.Assert.assertEquals
import org.junit.Test

class StockJourneyDraftStoreTest {
    @Test
    fun storesAndClearsDraftByStockJourneyId() {
        val store = StockJourneyDraftStore()

        store.replaceScannedQuantities(
            stockJourneyId = 10,
            scannedQuantityByItemId = mapOf(
                101 to 2,
                102 to 4
            )
        )
        store.updateNotes(
            stockJourneyId = 10,
            notes = "Ready to close"
        )
        store.replaceMutedOverscanProductIds(
            stockJourneyId = 10,
            mutedOverscanProductIds = setOf(688)
        )

        assertEquals(
            StockJourneyCloseDraft(
                scannedQuantityByItemId = mapOf(
                    101 to 2,
                    102 to 4
                ),
                notes = "Ready to close",
                mutedOverscanProductIds = setOf(688)
            ),
            store.getDraft(stockJourneyId = 10)
        )

        store.clearDraft(stockJourneyId = 10)

        assertEquals(
            StockJourneyCloseDraft(),
            store.getDraft(stockJourneyId = 10)
        )
    }
}
