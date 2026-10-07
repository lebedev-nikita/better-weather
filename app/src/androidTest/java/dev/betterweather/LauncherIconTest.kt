package dev.betterweather

import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherIconTest {
    @Test fun installedLauncherIconSupportsSystemMasksAndTheming() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val icon = context.applicationInfo.loadIcon(context.packageManager)
        assertTrue("Launcher icon must support the system's shape mask", icon is AdaptiveIconDrawable)
        if (Build.VERSION.SDK_INT >= 33) {
            assertNotNull("Themed icons need a monochrome layer", (icon as AdaptiveIconDrawable).monochrome)
        }
    }
}
