package app.morphe.manager.downloaders

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.*
import android.database.ContentObserver
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.view.View
import android.webkit.*
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONObject
import org.json.JSONTokener
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var web: WebView
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var open: Button
    private lateinit var cancel: Button
    private lateinit var store: Downloads
    private var awaitingDownload: Boolean
        get() = getSharedPreferences("browser", MODE_PRIVATE).getBoolean("awaitingDownload", false)
        set(value) { getSharedPreferences("browser", MODE_PRIVATE).edit().putBoolean("awaitingDownload", value).commit() }
    private val policy = ApkMirrorPolicy(BuildConfig.DEBUG)
    private val executor = Executors.newSingleThreadExecutor()
    private var validating = false
    private var resumed = false
    private var lastAutomaticUrl: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) { refreshDownload() }
    }
    private val completion = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2) == store.id) refreshDownload()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
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
        cancel = button(R.string.cancel) { store.cancel(); refreshDownload() }
        root.addView(actions)
        val next = Button(this).apply {
            setText(R.string.continue_download)
            setOnClickListener {
                if (store.id < 0 || store.ready || store.error != null) { awaitingDownload = true; inspectPage() }
            }
        }
        root.addView(next)
        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (!request.isForMainFrame) return false
                    val allowed = policy.pageUrl(request.url.toString())
                    if (allowed == null) { status.text = "This companion supports APKMirror links only."; return true }
                    if (request.hasGesture() && (store.id < 0 || store.ready || store.error != null)) awaitingDownload = true
                    if (allowed != request.url.toString()) { view.loadUrl(allowed); return true }
                    return false
                }
                override fun onPageFinished(view: WebView, url: String) {
                    getSharedPreferences("browser", MODE_PRIVATE).edit().putString("url", url).apply()
                    if (awaitingDownload && (store.id < 0 || store.ready || store.error != null)) inspectPage()
                }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) status.text = "APKMirror could not load: ${error.description}"
                }
                // Certificate failures use WebView's default cancellation. Never bypass SSL checks.
            }
            setDownloadListener { url, agent, disposition, mime, _ ->
                if (!awaitingDownload) return@setDownloadListener
                if (!policy.downloadUrl(url)) { status.text = "APKMirror returned an unsupported download host." }
                else try {
                    store.enqueue(url, agent, disposition, mime, this.url.orEmpty())
                    awaitingDownload = false
                    stopLoading(); refreshDownload()
                } catch (e: Exception) { status.text = e.message ?: "Download could not start." }
            }
        }
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { goBack() }
        })
        if (savedInstanceState == null || web.restoreState(savedInstanceState) == null) {
            if (!openIncoming(intent)) {
                getSharedPreferences("browser", MODE_PRIVATE).getString("url", null)?.let {
                    policy.pageUrl(it)?.let(web::loadUrl)
                }
            }
        }
    }
    private fun openIncoming(value: Intent): Boolean {
        val raw = value.dataString ?: return false
        val url = policy.pageUrl(raw)
        if (url == null) { status.text = "Open an APKMirror HTTP or HTTPS link."; return true }
        if (store.id >= 0 && !store.ready && store.error == null) {
            status.text = "Finish or cancel the current download before opening another link."; return true
        }
        lastAutomaticUrl = null
        awaitingDownload = true
        web.loadUrl(url)
        return true
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); openIncoming(intent) }
    override fun onSaveInstanceState(outState: Bundle) { web.saveState(outState); super.onSaveInstanceState(outState) }
    override fun onResume() {
        super.onResume(); resumed = true
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(this, completion, filter, ContextCompat.RECEIVER_EXPORTED)
        contentResolver.registerContentObserver(Uri.parse("content://downloads/my_downloads"), true, observer)
        refreshDownload()
    }
    override fun onPause() {
        resumed = false; unregisterReceiver(completion); contentResolver.unregisterContentObserver(observer)
        super.onPause()
    }
    override fun onDestroy() { web.destroy(); executor.shutdown(); super.onDestroy() }
    private fun goBack() { if (web.canGoBack()) web.goBack() else finish() }

    private fun inspectPage() {
        if (!awaitingDownload || policy.pageUrl(web.url.orEmpty()) == null) return
        val script = assets.open("apkmirror.js").bufferedReader().use { it.readText() }
        web.evaluateJavascript(script) { result ->
            if (isDestroyed || !awaitingDownload) return@evaluateJavascript
            try {
                val page = JSONObject(JSONTokener(result).nextValue() as String)
                val next = page.optString("next").takeUnless { it.isBlank() || it == "null" }
                when {
                    page.optBoolean("challenge") -> status.text = "Complete APKMirror's verification below."
                    next != null && policy.pageUrl(next) != null && next != lastAutomaticUrl -> {
                        lastAutomaticUrl = next; status.text = "Following APKMirror's download step…"
                        // Navigate in the page's browser context so redirects retain the normal Referer.
                        web.evaluateJavascript("window.location.assign(${JSONObject.quote(policy.pageUrl(next)!!)})", null)
                    }
                    page.optJSONArray("variants")?.length()?.let { it > 1 } == true ->
                        status.text = "Choose the required APK variant below. The download continues automatically."
                    else -> status.text = "Choose the required release or variant below."
                }
            } catch (_: Exception) { status.text = "Use APKMirror's download controls below." }
        }
    }

    private fun refreshDownload() {
        if (isDestroyed) return
        open.isEnabled = store.ready && store.file?.isFile == true
        findViewById<Button>(SHARE_ID).isEnabled = open.isEnabled
        cancel.visibility = if (store.id >= 0 && !store.ready) View.VISIBLE else View.GONE
        if (store.ready) {
            progress.isIndeterminate = false; progress.progress = 100
            status.text = "Ready: ${store.displayName}\nOriginal file preserved. Morphe handles patching in Expert mode."
            if (resumed && !store.autoOpened) shareToMorphe()
            return
        }
        store.error?.let { status.text = it; progress.isIndeterminate = false; return }
        val state = store.query() ?: run {
            progress.isIndeterminate = false
            if (store.id >= 0) { store.fail("The system download was removed. Open the APKMirror link to download it again."); refreshDownload() }
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
                store.fail("Download failed (Android reason ${state.reason}). Open the APKMirror link to try again.")
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
    companion object { private const val SHARE_ID = 10001 }
}
