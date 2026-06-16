package com.lowderancorp.inioli.data.stockjourney

import com.lowderancorp.inioli.data.auth.AuthLocalDataSource

class StockJourneyRepository(
    private val authLocalDataSource: AuthLocalDataSource,
    private val remoteDataSource: StockJourneyRemoteDataSource
) {
    suspend fun getMovementTypes(): List<MovementType> {
        return remoteDataSource.getMovementTypes(accessToken = requireAccessToken())
    }

    suspend fun getStockJourneyByMovementType(
        movementTypeCode: String
    ): List<StockJourneyItem> {
        return remoteDataSource.getStockJourneyByMovementType(
            accessToken = requireAccessToken(),
            movementTypeCode = movementTypeCode
        )
    }

    suspend fun getStockJourneyDetail(
        stockJourneyId: Int
    ): StockJourneyDetail {
        return remoteDataSource.getStockJourneyDetail(
            accessToken = requireAccessToken(),
            stockJourneyId = stockJourneyId
        )
    }

    private suspend fun requireAccessToken(): String {
        return authLocalDataSource.getSession()
            ?.accessToken
            ?.takeIf { token -> token.isNotBlank() }
            ?: throw StockJourneyException("Your session has expired. Please sign in again.")
    }
}
