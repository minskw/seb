package com.safeexam.browser.ui.exam

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.safeexam.browser.R
import com.safeexam.browser.databinding.ActivityExamBinding
import com.safeexam.browser.security.AntiCheatManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExamActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EXAM_ID      = "extra_exam_id"
        const val EXTRA_EXAM_URL     = "extra_exam_url"
        const val EXTRA_EXAM_NAME    = "extra_exam_name"
        const val EXTRA_EXAM_MAPEL   = "extra_exam_mapel"
        const val EXTRA_EXAM_TANGGAL = "extra_exam_tanggal"
        const val EXTRA_EXAM_WAKTU   = "extra_exam_waktu"
        const val EXTRA_EXAM_DURASI  = "extra_exam_durasi"
    }

    private lateinit var binding: ActivityExamBinding
    private val viewModel: ExamViewModel by viewModels()

    private var isExamActive  = false
    private var warningDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.antiCheatManager.applyScreenSecurity(this)

        binding = ActivityExamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val examId      = intent.getLongExtra(EXTRA_EXAM_ID, -1L)
        val examUrl     = intent.getStringExtra(EXTRA_EXAM_URL)     ?: ""
        val examName    = intent.getStringExtra(EXTRA_EXAM_NAME)    ?: "Ujian"
        val examMapel   = intent.getStringExtra(EXTRA_EXAM_MAPEL)   ?: ""
        val examTanggal = intent.getStringExtra(EXTRA_EXAM_TANGGAL) ?: ""
        val examWaktu   = intent.getStringExtra(EXTRA_EXAM_WAKTU)   ?: ""
        val examDurasi  = intent.getIntExtra(EXTRA_EXAM_DURASI, 0)

        if (examId == -1L || examUrl.isEmpty()) { finish(); return }

        supportActionBar?.hide()

        performPreExamChecks(examId, examUrl, examName, examMapel, examTanggal, examWaktu, examDurasi)
    }

    private fun performPreExamChecks(
        examId: Long, examUrl: String, examName: String,
        mapel: String, tanggal: String, waktu: String, durasi: Int
    ) {
        val checks = viewModel.antiCheatManager.performPreExamChecks()

        if (checks.isRooted) {
            showBlockedDialog(
                "Perangkat Diblokir",
                "Perangkat Anda terdeteksi menggunakan akses root. " +
                    "Ujian tidak dapat dijalankan pada perangkat ini."
            )
            return
        }

        val warnings = buildList {
            if (checks.isDevMode || checks.isUsbDebug)
                add("• Developer Options / USB Debugging aktif")
            if (checks.isVpn)
                add("• Koneksi VPN terdeteksi")
        }

        if (warnings.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("⚠️ Peringatan Keamanan")
                .setMessage(
                    "Kondisi berikut terdeteksi sebelum ujian:\n\n" +
                        warnings.joinToString("\n") +
                        "\n\nAnda dapat tetap melanjutkan, namun aktivitas ini telah dicatat."
                )
                .setPositiveButton("Lanjutkan") { _, _ ->
                    startExam(examId, examUrl, examName, mapel, tanggal, waktu, durasi)
                }
                .setNegativeButton("Keluar") { _, _ -> finish() }
                .setCancelable(false)
                .show()
        } else {
            startExam(examId, examUrl, examName, mapel, tanggal, waktu, durasi)
        }
    }

    private fun startExam(
        examId: Long, examUrl: String, examName: String,
        mapel: String, tanggal: String, waktu: String, durasi: Int
    ) {
        viewModel.initSession(examId)
        setupWebView(examUrl)
        setupObservers()

        // Populate header info
        binding.tvExamTitle.text  = examName
        binding.tvExamMapel.text  = mapel
        binding.tvExamInfo.text   = "$tanggal  •  $waktu  •  ${durasi} menit"

        isExamActive = true
        viewModel.onExamStarted()
        viewModel.antiCheatManager.enterKioskMode(this)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(url: String) {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled       = true
                domStorageEnabled       = true
                loadWithOverviewMode    = true
                useWideViewPort         = true
                builtInZoomControls     = false
                displayZoomControls     = false
                cacheMode               = WebSettings.LOAD_NO_CACHE
                setSupportMultipleWindows(false)
                allowFileAccess         = false
                allowContentAccess      = false
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView, request: WebResourceRequest
                ): Boolean {
                    val requestUrl = request.url.toString()
                    return if (requestUrl.contains("docs.google.com") ||
                        requestUrl.contains("accounts.google.com")) {
                        view.loadUrl(requestUrl); false
                    } else { true }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.progressBar.visibility = View.GONE
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    binding.progressBar.progress = newProgress
                    binding.progressBar.visibility =
                        if (newProgress < 100) View.VISIBLE else View.GONE
                }
            }

            loadUrl(url)
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.tvViolationCount.text =
                            "${state.violationCount}/${AntiCheatManager.MAX_VIOLATIONS}"
                        binding.tvViolationCount.visibility =
                            if (state.violationCount > 0) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.examEvent.collect { event ->
                        when (event) {
                            is ExamEvent.ShowWarning         -> showViolationWarning(event.message)
                            is ExamEvent.ViolationLimitReached -> showLimitReachedDialog()
                            is ExamEvent.ExamEnded           -> finishExam()
                        }
                    }
                }
            }
        }
    }

    private fun showViolationWarning(message: String) {
        val count = viewModel.uiState.value.violationCount
        val limit = AntiCheatManager.MAX_VIOLATIONS
        warningDialog?.dismiss()
        warningDialog = AlertDialog.Builder(this)
            .setTitle("⚠️ Pelanggaran Terdeteksi ($count/$limit)")
            .setMessage("$message\n\nLanjutan pelanggaran akan mengakibatkan ujian dihentikan secara otomatis.")
            .setPositiveButton("Mengerti") { d, _ -> d.dismiss() }
            .setCancelable(false)
            .show()
    }

    private fun showLimitReachedDialog() {
        warningDialog?.dismiss()
        AlertDialog.Builder(this)
            .setTitle("🚫 Ujian Dihentikan")
            .setMessage(
                "Anda telah mencapai batas maksimum pelanggaran (${AntiCheatManager.MAX_VIOLATIONS}). " +
                    "Ujian dihentikan secara otomatis dan laporan dikirim."
            )
            .setPositiveButton("OK") { _, _ -> finishExam() }
            .setCancelable(false)
            .show()
    }

    private fun showBlockedDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Keluar") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun finishExam() {
        isExamActive = false
        viewModel.antiCheatManager.exitKioskMode(this)
        finish()
    }

    override fun onResume() {
        super.onResume()
        if (isExamActive) {
            viewModel.onAppResumed()
            viewModel.antiCheatManager.checkMultiWindow(this)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && isExamActive) {
            viewModel.antiCheatManager.recordViolation(
                com.safeexam.browser.security.SecurityEventType.OVERLAY_DETECTED,
                "Window focus lost"
            )
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_MENU -> true
            else -> super.onKeyDown(keyCode, event)
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() { /* disabled during exam */ }

    override fun onNewIntent(intent: Intent?) { super.onNewIntent(intent) }

    override fun onDestroy() {
        warningDialog?.dismiss()
        binding.webView.destroy()
        super.onDestroy()
    }
}
