package com.lowderancorp.inioli.data.stockjourney

import org.junit.Assert.assertEquals
import org.junit.Test

class MovementTypeResolverTest {
    @Test
    fun returnsRequestedCodeWhenItExists() {
        val movementTypes = listOf(
            MovementType(1, "STORE_TO_STORE", "Store to Store", "OUT"),
            MovementType(2, DEFAULT_RECEIVE_STOCK_MOVEMENT_TYPE, "Warehouse to Store", "IN")
        )

        val resolvedCode = resolveMovementTypeCode(
            requestedMovementTypeCode = "STORE_TO_STORE",
            movementTypes = movementTypes
        )

        assertEquals("STORE_TO_STORE", resolvedCode)
    }

    @Test
    fun fallsBackToPreferredCodeWhenRequestedCodeIsMissing() {
        val movementTypes = listOf(
            MovementType(1, DEFAULT_RECEIVE_STOCK_MOVEMENT_TYPE, "Warehouse to Store", "IN"),
            MovementType(2, "SUPPLIER_RETURN", "Supplier Return", "OUT")
        )

        val resolvedCode = resolveMovementTypeCode(
            requestedMovementTypeCode = "UNKNOWN",
            movementTypes = movementTypes
        )

        assertEquals(DEFAULT_RECEIVE_STOCK_MOVEMENT_TYPE, resolvedCode)
    }

    @Test
    fun fallsBackToFirstAvailableCodeWhenPreferredCodeIsUnavailable() {
        val movementTypes = listOf(
            MovementType(1, "SUPPLIER_RETURN", "Supplier Return", "OUT"),
            MovementType(2, "STORE_TO_STORE", "Store to Store", "OUT")
        )

        val resolvedCode = resolveMovementTypeCode(
            requestedMovementTypeCode = "UNKNOWN",
            movementTypes = movementTypes
        )

        assertEquals("SUPPLIER_RETURN", resolvedCode)
    }
}
