package com.lowderancorp.inioli.data.stockjourney

import com.lowderancorp.inioli.BuildConfig
import com.lowderancorp.inioli.data.remote.configureJsonRequest
import com.lowderancorp.inioli.data.remote.errorMessage
import com.lowderancorp.inioli.data.remote.openApiConnection
import com.lowderancorp.inioli.data.remote.readResponseBody
import com.lowderancorp.inioli.data.remote.writeJsonBody
import java.net.HttpURLConnection
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class StockJourneyRemoteDataSource {
    suspend fun getMovementTypes(accessToken: String): List<MovementType> = withContext(Dispatchers.IO) {
        val connection = createConnection(
            accessToken = accessToken,
            pathWithQuery = "GetMovementTypes.php"
        )

        try {
            val responseCode = connection.responseCode
            val responseBody = connection.readResponseBody()

            if (responseCode !in 200..299) {
                throw StockJourneyException(
                    connection.errorMessage(
                        responseBody = responseBody,
                        defaultMessage = "Failed to load stock movements"
                    )
                )
            }

            val items = JSONArray(responseBody)
            buildList(items.length()) {
                repeat(items.length()) { index ->
                    val item = items.optJSONObject(index) ?: return@repeat
                    add(item.toMovementType())
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun getStockJourneyByMovementType(
        accessToken: String,
        movementTypeCode: String
    ): List<StockJourneyItem> = withContext(Dispatchers.IO) {
        val connection = createConnection(
            accessToken = accessToken,
            pathWithQuery = buildString {
                append("GetStockJourneyByMovementType.php?movement_type_code=")
                append(URLEncoder.encode(movementTypeCode, Charsets.UTF_8.name()))
            }
        )

        try {
            val responseCode = connection.responseCode
            val responseBody = connection.readResponseBody()

            if (responseCode !in 200..299) {
                throw StockJourneyException(
                    connection.errorMessage(
                        responseBody = responseBody,
                        defaultMessage = "Failed to load stock movements"
                    )
                )
            }

            val jsonBody = JSONObject(responseBody)
            val items = jsonBody.optJSONArray("items") ?: JSONArray()
            buildList(items.length()) {
                repeat(items.length()) { index ->
                    val item = items.optJSONObject(index) ?: return@repeat
                    add(item.toStockJourneyItem())
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun getStockJourneyDetail(
        accessToken: String,
        stockJourneyId: Int
    ): StockJourneyDetail = withContext(Dispatchers.IO) {
        val connection = createConnection(
            accessToken = accessToken,
            pathWithQuery = "GetStockJourneyDetail.php?stock_journey_id=$stockJourneyId"
        )

        try {
            val responseCode = connection.responseCode
            val responseBody = connection.readResponseBody()

            if (responseCode !in 200..299) {
                throw StockJourneyException(
                    connection.errorMessage(
                        responseBody = responseBody,
                        defaultMessage = "Failed to load stock movements"
                    )
                )
            }

            JSONObject(responseBody).toStockJourneyDetail()
        } finally {
            connection.disconnect()
        }
    }

    suspend fun closeStockJourney(
        accessToken: String,
        request: CloseStockJourneyRequest
    ): CloseStockJourneyResult = withContext(Dispatchers.IO) {
        val connection = createConnection(
            accessToken = accessToken,
            pathWithQuery = "CloseStockJourney.php",
            method = "POST",
            hasRequestBody = true
        )

        try {
            connection.writeJsonBody(request.toJson().toString())

            val responseCode = connection.responseCode
            val responseBody = connection.readResponseBody()

            if (responseCode !in 200..299) {
                throw StockJourneyException(
                    connection.errorMessage(
                        responseBody = responseBody,
                        defaultMessage = "Failed to close stock movement"
                    )
                )
            }

            try {
                JSONObject(responseBody).toRequiredCloseStockJourneyResult()
            } catch (exception: JSONException) {
                throw StockJourneyException(
                    "The server returned an invalid close response. Please try again."
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun createConnection(
        accessToken: String,
        pathWithQuery: String,
        method: String = "GET",
        hasRequestBody: Boolean = false
    ): HttpURLConnection {
        return openApiConnection(
            baseUrl = BuildConfig.STOCK_MOVEMENT_BASE_URL,
            pathWithQuery = pathWithQuery
        ).apply {
            configureJsonRequest(
                method = method,
                authorizationHeader = "Bearer $accessToken",
                hasRequestBody = hasRequestBody
            )
        }
    }

    private fun CloseStockJourneyRequest.toJson(): JSONObject {
        return JSONObject().apply {
            put("stock_journey_id", stockJourneyId)
            put(
                "items",
                JSONArray().apply {
                    items.forEach { item ->
                        put(
                            JSONObject().apply {
                                put("product_id", item.productId)
                                put("received_qty", item.receivedQty)
                            }
                        )
                    }
                }
            )
            notes?.let { closeNotes ->
                put("notes", closeNotes)
            }
        }
    }

    private fun JSONObject.toMovementType(): MovementType {
        return MovementType(
            id = optInt("id"),
            code = optString("code"),
            description = optString("description"),
            direction = optString("direction")
        )
    }

    private fun JSONObject.toStockJourneyItem(): StockJourneyItem {
        return StockJourneyItem(
            id = optInt("id"),
            status = optString("status"),
            notes = optNullableString("notes"),
            createdAt = optString("created_at"),
            sourceLocationCode = optString("source_location_code"),
            sourceLocationName = optString("source_location_name"),
            destinationLocationCode = optString("destination_location_code"),
            destinationLocationName = optString("destination_location_name"),
            totalQty = optString("total_qty"),
            totalReceivedQty = optNullableString("total_received_qty"),
            itemCount = optInt("item_count")
        )
    }

    private fun JSONObject.toStockJourneyDetail(): StockJourneyDetail {
        val detailItems = optJSONArray("items") ?: JSONArray()
        return StockJourneyDetail(
            id = optInt("id"),
            status = optString("status"),
            notes = optNullableString("notes"),
            createdAt = optString("created_at"),
            movementType = optString("movement_type"),
            movementDescription = optString("movement_description"),
            sourceLocationCode = optNullableString("source_location_code"),
            sourceLocationName = optNullableString("source_location_name"),
            destinationLocationCode = optNullableString("destination_location_code"),
            destinationLocationName = optNullableString("destination_location_name"),
            items = buildList(detailItems.length()) {
                repeat(detailItems.length()) { index ->
                    val item = detailItems.optJSONObject(index) ?: return@repeat
                    add(item.toStockJourneyDetailItem())
                }
            },
            itemCount = optInt("item_count"),
            totalQty = optNullableString("total_qty") ?: optDouble("total_qty").toString(),
            totalReceivedQty = optNullableString("total_received_qty")
        )
    }

    private fun JSONObject.toStockJourneyDetailItem(): StockJourneyDetailItem {
        return StockJourneyDetailItem(
            id = optInt("id"),
            stockJourneyId = optInt("stock_journey_id"),
            productId = optInt("product_id"),
            productCode = optString("product_code"),
            barcode = optNullableString("barcode"),
            productName = optString("product_name"),
            allowDecimalStock = optInt("allow_decimal_stock") == 1,
            brandCode = optNullableString("brand_code"),
            brandName = optNullableString("brand_name"),
            prevQty = optString("prev_qty"),
            qty = optString("qty"),
            receivedQty = optNullableString("received_qty"),
            newQty = optString("new_qty"),
            createdAt = optString("created_at")
        )
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() }
    }
}
