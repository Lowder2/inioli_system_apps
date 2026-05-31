package com.lowderancorp.inioli.data.stockjourney

data class MovementType(
    val id: Int,
    val code: String,
    val description: String,
    val direction: String
)

data class StockJourneyItem(
    val id: Int,
    val status: String,
    val notes: String?,
    val createdAt: String,
    val sourceLocationCode: String,
    val sourceLocationName: String,
    val destinationLocationCode: String,
    val destinationLocationName: String,
    val totalQty: String,
    val totalReceivedQty: String?,
    val itemCount: Int
)

data class StockJourneyDetail(
    val id: Int,
    val status: String,
    val notes: String?,
    val createdAt: String,
    val movementType: String,
    val movementDescription: String,
    val sourceLocationCode: String?,
    val sourceLocationName: String?,
    val destinationLocationCode: String?,
    val destinationLocationName: String?,
    val items: List<StockJourneyDetailItem>,
    val itemCount: Int,
    val totalQty: String,
    val totalReceivedQty: String?
)

data class StockJourneyDetailItem(
    val id: Int,
    val stockJourneyId: Int,
    val productId: Int,
    val productCode: String,
    val barcode: String?,
    val productName: String,
    val allowDecimalStock: Boolean,
    val brandCode: String?,
    val brandName: String?,
    val prevQty: String,
    val qty: String,
    val receivedQty: String?,
    val newQty: String,
    val createdAt: String
)

class StockJourneyException(message: String) : Exception(message)
