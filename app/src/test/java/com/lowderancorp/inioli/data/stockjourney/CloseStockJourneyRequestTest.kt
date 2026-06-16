package com.lowderancorp.inioli.data.stockjourney

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CloseStockJourneyRequestTest {
    @Test
    fun buildsCloseRequestFromScannedItemsAndTrimmedNotes() {
        val detail = stockJourneyDetail(
            id = 1,
            items = listOf(
                stockJourneyDetailItem(id = 10, productId = 688, qty = "10"),
                stockJourneyDetailItem(id = 11, productId = 681, qty = "5")
            )
        )

        val request = detail.buildCloseStockJourneyRequest(
            scannedQuantityByItemId = mapOf(
                10 to 10,
                11 to 5
            ),
            notes = "  optional close notes  "
        )

        assertNotNull(request)
        assertEquals(1, request?.stockJourneyId)
        assertEquals("optional close notes", request?.notes)
        assertEquals(listOf(688, 681), request?.items?.map { item -> item.productId })
        assertBigDecimalEquals("10", request?.items?.get(0)?.receivedQty)
        assertBigDecimalEquals("5", request?.items?.get(1)?.receivedQty)

        val preview = detail.buildCloseStockJourneyPreview(
            scannedQuantityByItemId = mapOf(
                10 to 10,
                11 to 5
            ),
            notes = "  optional close notes  "
        )

        assertEquals("15", preview.totalSubmittedQuantityDisplay)
        assertFalse(preview.hasOverscannedQuantities)
    }

    @Test
    fun preservesOverscannedQuantitiesAndOmitsZeroQuantityEntries() {
        val detail = stockJourneyDetail(
            id = 2,
            items = listOf(
                stockJourneyDetailItem(id = 20, productId = 688, qty = "1.5"),
                stockJourneyDetailItem(id = 21, productId = 681, qty = "3")
            )
        )

        val request = detail.buildCloseStockJourneyRequest(
            scannedQuantityByItemId = mapOf(
                20 to 3,
                21 to 0
            ),
            notes = "   "
        )

        assertNotNull(request)
        assertNull(request?.notes)
        assertEquals(1, request?.items?.size)
        assertEquals(688, request?.items?.single()?.productId)
        assertBigDecimalEquals("3", request?.items?.single()?.receivedQty)

        val preview = detail.buildCloseStockJourneyPreview(
            scannedQuantityByItemId = mapOf(
                20 to 3,
                21 to 0
            ),
            notes = "   "
        )

        assertTrue(preview.hasOverscannedQuantities)
        assertEquals("3", preview.totalSubmittedQuantityDisplay)
        assertEquals(1, preview.items.size)
        assertEquals("3", preview.items.single().scannedQtyDisplay)
        assertEquals("3", preview.items.single().receivedQtyDisplay)
    }

    @Test
    fun aggregatesDuplicateProductIdsIntoOneSubmittedItem() {
        val detail = stockJourneyDetail(
            id = 3,
            items = listOf(
                stockJourneyDetailItem(id = 30, productId = 688, qty = "2"),
                stockJourneyDetailItem(id = 31, productId = 688, qty = "3")
            )
        )

        val request = detail.buildCloseStockJourneyRequest(
            scannedQuantityByItemId = mapOf(
                30 to 2,
                31 to 4
            ),
            notes = ""
        )

        assertNotNull(request)
        assertEquals(1, request?.items?.size)
        assertEquals(688, request?.items?.single()?.productId)
        assertBigDecimalEquals("6", request?.items?.single()?.receivedQty)
    }

    @Test
    fun rejectsInvalidCloseResponsePayload() {
        assertThrows(StockJourneyException::class.java) {
            closeStockJourneyResultOrThrow(
                id = 0,
                status = "CLOSED"
            )
        }
    }

    private fun assertBigDecimalEquals(
        expected: String,
        actual: BigDecimal?
    ) {
        assertNotNull(actual)
        assertEquals(0, actual?.compareTo(BigDecimal(expected)))
    }

    private fun stockJourneyDetail(
        id: Int,
        items: List<StockJourneyDetailItem>
    ): StockJourneyDetail {
        return StockJourneyDetail(
            id = id,
            status = "OPEN",
            notes = null,
            createdAt = "2026-06-16 10:00:00",
            movementType = "TR_IN",
            movementDescription = "Receive Stock",
            sourceLocationCode = "SRC",
            sourceLocationName = "Source Warehouse",
            destinationLocationCode = "DST",
            destinationLocationName = "Destination Warehouse",
            items = items,
            itemCount = items.size,
            totalQty = items.size.toString(),
            totalReceivedQty = null
        )
    }

    private fun stockJourneyDetailItem(
        id: Int,
        productId: Int,
        qty: String
    ): StockJourneyDetailItem {
        return StockJourneyDetailItem(
            id = id,
            stockJourneyId = 1,
            productId = productId,
            productCode = "SKU-$productId",
            barcode = "BAR-$id",
            productName = "Product $productId",
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
