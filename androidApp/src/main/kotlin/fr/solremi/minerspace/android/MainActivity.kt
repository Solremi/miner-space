package fr.solremi.minerspace.android

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import fr.solremi.minerspace.android.platform.AndroidGameLogger
import fr.solremi.minerspace.android.platform.AndroidPlatformServices
import fr.solremi.minerspace.game.MinerSpaceGame

class MainActivity : AndroidApplication() {
    private lateinit var platformServices: AndroidPlatformServices

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        platformServices = AndroidPlatformServices(applicationContext)
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
    }

    override fun onResume() {
        super.onResume()
        if (::platformServices.isInitialized) {
            platformServices.onForeground()
        }
    }

    override fun onPause() {
        if (::platformServices.isInitialized) {
            platformServices.onBackground()
        }
        super.onPause()
    }
}
