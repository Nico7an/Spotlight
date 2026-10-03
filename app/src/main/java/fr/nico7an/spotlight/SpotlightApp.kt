package fr.nico7an.spotlight

import android.app.Application
import fr.nico7an.spotlight.data.AppRepository

class SpotlightApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppRepository.get(this).apply {
            watchPackageChanges()
            refreshAsync()
        }
    }
}
