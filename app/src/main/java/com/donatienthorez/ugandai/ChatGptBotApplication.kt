package com.ugandai.ugandai

import android.app.Application
import com.ugandai.ugandai.di.chatModule
import com.ugandai.ugandai.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class UgandAI : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin()
    }

    private fun startKoin() {
        startKoin {
            // Log Koin into Android logger
            androidLogger()
            // Reference Android context
            androidContext(this@UgandAI)
            // Load modules
            modules(
                listOf(
                    networkModule,
                    chatModule
                )
            )
        }
    }
}