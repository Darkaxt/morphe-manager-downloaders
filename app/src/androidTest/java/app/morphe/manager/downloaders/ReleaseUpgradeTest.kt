package app.morphe.manager.downloaders

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Opt in with releaseUpgrade=prepare/verify on old/new production APKs respectively. */
@RunWith(AndroidJUnit4::class)
class ReleaseUpgradeTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val endpoint = "https://upgrade-fixture.example:8191/v1"
    @Test fun aSaveEndpointOnPublishedRelease() {
        org.junit.Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("releaseUpgrade") == "prepare")
        assertEquals("0.3.2", context.packageManager.getPackageInfo(context.packageName, 0).versionName)
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            val input = device.wait(Until.findObject(By.clazz("android.widget.EditText")), 15000)
            assertNotNull(input)
            input.text = endpoint
            val prefs = context.getSharedPreferences("byparr", Context.MODE_PRIVATE)
            val saved = CountDownLatch(1)
            val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == "endpoint") saved.countDown()
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            try {
                device.findObject(By.res("android:id/button1")).click()
                assertTrue(saved.await(15, TimeUnit.SECONDS))
            } finally { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            assertEquals(endpoint, prefs.getString("endpoint", null))
        }
    }
    @Test fun bVerifyEndpointOnNewRelease() {
        org.junit.Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("releaseUpgrade") == "verify")
        assertEquals("0.3.3", context.packageManager.getPackageInfo(context.packageName, 0).versionName)
        assertEquals(endpoint, context.getSharedPreferences("byparr", Context.MODE_PRIVATE).getString("endpoint", null))
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertNull(device.findObject(By.clazz("android.widget.EditText")))
        }
    }
}
