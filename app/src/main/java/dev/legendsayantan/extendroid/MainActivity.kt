package dev.legendsayantan.extendroid

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.initialize
import com.google.firebase.messaging.FirebaseMessaging
import dev.legendsayantan.extendroid.Utils.Companion.isShizukuAllowed
import dev.legendsayantan.extendroid.Utils.Companion.isShizukuSetup
import dev.legendsayantan.extendroid.Utils.Companion.miuiRequirements
import dev.legendsayantan.extendroid.echo.EchoNetworkUtils
import dev.legendsayantan.extendroid.echo.EchoState
import dev.legendsayantan.extendroid.lib.Logging
import dev.legendsayantan.extendroid.lib.MediaCore
import dev.legendsayantan.extendroid.lib.MediaCore.Companion.requestMediaProjection
import dev.legendsayantan.extendroid.services.ExtendService
import dev.legendsayantan.extendroid.services.ExtendService.Companion.svcIntent
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import java.util.*
import kotlin.concurrent.timerTask

class MainActivity : AppCompatActivity() {
    val prefs by lazy { Prefs(applicationContext) }
    val logging by lazy { Logging(applicationContext) }
    var onPauseTask = {}

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HiddenApiBypass.addHiddenApiExemptions("");
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        setupFirebase()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        prefs.registerConfigChangeListener {
            handleSections()
        }
        refreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay;
        }.refreshRate.toInt()
    }

    /**
     * Echo is an optional, server backed feature. Builds that ship without a Firebase
     * configuration must still start normally, so a failure here only disables Echo.
     */
    private fun setupFirebase() {
        if (!Utils.hasFirebaseConfig(this)) {
            EchoState.available = false
            logging.i("No Firebase configuration found, Echo is disabled.", "MainActivity")
            return
        }
        try {
            Firebase.initialize(context = this)
            Firebase.appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance(),
            )
            FirebaseMessaging.getInstance().isAutoInitEnabled = false
            EchoState.available = true
        } catch (t: Throwable) {
            EchoState.available = false
            logging.e(t as? Exception ?: Exception(t), "MainActivity")
        }
    }

    override fun onPause() {
        super.onPause()
        onPauseTask()
    }

    override fun onResume() {
        super.onResume()
        initialiseSetupMenu()
        handleSections()
        initialiseConfigure()
        initialiseBottomBar()
        startForegroundService()
    }

    private fun initialiseSetupMenu() {
        val setupImage = findViewById<ImageView>(R.id.setupShizukuImage)
        val allowImage = findViewById<ImageView>(R.id.allowShizukuImage)


        if (isShizukuSetup()) {
            setupImage.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.round_done_24,
                    null
                )
            )
            if (isShizukuAllowed()) {
                allowImage.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.round_done_24,
                        null
                    )
                )
            } else {
                allowImage.setOnClickListener {
                    Shizuku.addRequestPermissionResultListener { _: Int, i1: Int ->
                        if (i1 == PackageManager.PERMISSION_GRANTED) {
                            initialiseSetupMenu()
                        }
                    }
                    Shizuku.requestPermission(0)
                }
            }
        } else {
            setupImage.setOnClickListener {
                val intent =
                    packageManager.getLaunchIntentForPackage(getString(R.string.package_shizuku))
                        ?: Intent(Intent.ACTION_VIEW).apply {
                            setData(getString(R.string.url_shizuku).toUri())
                        }
                startActivity(intent)
            }
        }
    }

    private fun handleSections() {

        val setupCard = findViewById<MaterialCardView>(R.id.cardSetup)
        val configCard = findViewById<MaterialCardView>(R.id.cardConfigure)
        val setupHeader = findViewById<MaterialCardView>(R.id.headerSetup)
        val configHeader = findViewById<MaterialCardView>(R.id.headerConfigure)
        val configBlocker = findViewById<LinearLayout>(R.id.configBlocker)
        val ready = isShizukuSetup() && isShizukuAllowed()

        setupCard.isEnabled = !ready
        configCard.isEnabled = ready
        configBlocker.isVisible = !ready
        setupCard.alpha = if (ready) 0.6f else 1f
        setupHeader.alpha = if (ready) 0.6f else 1f
        configCard.alpha = if (ready) 1f else 0.6f
        configHeader.alpha = if (ready) 1f else 0.6f
    }


    private fun initialiseConfigure() {
        val floatingBall = findViewById<MaterialSwitch>(R.id.floatingBall)
        val tasksRow = findViewById<LinearLayout>(R.id.tasksRow)
        val workspaceRow = findViewById<LinearLayout>(R.id.workspaceRow)
        val languageRow = findViewById<LinearLayout>(R.id.languageRow)
        val languageValue = findViewById<TextView>(R.id.languageValue)
        val helpRow = findViewById<LinearLayout>(R.id.helpRow)
        val aboutRow = findViewById<LinearLayout>(R.id.aboutRow)

        languageValue.text = LocaleHelper.displayName(this, prefs.language)
        languageRow.setOnClickListener { showLanguageDialog() }
        aboutRow.setOnClickListener { Utils.showAboutDialog(this) }
        helpRow.setOnClickListener {
            Utils.showInfoDialog(
                this,
                getString(R.string.help_title),
                getString(R.string.help_text)
            ) { }
        }

        floatingBall.isChecked = prefs.floatingBall

        floatingBall.setOnCheckedChangeListener { _, checked ->
            prefs.floatingBall = checked
        }
        tasksRow.setOnClickListener {
            //TODO: implement tasks
        }
        workspaceRow.setOnClickListener {
            Utils.showWorkspaceNameDialog(this) { name ->
                val intent = Intent(this, WorkspaceActivity::class.java).apply {
                    putExtra(WorkspaceActivity.EXTRA_WORKSPACE_NAME, name)
                }
                startActivity(intent)
            }
        }
    }

    fun initialiseBottomBar() {
        val echoBtn = findViewById<MaterialCardView>(R.id.echoBtn)
        val echoTxt = findViewById<TextView>(R.id.echoTxt)
        val logBtn = findViewById<MaterialCardView>(R.id.logBtn)
        val stopBtn = findViewById<ImageView>(R.id.stopBtn)

        val openEcho = {
            val intent = Intent(this@MainActivity, EchoActivity::class.java)
            startActivity(intent)
        }
        echoBtn.setOnClickListener {
            if (prefs.disclaimerTextShown) {
                openEcho()
            } else {
                Toast.makeText(applicationContext, R.string.please_wait, Toast.LENGTH_SHORT).show()
                Utils.showInfoDialog(this@MainActivity, getString(R.string.disclaimer),
                    "${getString(R.string.agreed_to_tos)} ${getString(R.string.url_homepage_web)}/tos"
                ) {
                    prefs.disclaimerTextShown = true
                    runOnUiThread {
                        openEcho()
                    }
                }
            }

        }
        echoBtn.visibility = if (EchoState.available) View.VISIBLE else View.GONE
        echoBtn.isEnabled = Utils.isSafeForUI(this)
        echoBtn.alpha = if (Utils.isSafeForUI(this)) 1f else 0.6f
        if (ExtendService.svc == null) {
            stopBtn.visibility = View.GONE
        } else {
            stopBtn.visibility = View.VISIBLE
            stopBtn.setOnClickListener {
                try {
                    stopService(svcIntent)
                } catch (_: Exception) { }
                onPauseTask = {
                    finishAffinity()
                    onDestroy()
                }
                Toast.makeText(
                    applicationContext,
                    getString(R.string.stopping_services),
                    Toast.LENGTH_LONG
                ).show()
                initialiseSetupMenu()
                handleSections()
                initialiseConfigure()
                initialiseBottomBar()
            }
        }
        echoTxt.isSelected = true
        echoTxt.text =
            if (EchoState.available && FirebaseAuth.getInstance().currentUser != null && prefs.fcmSent && ExtendService.svc != null) {
                EchoNetworkUtils.trySyncBoostersWithServer(applicationContext)
                getString(R.string.echo_boosters, String.format("%.2f", prefs.balance))
            } else {
                getString(R.string.extendroid_echo)
            }

        logBtn.setOnClickListener {
            startActivity(Intent(applicationContext, LogsActivity::class.java))
        }
    }

    private fun showLanguageDialog() {
        val tags = LocaleHelper.selectable
        val labels = tags.map { LocaleHelper.displayName(this, it) }.toTypedArray()
        val checked = tags.indexOf(prefs.language).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.language)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                dialog.dismiss()
                val tag = tags[which]
                if (tag != prefs.language) {
                    LocaleHelper.setLanguage(this, tag)
                    Toast.makeText(this, R.string.language_changed, Toast.LENGTH_SHORT).show()
                    // The overlay menu lives in a service, ask it to rebuild in the new language.
                    try {
                        startService(
                            Intent(this, ExtendService::class.java)
                                .setAction(ExtendService.ACTION_RELOAD_LOCALE)
                        )
                    } catch (_: Exception) {
                    }
                    recreate()
                }
            }
            .show()
    }

    private fun startForegroundService() {
        if (!isShizukuSetup() || !isShizukuAllowed()) return
        if (ExtendService.svc != null) return
        if (!prefs.allowedMiuiPerms) {
            miuiRequirements()
            prefs.allowedMiuiPerms = true
        }
        svcIntent = Intent(applicationContext, ExtendService::class.java)
        startForegroundService(svcIntent)
        val projectionScheduler = Timer()
        projectionScheduler.schedule(timerTask {
            if(ExtendService.svc!=null){
                requestMediaProjection()
                this.cancel()
                projectionScheduler.cancel()
            }
        },500,500)
        if (intent.action == ACTION_AUTOSTART || intent.hasExtra(EXTRA_AUTOSTART)) {
            val closeTimer = Timer()
            closeTimer.schedule(timerTask {
                if (ExtendService.svc != null && MediaCore.mInstance?.projection != null) {
                    this.cancel()
                    closeTimer.cancel()
                    finishAffinity()
                }
            }, 500, 500)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            MediaCore.onMediaProjectionResult(requestCode, resultCode, data)
        }catch (e: Exception){

        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
    }


    companion object {
        var refreshRate : Int = 60
        const val ACTION_AUTOSTART = "dev.legendsayantan.extendroid.action.autostart"
        const val EXTRA_AUTOSTART = "dev.legendsayantan.extendroid.extra.autostart"
    }

}