package com.lowderancorp.inioli.data.stockjourney

import java.math.BigDecimal

data class CloseStockJourneyRequest(
    val stockJourneyId: Int,
    val items: List<CloseStockJourneyRequestItem>,
    val notes: String? = null
)

data class CloseStockJourneyRequestItem(
    val productId: Int,
    val receivedQty: BigDecimal
)

data class CloseStockJourneyPreview(
    val items: List<CloseStockJourneyPreviewItem>,
    val notes: String? = null
) {
    val hasOverscannedQuantities: Boolean
        get() = items.any { item -> item.isOverscanned }

    val totalSubmittedQuantityDisplay: String
        get() = items
            .fold(BigDecimal.ZERO) { total, item -> total + item.receivedQty }
            .toDisplayQuantity()
}

data class CloseStockJourneyPreviewItem(
    val productId: Int,
    val productCode: String,
    val productName: String,
    val scannedQty: BigDecimal,
    val receivedQty: BigDecimal,
    val requiredQty: BigDecimal?
) {
    val isOverscanned: Boolean
        get() = requiredQty?.let { required -> scannedQty > required } ?: false

    val scannedQtyDisplay: String
        get() = scannedQty.toDisplayQuantity()

    val receivedQtyDisplay: String
        get() = receivedQty.toDisplayQuantity()

    val requiredQtyDisplay: String?
        get() = requiredQty?.toDisplayQuantity()
}

fun StockJourneyDetail.buildCloseStockJourneyPreview(
    scannedQuantityByItemId: Map<Int, Int>,
    notes: String
): CloseStockJourneyPreview {
    return CloseStockJourneyPreview(
        items = aggregateClosePreviewItems(scannedQuantityByItemId = scannedQuantityByItemId),
        notes = notes.trimToNull()
    )
}

fun StockJourneyDetail.buildCloseStockJourneyRequest(
    scannedQuantityByItemId: Map<Int, Int>,
    notes: String
): CloseStockJourneyRequest? {
    val previewItems = aggregateClosePreviewItems(scannedQuantityByItemId = scannedQuantityByItemId)
    if (previewItems.isEmpty()) return null

    return CloseStockJourneyRequest(
        stockJourneyId = id,
        items = previewItems.map { item ->
            CloseStockJourneyRequestItem(
                productId = item.productId,
                receivedQty = item.receivedQty
            )
        },
        notes = notes.trimToNull()
    )
}

private fun StockJourneyDetail.aggregateClosePreviewItems(
    scannedQuantityByItemId: Map<Int, Int>
): List<CloseStockJourneyPreviewItem> {
    val aggregatedItems = linkedMapOf<Int, CloseStockJourneyPreviewItem>()

    items.forEach { item ->
        val scannedQuantity = scannedQuantityByItemId[item.id] ?: 0
        if (scannedQuantity <= 0) return@forEach

        val scannedQty = scannedQuantity.toBigDecimal()
        val requiredQty = item.qty.toBigDecimalOrNull()?.coerceAtLeast(BigDecimal.ZERO)
        val receivedQty = scannedQty

        if (receivedQty <= BigDecimal.ZERO) return@forEach

        val currentItem = aggregatedItems[item.productId]
        aggregatedItems[item.productId] = if (currentItem == null) {
            CloseStockJourneyPreviewItem(
                productId = item.productId,
                productCode = item.productCode,
                productName = item.productName,
                scannedQty = scannedQty,
                receivedQty = receivedQty,
                requiredQty = requiredQty
            )
        } else {
            currentItem.copy(
                scannedQty = currentItem.scannedQty + scannedQty,
                receivedQty = currentItem.receivedQty + receivedQty,
                requiredQty = sumNullable(currentItem.requiredQty, requiredQty)
            )
        }
    }

    return aggregatedItems.values.toList()
}

private fun BigDecimal.coerceAtLeast(minimum: BigDecimal): BigDecimal {
    return if (this < minimum) minimum else this
}

private fun sumNullable(
    first: BigDecimal?,
    second: BigDecimal?
): BigDecimal? {
    return when {
        first == null && second == null -> null
        first == null -> second
        second == null -> first
        else -> first + second
    }
}

private fun BigDecimal.toDisplayQuantity(): String {
    return stripTrailingZeros().toPlainString()
}

private fun String.trimToNull(): String? {
    return trim().takeIf { value -> value.isNotBlank() }
}
