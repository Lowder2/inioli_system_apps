package com.lowderancorp.inioli.data.auth

import com.lowderancorp.inioli.BuildConfig
import com.lowderancorp.inioli.data.remote.configureJsonRequest
import com.lowderancorp.inioli.data.remote.errorMessage
import com.lowderancorp.inioli.data.remote.openApiConnection
import com.lowderancorp.inioli.data.remote.readResponseBody
import java.net.HttpURLConnection
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
                throw AuthException(
                    connection.errorMessage(
                        responseBody = responseBody,
                        defaultMessage = "Login failed"
                    )
                )
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
        return openApiConnection(
            baseUrl = BuildConfig.LOGIN_BASE_URL,
            pathWithQuery = "Login.php"
        ).apply {
            configureJsonRequest(
                method = "POST",
                authorizationHeader = BuildConfig.LOGIN_BEARER_TOKEN
                    .takeIf { token -> token.isNotBlank() }
                    ?.let { token -> "Bearer $token" },
                hasRequestBody = true
            )
        }
    }
}
