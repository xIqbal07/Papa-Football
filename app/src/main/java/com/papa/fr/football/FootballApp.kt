package com.papa.fr.football

import android.app.Application
import com.papa.fr.football.di.dataModule
import com.papa.fr.football.di.domainModule
import com.papa.fr.football.di.networkModule
import com.papa.fr.football.di.presentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class FootballApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@FootballApp)
            modules(listOf(networkModule, dataModule, domainModule, presentationModule))
        }
    }
}
