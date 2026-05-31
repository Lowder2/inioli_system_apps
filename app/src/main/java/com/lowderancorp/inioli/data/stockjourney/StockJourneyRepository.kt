package com.lowderancorp.inioli.data.stockjourney

class StockJourneyRepository(
    private val remoteDataSource: StockJourneyRemoteDataSource
) {
    suspend fun getMovementTypes(accessToken: String): List<MovementType> {
        return remoteDataSource.getMovementTypes(accessToken = accessToken)
    }

    suspend fun getStockJourneyByMovementType(
        accessToken: String,
        movementTypeCode: String
    ): List<StockJourneyItem> {
        return remoteDataSource.getStockJourneyByMovementType(
            accessToken = accessToken,
            movementTypeCode = movementTypeCode
        )
    }

    suspend fun getStockJourneyDetail(
        accessToken: String,
        stockJourneyId: Int
    ): StockJourneyDetail {
        return remoteDataSource.getStockJourneyDetail(
            accessToken = accessToken,
            stockJourneyId = stockJourneyId
        )
    }
}
