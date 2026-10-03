package app.morphe.manager.downloaders

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

/** Optional installed-LinkSheet check; inspect its chooser and select the companion afterward. */
@RunWith(AndroidJUnit4::class)
class LinkSheetSmokeTest {
    @Test fun openApkMirrorLinkInInstalledLinkSheet() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                activity.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.apkmirror.com/apk/google-inc/youtube/youtube-20-37-41-release/"))
                    .setPackage("fe.linksheet"))
            }
        }
    }
}
