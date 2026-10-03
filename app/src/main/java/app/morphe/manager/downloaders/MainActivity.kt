package app.morphe.manager.downloaders

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.*
import android.database.ContentObserver
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.*
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var status: TextView
    private lateinit var apkTitle: TextView
    private lateinit var progress: ProgressBar
    private lateinit var open: Button
    private lateinit var cancel: Button
    private lateinit var choices: LinearLayout
    private lateinit var store: Downloads
    private val policy = DownloadPolicy(BuildConfig.DEBUG)
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
        fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
        fun color(id: Int) = getColor(id)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(16), dp(24), dp(20))
        }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(ImageView(this).apply { setImageResource(R.mipmap.ic_launcher); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO },
            LinearLayout.LayoutParams(dp(40), dp(40)))
        header.addView(TextView(this).apply {
            text = getString(R.string.app_name); textSize = 15f; setTextColor(color(R.color.text_secondary))
            setPadding(dp(10), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(ImageButton(this).apply {
            id = SETTINGS_ID; contentDescription = getString(R.string.server_settings)
            setImageResource(R.drawable.ic_settings)
            setBackgroundResource(android.R.drawable.list_selector_background)
            minimumWidth = dp(48); minimumHeight = dp(48)
            setOnClickListener { showServerSettings() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        root.addView(header)
        apkTitle = TextView(this).apply {
            id = APK_TITLE_ID; textSize = 23f; setTextColor(color(R.color.text_primary))
            setTypeface(typeface, Typeface.BOLD); maxLines = 3; ellipsize = TextUtils.TruncateAt.END
            setPadding(0, dp(20), 0, dp(16))
            text = browserPrefs.getString("title", null)?.let { "Downloading $it" } ?: "Download an original APK"
        }
        root.addView(apkTitle)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        progress.progressDrawable = getDrawable(R.drawable.download_progress)
        progress.indeterminateTintList = ColorStateList.valueOf(color(R.color.primary))
        root.addView(progress, LinearLayout.LayoutParams(-1, dp(8)))
        status = TextView(this).apply {
            text = getString(R.string.start_hint); textSize = 14f; setTextColor(color(R.color.text_secondary))
            setPadding(0, dp(14), 0, dp(12)); accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        root.addView(status)
        choices = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(choices, LinearLayout.LayoutParams(-1, -2))
        val actions = LinearLayout(this).apply { gravity = Gravity.END; setPadding(0, dp(8), 0, 0) }
        fun button(label: Int, action: () -> Unit): Button = Button(this).apply {
            setText(label); isAllCaps = false; textSize = 14f; minimumHeight = dp(48)
            setPadding(dp(12), 0, dp(12), 0); setTextColor(color(R.color.primary))
            setBackgroundResource(android.R.drawable.list_selector_background)
            setOnClickListener { action() }
            actions.addView(this, LinearLayout.LayoutParams(-2, -2))
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
        val content = object : ScrollView(this) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(
                    (resources.displayMetrics.heightPixels * 0.8).toInt(), MeasureSpec.AT_MOST))
            }
        }.apply {
            background = GradientDrawable().apply { setColor(color(R.color.surface)); cornerRadius = dp(28).toFloat() }
            clipToOutline = true; addView(root)
        }
        setContentView(content)
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setLayout(minOf(dp(420), resources.displayMetrics.widthPixels - dp(32)), WindowManager.LayoutParams.WRAP_CONTENT)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finish() }
        })
        // Recreation restores persisted state; its original VIEW intent was already handled.
        if (savedInstanceState != null || !openIncoming(intent)) {
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
        if (url == null) { status.text = "Open a supported download-site link."; return true }
        if (store.id >= 0 && !store.ready && store.error == null) {
            status.text = "Finish or cancel the current download before opening another link."; return true
        }
        cancelPage()
        browserPrefs.edit().putString("url", url).putBoolean("awaitingDownload", true).remove("title").commit()
        apkTitle.text = "Preparing your APK"
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
                "Enter your private HTTPS Byparr API URL. The saved server resolves download pages; Android downloads the original file.")
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

    private fun loadPage(url: String, resolvedPage: ByparrClient.Page? = null) {
        if (loadingPage || isDestroyed) return
        val endpoint = serverPrefs.getString("endpoint", null)
        if (endpoint == null) { status.text = "Configure your Byparr server to continue."; showServerSettings(); return }
        val allowed = if (resolvedPage != null && policy.downloadUrl(url, resolvedPage.content.source)) url else
            policy.pageUrl(url) ?: run { status.text = "Open a supported download-site link."; return }
        browserPrefs.edit().putString("url", resolvedPage?.content?.url ?: allowed)
            .putBoolean("awaitingDownload", true).remove("pageError").commit()
        choices.removeAllViews()
        loadingPage = true; progress.isIndeterminate = true
        status.text = if (resolvedPage == null) "Contacting Byparr service…" else "Resolving the download link…"
        refreshDownload()
        val generation = ++requestGeneration
        val client = ByparrClient(BuildConfig.DEBUG)
        pageClient = client
        executor.execute {
            try {
                val page = resolvedPage ?: client.fetch(endpoint, allowed)
                handler.post {
                    if (isDestroyed || generation != requestGeneration) return@post
                    browserPrefs.edit().putString("title", page.content.appName).commit()
                    apkTitle.text = "Downloading ${page.content.appName}"
                    status.text = "Resolving the download link…"
                }
                val next = if (resolvedPage != null) allowed else page.content.next
                val attachment = if (next != null && page.content.isAttachment(next)) client.attachment(page, next) else null
                handler.post {
                    if (isDestroyed || generation != requestGeneration) return@post
                    pageClient = null; loadingPage = false; progress.isIndeterminate = false
                    if (attachment != null) {
                        try {
                            store.enqueue(attachment.url, page.userAgent, attachment.disposition, attachment.mime,
                                page.content.url, attachment.cookieHeader, attachment.sendReferer)
                            browserPrefs.edit().putBoolean("awaitingDownload", false).commit()
                            refreshDownload()
                        } catch (e: Exception) { pageFailed(e.message ?: "Download could not start.") }
                    } else if (next != null) {
                        loadPage(next)
                    } else {
                        cancel.visibility = View.GONE
                        status.text = "Choose the required release or APK variant below."
                        for (choice in page.content.choices) choices.addView(Button(this).apply {
                            text = choice.label; isAllCaps = false
                            setTextColor(getColor(R.color.text_primary))
                            setOnClickListener { loadPage(choice.url, page.takeIf { it.content.isAttachment(choice.url) }) }
                        })
                        if (page.content.choices.isEmpty()) pageFailed("Byparr returned no usable ${page.content.source.label} download choices.")
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
        open.visibility = if (open.isEnabled) View.VISIBLE else View.GONE
        findViewById<Button>(SHARE_ID).visibility = open.visibility
        cancel.setText(if (loadingPage) R.string.cancel_page else R.string.cancel)
        cancel.visibility = if (loadingPage || (store.id >= 0 && !store.ready)) View.VISIBLE else View.GONE
        if (loadingPage || browserPrefs.getBoolean("awaitingDownload", false)) return
        browserPrefs.getString("pageError", null)?.let {
            status.text = it; progress.isIndeterminate = false
            return
        }
        if (store.ready) {
            apkTitle.text = "Ready: ${browserPrefs.getString("title", null) ?: store.displayName}"
            progress.isIndeterminate = false; progress.progress = 100
            status.text = "Download complete. Ready to open in Morphe."
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
            if (store.id >= 0) { store.fail("The system download was removed. Open the download link again."); refreshDownload() }
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
                status.text = "$verb…\n${state.downloaded / 1024} KB" +
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
        const val APK_TITLE_ID = 10004
    }
}
