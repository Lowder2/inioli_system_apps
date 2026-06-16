package com.lowderancorp.inioli.ui

import com.lowderancorp.inioli.data.auth.AuthException
import com.lowderancorp.inioli.data.stockjourney.StockJourneyException
import org.junit.Assert.assertEquals
import org.junit.Test

class ThrowableUserMessageTest {
    @Test
    fun keepsAuthExceptionMessageWhenAvailable() {
        val message = AuthException("Invalid credentials").toAuthUserMessage()

        assertEquals("Invalid credentials", message)
    }

    @Test
    fun keepsStockJourneyExceptionMessageWhenAvailable() {
        val message = StockJourneyException("Session expired").toStockJourneyUserMessage(
            targetLabel = "stock movements"
        )

        assertEquals("Session expired", message)
    }
}
