package com.lowderancorp.inioli.data.stockjourney

const val DEFAULT_RECEIVE_STOCK_MOVEMENT_TYPE = "WAREHOUSE_TO_STORE"

fun resolveMovementTypeCode(
    requestedMovementTypeCode: String,
    movementTypes: List<MovementType>,
    preferredFallbackCode: String = DEFAULT_RECEIVE_STOCK_MOVEMENT_TYPE
): String {
    return when {
        movementTypes.any { it.code == requestedMovementTypeCode } -> requestedMovementTypeCode
        movementTypes.any { it.code == preferredFallbackCode } -> preferredFallbackCode
        else -> movementTypes.firstOrNull()?.code ?: requestedMovementTypeCode
    }
}
