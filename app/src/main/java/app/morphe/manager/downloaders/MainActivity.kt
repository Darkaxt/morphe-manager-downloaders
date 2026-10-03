package app.morphe.manager.downloaders

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.*
import android.database.ContentObserver
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var open: Button
    private lateinit var cancel: Button
    private lateinit var choices: LinearLayout
    private lateinit var store: Downloads
    private val policy = ApkMirrorPolicy(BuildConfig.DEBUG)
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private val browserPrefs by lazy { getSharedPreferences("browser", MODE_PRIVATE) }
    private val serverPrefs by lazy { getSharedPreferences("byparr", MODE_PRIVATE) }
    private var pageClient: ByparrClient? = null
    private var requestGeneration = 0
    private var loadingPage = false
    private var validating = false
    private var resumed = false
    private var settingsDialog: AlertDialog? = null
    private var lastPromptedFailure: Long? = null
    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) { refreshDownload() }
    }
    private val completion = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2) == store.id) refreshDownload()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Downloads(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(247, 250, 248))
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        }
        fun text(value: String, size: Float) = TextView(this).apply {
            text = value; textSize = size; setTextColor(Color.rgb(28, 51, 42)); setPadding(20, 12, 20, 12)
        }
        root.addView(text(getString(R.string.app_name), 21f))
        status = text(getString(R.string.start_hint), 15f)
        root.addView(status)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        root.addView(progress, LinearLayout.LayoutParams(-1, 8))
        val actions = LinearLayout(this)
        fun button(label: Int, action: () -> Unit): Button = Button(this).apply {
            setText(label); setOnClickListener { action() }
            actions.addView(this, LinearLayout.LayoutParams(0, -2, 1f))
        }
        open = button(R.string.open_morphe) { shareToMorphe() }.apply { isEnabled = false }
        button(R.string.share) { shareChooser() }.apply { id = SHARE_ID }
        cancel = button(R.string.cancel) {
            if (loadingPage) {
                cancelPage(); browserPrefs.edit().putBoolean("awaitingDownload", false).commit()
                status.text = "Page request cancelled."; refreshDownload()
            } else { store.cancel(); refreshDownload() }
        }
        root.addView(actions)
        root.addView(Button(this).apply {
            setText(R.string.server_settings); id = SETTINGS_ID
            setOnClickListener { showServerSettings() }
        })
        choices = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(choices) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finish() }
        })
        if (!openIncoming(intent)) {
            val url = browserPrefs.getString("url", null)
            if (browserPrefs.getBoolean("awaitingDownload", false) && url != null &&
                (store.id < 0 || store.ready || store.error != null)) loadPage(url)
            else if (serverPrefs.getString("endpoint", null) == null) showServerSettings()
            else browserPrefs.getString("pageError", null)?.let { status.text = it; showServerSettings(it) }
        }
    }

    private fun openIncoming(value: Intent): Boolean {
        val raw = value.dataString ?: return false
        val url = policy.pageUrl(raw)
        if (url == null) { status.text = "Open an APKMirror HTTP or HTTPS link."; return true }
        if (store.id >= 0 && !store.ready && store.error == null) {
            status.text = "Finish or cancel the current download before opening another link."; return true
        }
        cancelPage()
        browserPrefs.edit().putString("url", url).putBoolean("awaitingDownload", true).commit()
        loadPage(url)
        return true
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); openIncoming(intent) }
    override fun onResume() {
        super.onResume(); resumed = true
        ContextCompat.registerReceiver(this, completion, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED)
        contentResolver.registerContentObserver(Uri.parse("content://downloads/my_downloads"), true, observer)
        refreshDownload()
    }
    override fun onPause() {
        resumed = false; unregisterReceiver(completion); contentResolver.unregisterContentObserver(observer)
        super.onPause()
    }
    override fun onDestroy() {
        cancelPage(); settingsDialog?.dismiss(); executor.shutdown(); super.onDestroy()
    }

    private fun cancelPage() {
        requestGeneration++; pageClient?.cancel(); pageClient = null; loadingPage = false
    }
    private fun showServerSettings(failure: String? = null) {
        if (isDestroyed || isFinishing || settingsDialog?.isShowing == true) return
        val input = EditText(this).apply {
            id = ENDPOINT_ID
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            isSingleLine = true; hint = "https://your-private-server:8191/v1"
            setText(serverPrefs.getString("endpoint", ""))
        }
        val dialog = AlertDialog.Builder(this).setTitle("Byparr server")
            .setMessage((failure?.let { "$it\n\n" } ?: "") +
                "Enter your private HTTPS Byparr API URL. The saved server resolves APKMirror pages; Android downloads the original file.")
            .setView(input).setPositiveButton("Save and retry", null).setNegativeButton("Keep current state", null)
            .create()
        settingsDialog = dialog
        dialog.setOnDismissListener { if (settingsDialog === dialog) settingsDialog = null }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val endpoint = ByparrEndpoint.normalize(input.text.toString(), BuildConfig.DEBUG)
                if (endpoint == null) { input.error = "Enter a HTTPS server URL without credentials, query or fragment."; return@setOnClickListener }
                serverPrefs.edit().putString("endpoint", endpoint).commit()
                dialog.dismiss()
                val url = browserPrefs.getString("url", null)
                if (url != null && (store.id < 0 || store.ready || store.error != null)) {
                    cancelPage()
                    browserPrefs.edit().putBoolean("awaitingDownload", true).commit()
                    loadPage(url)
                } else if (store.id < 0) status.text = getString(R.string.start_hint)
            }
        }
        dialog.show()
    }

    private fun loadPage(url: String) {
        if (loadingPage || isDestroyed) return
        val endpoint = serverPrefs.getString("endpoint", null)
        if (endpoint == null) { status.text = "Configure your Byparr server to continue."; showServerSettings(); return }
        val allowed = policy.pageUrl(url) ?: run { status.text = "Only APKMirror page links are supported."; return }
        browserPrefs.edit().putString("url", allowed).putBoolean("awaitingDownload", true).remove("pageError").commit()
        choices.removeAllViews()
        loadingPage = true; progress.isIndeterminate = true
        status.text = "Resolving APKMirror through Byparr…"
        refreshDownload()
        val generation = ++requestGeneration
        val client = ByparrClient(BuildConfig.DEBUG)
        pageClient = client
        executor.execute {
            try {
                val page = client.fetch(endpoint, allowed)
                val next = page.content.next
                val attachment = if (next != null && page.content.isAttachment(next)) client.attachment(page, next) else null
                handler.post {
                    if (isDestroyed || generation != requestGeneration) return@post
                    pageClient = null; loadingPage = false; progress.isIndeterminate = false
                    if (attachment != null) {
                        try {
                            store.enqueue(attachment.url, page.userAgent, attachment.disposition, attachment.mime,
                                page.content.url, attachment.cookieHeader)
                            browserPrefs.edit().putBoolean("awaitingDownload", false).commit()
                            refreshDownload()
                        } catch (e: Exception) { pageFailed(e.message ?: "Download could not start.") }
                    } else if (next != null) {
                        loadPage(next)
                    } else {
                        cancel.visibility = View.GONE
                        status.text = "Choose the required release or APK variant below."
                        choices.addView(TextView(this).apply { text = page.content.title; textSize = 18f; setPadding(20, 20, 20, 20) })
                        for (choice in page.content.choices) choices.addView(Button(this).apply {
                            text = choice.label; isAllCaps = false
                            setOnClickListener { loadPage(choice.url) }
                        })
                        if (page.content.choices.isEmpty()) pageFailed("Byparr returned no usable APKMirror download choices.")
                    }
                }
            } catch (e: Exception) {
                handler.post {
                    if (isDestroyed || generation != requestGeneration) return@post
                    pageClient = null; loadingPage = false
                    pageFailed(e.message ?: "Byparr could not resolve the page.")
                }
            }
        }
    }
    private fun pageFailed(message: String) {
        browserPrefs.edit().putBoolean("awaitingDownload", false).putString("pageError", message).commit()
        progress.isIndeterminate = false; cancel.visibility = View.GONE
        status.text = message
        showServerSettings(message)
    }

    private fun refreshDownload() {
        if (isDestroyed) return
        open.isEnabled = store.ready && store.file?.isFile == true && !loadingPage &&
            !browserPrefs.getBoolean("awaitingDownload", false)
        findViewById<Button>(SHARE_ID).isEnabled = open.isEnabled
        cancel.setText(if (loadingPage) R.string.cancel_page else R.string.cancel)
        cancel.visibility = if (loadingPage || (store.id >= 0 && !store.ready)) View.VISIBLE else View.GONE
        if (loadingPage || browserPrefs.getBoolean("awaitingDownload", false)) return
        browserPrefs.getString("pageError", null)?.let {
            status.text = it; progress.isIndeterminate = false
            return
        }
        if (store.ready) {
            progress.isIndeterminate = false; progress.progress = 100
            status.text = "Ready: ${store.displayName}\nOriginal file preserved. Morphe handles patching in Expert mode."
            if (resumed && !store.autoOpened) shareToMorphe()
            return
        }
        store.error?.let {
            status.text = it; progress.isIndeterminate = false
            if (resumed && lastPromptedFailure != store.id) {
                lastPromptedFailure = store.id; showServerSettings(it)
            }
            return
        }
        val state = store.query() ?: run {
            progress.isIndeterminate = false
            if (store.id >= 0) { store.fail("The system download was removed. Retry the APKMirror link."); refreshDownload() }
            return
        }
        when (state.status) {
            DownloadManager.STATUS_SUCCESSFUL -> if (!validating) {
                validating = true; status.text = "Checking the downloaded archive…"
                executor.execute {
                    store.validate()
                    handler.post { validating = false; if (!isDestroyed) refreshDownload() }
                }
            }
            DownloadManager.STATUS_FAILED -> {
                store.fail("Download failed (Android reason ${state.reason}). Check the server URL and retry.")
                refreshDownload()
            }
            else -> {
                progress.isIndeterminate = state.total <= 0
                if (state.total > 0) progress.progress = ((state.downloaded * 100) / state.total).toInt()
                val verb = if (state.status == DownloadManager.STATUS_PAUSED) "Waiting for the network" else "Downloading"
                status.text = "$verb: ${store.displayName}\n${state.downloaded / 1024} KB" +
                    if (state.total > 0) " / ${state.total / 1024} KB" else ""
            }
        }
    }
    private fun shareToMorphe() {
        val file = store.file?.takeIf { store.ready && it.isFile } ?: return
        try {
            startActivity(MorpheHandoff.intent(this, file, store.displayName, store.format))
            store.markOpened()
        } catch (_: ActivityNotFoundException) {
            store.markOpened()
            AlertDialog.Builder(this).setTitle("Morphe is not installed")
                .setMessage("Install Morphe and enable Expert mode, then use Open in Morphe. You can also share the original file with another app.")
                .setPositiveButton("Share file") { _, _ -> shareChooser() }.setNegativeButton("Keep file", null).show()
        }
    }
    private fun shareChooser() {
        val file = store.file?.takeIf { store.ready && it.isFile } ?: return
        startActivity(Intent.createChooser(MorpheHandoff.intent(this, file, store.displayName, store.format, null), "Share original APK"))
    }
    companion object {
        private const val SHARE_ID = 10001
        const val SETTINGS_ID = 10002
        const val ENDPOINT_ID = 10003
    }
}
