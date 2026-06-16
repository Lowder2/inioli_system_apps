package com.lowderancorp.inioli.data.stockjourney

import java.math.BigDecimal

fun StockJourneyDetailItem.willOverScan(
    currentScannedQuantity: Int
): Boolean {
    val requiredQuantity = qty.toBigDecimalOrNull() ?: return false
    val nextScannedQuantity = currentScannedQuantity.toBigDecimal() + BigDecimal.ONE
    return nextScannedQuantity > requiredQuantity
}
