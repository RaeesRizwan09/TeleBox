package com.telebox.app

import android.app.Application
import com.telebox.app.data.PreferencesStore
import com.telebox.app.data.RustTelegramRepository  // new import
import com.telebox.app.data.TelegramRepository

class TeleBoxApplication : Application() {
    lateinit var repository: TelegramRepository
        private set
    lateinit var preferencesStore: PreferencesStore
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesStore = PreferencesStore(this)
        repository = RustTelegramRepository(this, preferencesStore)
    }
}