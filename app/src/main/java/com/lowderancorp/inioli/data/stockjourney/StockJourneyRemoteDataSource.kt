package com.lowderancorp.inioli.data.stockjourney

import com.lowderancorp.inioli.BuildConfig
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
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
                throw StockJourneyException(connection.errorMessage(responseBody))
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
                throw StockJourneyException(connection.errorMessage(responseBody))
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
                throw StockJourneyException(connection.errorMessage(responseBody))
            }

            JSONObject(responseBody).toStockJourneyDetail()
        } finally {
            connection.disconnect()
        }
    }

    private fun createConnection(
        accessToken: String,
        pathWithQuery: String
    ): HttpURLConnection {
        val endpoint = URL(
            "${BuildConfig.STOCK_MOVEMENT_BASE_URL.trimEnd('/')}/$pathWithQuery"
        )
        return (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            doInput = true
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $accessToken")
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

    private fun HttpURLConnection.readResponseBody(): String {
        val stream = if (responseCode in 200..299) inputStream else errorStream
        return stream?.readAllText().orEmpty()
    }

    private fun InputStream.readAllText(): String {
        return BufferedReader(InputStreamReader(this)).use { reader ->
            reader.readText()
        }
    }

    private fun HttpURLConnection.errorMessage(responseBody: String): String {
        if (responseBody.isBlank()) {
            return "Failed to load stock movements with HTTP $responseCode."
        }

        return runCatching {
            JSONObject(responseBody).optString("message")
        }.getOrNull().orEmpty().ifBlank {
            "Failed to load stock movements with HTTP $responseCode."
        }
    }
}
