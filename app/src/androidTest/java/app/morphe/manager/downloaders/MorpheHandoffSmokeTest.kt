package app.morphe.manager.downloaders

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Optional real-Morphe check after process-restore; inspect Morphe's selected APK UI afterward. */
@RunWith(AndroidJUnit4::class)
class MorpheHandoffSmokeTest {
    @Test fun openCompletedOriginalInInstalledMorphe() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue(runCatching { context.packageManager.getPackageInfo(MorpheHandoff.PACKAGE, 0) }.isSuccess)
        val store = Downloads(context)
        assertTrue("Run process-restore first to retain a validated original", store.ready)
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                activity.startActivity(MorpheHandoff.intent(activity, store.file!!, store.displayName, store.format))
            }
        }
    }
}
