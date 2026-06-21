package com.lowderancorp.inioli.data.stockjourney

import java.math.BigDecimal
import java.math.RoundingMode

data class QuantityProgressSummary(
    val requiredDisplay: String,
    val scannedDisplay: String,
    val isOverScanned: Boolean,
    val overScannedDisplay: String? = null
)

fun StockJourneyDetailItem.toQuantityProgress(
    scannedQuantity: Int
): QuantityProgressSummary {
    val required = qty.toBigDecimalOrNull()
    val scanned = scannedQuantity.toBigDecimal()
    val overScannedDisplay = required
        ?.subtract(scanned)
        ?.takeIf { value -> value < BigDecimal.ZERO }
        ?.abs()
        ?.formatQuantity()

    return QuantityProgressSummary(
        requiredDisplay = required?.formatQuantity() ?: qty,
        scannedDisplay = scannedQuantity.toString(),
        isOverScanned = overScannedDisplay != null,
        overScannedDisplay = overScannedDisplay
    )
}

fun String?.formatQuantityOrDefault(defaultValue: String = "-"): String {
    val rawValue = this?.trim().orEmpty()
    if (rawValue.isBlank()) {
        return defaultValue
    }

    return rawValue.toBigDecimalOrNull()?.formatQuantity() ?: rawValue
}

private fun BigDecimal.formatQuantity(): String {
    return setScale(3, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
}
