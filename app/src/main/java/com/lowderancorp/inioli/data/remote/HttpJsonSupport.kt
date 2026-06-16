package com.lowderancorp.inioli.data.remote

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

private const val DEFAULT_TIMEOUT_MS = 15_000

internal fun openApiConnection(
    baseUrl: String,
    pathWithQuery: String
): HttpURLConnection {
    val endpoint = URL("${baseUrl.trimEnd('/')}/$pathWithQuery")
    return endpoint.openConnection() as HttpURLConnection
}

internal fun HttpURLConnection.configureJsonRequest(
    method: String,
    authorizationHeader: String? = null,
    hasRequestBody: Boolean = false
) {
    requestMethod = method
    connectTimeout = DEFAULT_TIMEOUT_MS
    readTimeout = DEFAULT_TIMEOUT_MS
    doInput = true
    doOutput = hasRequestBody
    useCaches = false
    setRequestProperty("Accept", "application/json")
    if (hasRequestBody) {
        setRequestProperty("Content-Type", "application/json")
    }
    authorizationHeader
        ?.takeIf { header -> header.isNotBlank() }
        ?.let { header -> setRequestProperty("Authorization", header) }
}

internal fun HttpURLConnection.readResponseBody(): String {
    val stream = if (responseCode in 200..299) inputStream else errorStream
    return stream?.readAllText().orEmpty()
}

internal fun HttpURLConnection.writeJsonBody(
    jsonBody: String
) {
    outputStream.bufferedWriter().use { writer ->
        writer.write(jsonBody)
    }
}

internal fun HttpURLConnection.errorMessage(
    responseBody: String,
    defaultMessage: String
): String {
    if (responseBody.isBlank()) {
        return "$defaultMessage with HTTP $responseCode."
    }

    return runCatching {
        JSONObject(responseBody).optString("message")
    }.getOrNull().orEmpty().ifBlank {
        "$defaultMessage with HTTP $responseCode."
    }
}

private fun InputStream.readAllText(): String {
    return BufferedReader(InputStreamReader(this)).use { reader ->
        reader.readText()
    }
}
