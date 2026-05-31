package com.lowderancorp.inioli

import android.app.Application
import com.lowderancorp.inioli.data.AppContainer
import com.lowderancorp.inioli.data.DefaultAppContainer

class InioliApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
