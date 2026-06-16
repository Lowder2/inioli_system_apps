package com.lowderancorp.inioli.data.stockjourney

import java.math.BigDecimal
import java.math.RoundingMode

data class QuantityProgressSummary(
    val requiredDisplay: String,
    val scannedDisplay: String,
    val remainingDisplay: String,
    val receivedDisplay: String,
    val isOverScanned: Boolean,
    val overScannedDisplay: String? = null
) {
    val statusMessage: String
        get() = overScannedDisplay?.let { overScanned ->
            "Over scanned by $overScanned | Received $receivedDisplay"
        } ?: "Received $receivedDisplay"
}

fun StockJourneyDetailItem.toQuantityProgress(
    scannedQuantity: Int
): QuantityProgressSummary {
    val required = qty.toBigDecimalOrNull()
    val scanned = scannedQuantity.toBigDecimal()
    val remaining = required?.subtract(scanned)
    val overScannedDisplay = remaining
        ?.takeIf { value -> value < BigDecimal.ZERO }
        ?.abs()
        ?.formatQuantity()

    return QuantityProgressSummary(
        requiredDisplay = required?.formatQuantity() ?: qty,
        scannedDisplay = scannedQuantity.toString(),
        remainingDisplay = remaining?.coerceAtLeastZero()?.formatQuantity() ?: "-",
        receivedDisplay = receivedQty.formatQuantityOrDefault(defaultValue = "0"),
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

private fun BigDecimal.coerceAtLeastZero(): BigDecimal {
    return if (this < BigDecimal.ZERO) BigDecimal.ZERO else this
}

private fun BigDecimal.formatQuantity(): String {
    return setScale(3, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
}
