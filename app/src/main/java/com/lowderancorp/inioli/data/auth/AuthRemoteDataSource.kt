package com.lowderancorp.inioli.data.auth

import com.lowderancorp.inioli.BuildConfig
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AuthRemoteDataSource {
    suspend fun login(username: String, password: String): UserSession = withContext(Dispatchers.IO) {
        val connection = createConnection()

        try {
            connection.outputStream.bufferedWriter().use { writer ->
                val body = JSONObject()
                    .put("username", username)
                    .put("password", password)
                    .toString()
                writer.write(body)
            }

            val responseCode = connection.responseCode
            val responseBody = connection.readResponseBody()

            if (responseCode !in 200..299) {
                throw AuthException(connection.errorMessage(responseBody))
            }

            val jsonBody = JSONObject(responseBody)
            val accessToken = jsonBody.optString("access_token")
            val resolvedUsername = jsonBody.optString("username")

            if (accessToken.isBlank() || resolvedUsername.isBlank()) {
                throw AuthException("The server returned an incomplete login response.")
            }

            UserSession(
                accessToken = accessToken,
                username = resolvedUsername
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun createConnection(): HttpURLConnection {
        val endpoint = URL("${BuildConfig.LOGIN_BASE_URL.trimEnd('/')}/Login.php")
        return (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 15_000
            doInput = true
            doOutput = true
            useCaches = false
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", BuildConfig.LOGIN_AUTHORIZATION_HEADER)
        }
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
            return "Login failed with HTTP $responseCode."
        }

        return runCatching {
            JSONObject(responseBody).optString("message")
        }.getOrNull().orEmpty().ifBlank {
            "Login failed with HTTP $responseCode."
        }
    }
}
