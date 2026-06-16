package com.lowderancorp.inioli.data.stockjourney

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StockJourneyScanRulesTest {
    @Test
    fun returnsFalseWhenNextScanDoesNotExceedRequiredQuantity() {
        val item = stockJourneyDetailItem(qty = "3")

        assertFalse(item.willOverScan(currentScannedQuantity = 2))
    }

    @Test
    fun returnsTrueWhenNextScanWouldExceedRequiredQuantity() {
        val item = stockJourneyDetailItem(qty = "3")

        assertTrue(item.willOverScan(currentScannedQuantity = 3))
    }

    @Test
    fun supportsDecimalRequiredQuantities() {
        val item = stockJourneyDetailItem(qty = "1.5")

        assertFalse(item.willOverScan(currentScannedQuantity = 0))
        assertTrue(item.willOverScan(currentScannedQuantity = 1))
    }

    private fun stockJourneyDetailItem(
        qty: String
    ): StockJourneyDetailItem {
        return StockJourneyDetailItem(
            id = 1,
            stockJourneyId = 1,
            productId = 688,
            productCode = "SKU-688",
            barcode = "123456789",
            productName = "Test Product",
            allowDecimalStock = true,
            brandCode = null,
            brandName = null,
            prevQty = "0",
            qty = qty,
            receivedQty = null,
            newQty = qty,
            createdAt = "2026-06-16 10:00:00"
        )
    }
}
