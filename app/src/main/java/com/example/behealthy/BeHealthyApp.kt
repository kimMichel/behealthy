package com.example.behealthy

import android.app.Application
import com.example.behealthy.shared.di.mainModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class BeHealthyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin()
    }

    private fun startKoin() {
        val appModules = listOf(
            mainModule,
        )
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@BeHealthyApp)
            modules(appModules)
        }
    }
}
