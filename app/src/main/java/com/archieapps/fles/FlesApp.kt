package com.archieapps.fles

import android.app.Application
import com.archieapps.fles.di.AppContainer

class FlesApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
