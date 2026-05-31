package com.lowderancorp.inioli.data.auth

data class UserSession(
    val accessToken: String,
    val username: String
)

class AuthException(message: String) : Exception(message)
