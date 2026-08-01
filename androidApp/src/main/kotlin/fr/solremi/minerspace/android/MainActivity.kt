package fr.solremi.minerspace.android

import android.content.ComponentCallbacks2
import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import fr.solremi.minerspace.android.platform.AndroidGameLogger
import fr.solremi.minerspace.android.platform.AndroidPlatformServices
import fr.solremi.minerspace.android.platform.LocalCrashReporter
import fr.solremi.minerspace.game.MinerSpaceGame

class MainActivity : AndroidApplication() {
    private lateinit var platformServices: AndroidPlatformServices

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalCrashReporter.install(applicationContext)
        platformServices = AndroidPlatformServices(this)
        val configuration = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            useWakelock = false
            useCompass = false
            useAccelerometer = false
            numSamples = 2
        }
        initialize(
            MinerSpaceGame(
                services = platformServices.services,
                logger = AndroidGameLogger,
            ),
            configuration,
        )
        platformServices.requestConsentAtLaunch()
    }

    override fun onResume() {
        super.onResume()
        if (::platformServices.isInitialized) platformServices.onForeground()
    }

    override fun onPause() {
        if (::platformServices.isInitialized) platformServices.onBackground()
        super.onPause()
    }

    override fun onStop() {
        if (::platformServices.isInitialized) platformServices.onBackground()
        super.onStop()
    }

    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN && ::platformServices.isInitialized) {
            platformServices.onBackground()
        }
        super.onTrimMemory(level)
    }

    override fun onLowMemory() {
        if (::platformServices.isInitialized) platformServices.onBackground()
        super.onLowMemory()
    }

    override fun onDestroy() {
        if (::platformServices.isInitialized) platformServices.close()
        super.onDestroy()
    }
}
