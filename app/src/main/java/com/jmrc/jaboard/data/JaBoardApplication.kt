package com.jmrc.jaboard.data

import android.app.Application

class JaBoardApplication : Application() {
    val database by lazy { JaBoardDatabase.get(this) }
    val settings by lazy { SettingsRepository(this) }
}
