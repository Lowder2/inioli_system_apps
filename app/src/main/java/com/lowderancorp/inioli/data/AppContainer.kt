package com.lowderancorp.inioli.data

import android.content.Context
import com.lowderancorp.inioli.data.auth.AuthLocalDataSource
import com.lowderancorp.inioli.data.auth.AuthRemoteDataSource
import com.lowderancorp.inioli.data.auth.AuthRepository
import com.lowderancorp.inioli.data.stockjourney.StockJourneyRemoteDataSource
import com.lowderancorp.inioli.data.stockjourney.StockJourneyRepository

interface AppContainer {
    val authRepository: AuthRepository
    val stockJourneyRepository: StockJourneyRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext
    private val authLocalDataSource = AuthLocalDataSource(appContext)
    private val authRemoteDataSource = AuthRemoteDataSource()
    private val stockJourneyRemoteDataSource = StockJourneyRemoteDataSource()

    override val authRepository: AuthRepository by lazy {
        AuthRepository(
            localDataSource = authLocalDataSource,
            remoteDataSource = authRemoteDataSource
        )
    }

    override val stockJourneyRepository: StockJourneyRepository by lazy {
        StockJourneyRepository(
            remoteDataSource = stockJourneyRemoteDataSource
        )
    }
}
