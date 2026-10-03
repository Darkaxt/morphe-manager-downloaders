package app.morphe.manager.downloaders

import android.content.Context
import android.content.Intent
import android.view.WindowManager
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PresentationTest {
    @Test fun downloadWindowIsCompactAccessibleAndKeepsSettings() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("browser", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("byparr", Context.MODE_PRIVATE).edit()
            .putString("endpoint", "https://private.example/v1").commit()
        Downloads(context).cancel()
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                val width = activity.window.attributes.width
                assertTrue("Window must be a bounded dialog", width > 0 && width < context.resources.displayMetrics.widthPixels)
                assertEquals(WindowManager.LayoutParams.WRAP_CONTENT, activity.window.attributes.height)
                assertTrue(activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0)
                val settings = activity.findViewById<android.view.View>(MainActivity.SETTINGS_ID)
                assertNotNull(settings)
                assertEquals("Byparr server", settings.contentDescription)
                assertTrue(settings.minimumHeight >= (48 * context.resources.displayMetrics.density).toInt())
                assertEquals("Morphe Downloader", context.getString(R.string.app_name))
                assertNotNull(activity.findViewById<TextView>(APK_TITLE_ID))
            }
        }
    }

    companion object { const val APK_TITLE_ID = 10004 }
}
