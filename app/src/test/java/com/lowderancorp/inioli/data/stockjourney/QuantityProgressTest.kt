package com.lowderancorp.inioli.data.stockjourney

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuantityProgressTest {
    @Test
    fun formatsQuantityProgressForNormalScan() {
        val item = stockJourneyDetailItem(
            qty = "10.000",
            receivedQty = "2.500"
        )

        val progress = item.toQuantityProgress(scannedQuantity = 3)

        assertEquals("10", progress.requiredDisplay)
        assertEquals("3", progress.scannedDisplay)
        assertEquals("7", progress.remainingDisplay)
        assertEquals("2.5", progress.receivedDisplay)
        assertEquals("Received 2.5", progress.statusMessage)
        assertFalse(progress.isOverScanned)
    }

    @Test
    fun marksOverscanAndClampsRemainingAtZero() {
        val item = stockJourneyDetailItem(
            qty = "1",
            receivedQty = null
        )

        val progress = item.toQuantityProgress(scannedQuantity = 3)

        assertEquals("0", progress.remainingDisplay)
        assertEquals("2", progress.overScannedDisplay)
        assertEquals("Over scanned by 2 | Received 0", progress.statusMessage)
        assertTrue(progress.isOverScanned)
    }

    private fun stockJourneyDetailItem(
        qty: String,
        receivedQty: String?
    ): StockJourneyDetailItem {
        return StockJourneyDetailItem(
            id = 1,
            stockJourneyId = 1,
            productId = 1,
            productCode = "SKU-1",
            barcode = "123456",
            productName = "Test Product",
            allowDecimalStock = true,
            brandCode = null,
            brandName = null,
            prevQty = "0",
            qty = qty,
            receivedQty = receivedQty,
            newQty = qty,
            createdAt = "2026-05-31 10:00:00"
        )
    }
}
