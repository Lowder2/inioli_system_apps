package com.lowderancorp.inioli.ui

import com.lowderancorp.inioli.data.auth.AuthException
import com.lowderancorp.inioli.data.stockjourney.StockJourneyException
import java.io.IOException
import java.net.SocketTimeoutException

fun Throwable.toAuthUserMessage(): String {
    return when (this) {
        is SocketTimeoutException -> {
            "The server took too long to respond. Please try again."
        }

        is AuthException -> message ?: "Login failed. Please check your credentials."
        is IOException -> {
            "Unable to reach the login server. When testing on the Android emulator, use 10.0.2.2 instead of localhost."
        }

        else -> {
            "Something went wrong while signing in. Please try again."
        }
    }
}

fun Throwable.toStockJourneyUserMessage(
    targetLabel: String
): String {
    return when (this) {
        is SocketTimeoutException -> {
            "The stock movement server took too long to respond. Please try again."
        }

        is StockJourneyException -> {
            message ?: "Unable to load $targetLabel."
        }

        is IOException -> {
            "Unable to reach the stock movement server. Please check the connection and try again."
        }

        else -> {
            "Something went wrong while loading $targetLabel. Please try again."
        }
    }
}

fun Throwable.toStockJourneyCloseUserMessage(): String {
    return when (this) {
        is SocketTimeoutException -> {
            "The stock movement server took too long to respond while closing this movement. Please try again."
        }

        is StockJourneyException -> {
            message ?: "Unable to close this stock movement."
        }

        is IOException -> {
            "Unable to reach the stock movement server. Please check the connection and try again."
        }

        else -> {
            "Something went wrong while closing this stock movement. Please try again."
        }
    }
}
