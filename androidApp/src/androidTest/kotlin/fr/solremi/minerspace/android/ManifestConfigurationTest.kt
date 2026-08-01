package fr.solremi.minerspace.android

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.test.InstrumentationTestCase

@Suppress("DEPRECATION")
class ManifestConfigurationTest : InstrumentationTestCase() {
    fun testApplicationDisablesBackupAndCleartextTraffic() {
        val context = instrumentation.targetContext
        val info = context.packageManager.getApplicationInfo(context.packageName, 0)

        assertEquals(0, info.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
        assertEquals(0, info.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC)
    }

    fun testMainActivityRemainsLandscapeAndExported() {
        val context = instrumentation.targetContext
        val component = ComponentName(context, MainActivity::class.java)
        val info = context.packageManager.getActivityInfo(component, 0)

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, info.screenOrientation)
        assertTrue(info.exported)
    }
}
