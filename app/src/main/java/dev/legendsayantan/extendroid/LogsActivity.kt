package dev.legendsayantan.extendroid

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import dev.legendsayantan.extendroid.adapters.LogAdapter
import dev.legendsayantan.extendroid.lib.Logging

class LogsActivity : AppCompatActivity() {
    val logging by lazy { Logging(applicationContext) }
    val daysLimit by lazy { findViewById<EditText>(R.id.days) }
    val ascending by lazy { findViewById<RadioButton>(R.id.ascending) }
    val descending by lazy { findViewById<RadioButton>(R.id.descending) }

    val clearLogsBtn by lazy { findViewById<MaterialButton>(R.id.clearLogs) }
    val copyLogsBtn by lazy { findViewById<TextView>(R.id.copyLogs) }
    val shareLogsBtn by lazy { findViewById<TextView>(R.id.shareLogs) }

    /** Logs currently shown, in the order they are displayed. */
    @Volatile
    private var currentLogs: Map<String, String> = emptyMap()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_logs)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        updateLogView()
        var lastValidDays = 1
        daysLimit.doOnTextChanged{ text,_,_,_ ->
            if(text.isNullOrEmpty() || text.toString().toIntOrNull() == null || text.toString().toInt() < 0){
                daysLimit.error = getString(R.string.invalid_number)
            }else{
                daysLimit.error = null
                lastValidDays = text.toString().toInt()
                updateLogView()
            }
        }
        ascending.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked) updateLogView()
        }
        descending.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) updateLogView()
        }
        clearLogsBtn.setOnClickListener {
            Thread{
                logging.clearLogsOlderThan(lastValidDays)
                runOnUiThread {
                    Toast.makeText(applicationContext, getString(R.string.logs_cleared, lastValidDays), Toast.LENGTH_LONG).show()
                    updateLogView()
                }
            }.start()
        }
        copyLogsBtn.setOnClickListener { copyToClipboard(buildReport(currentLogs)) }
        shareLogsBtn.setOnClickListener { shareReport(buildReport(currentLogs)) }
    }

    fun updateLogView() {
        val recycler = findViewById<RecyclerView>(R.id.logsView);
        recycler.adapter = null
        Toast.makeText(applicationContext, getString(R.string.loading), Toast.LENGTH_SHORT).show()
        val days = daysLimit.text.toString().toIntOrNull() ?: 1
        val newestFirst = descending.isChecked
        Thread {
            //fetch logs
            val logs = Logging(applicationContext).getLogsOf(days, newestFirst)
            runOnUiThread {
                currentLogs = logs
                recycler.adapter = LogAdapter(logs) { entry ->
                    copyToClipboard(
                        getString(
                            R.string.log_single_entry,
                            entry.time,
                            entry.level.name,
                            entry.message
                        )
                    )
                }
            }
        }.start()
    }

    /** Copy + share text: a short device header makes pasted reports far easier to debug. */
    private fun buildReport(logs: Map<String, String>): String {
        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
        } catch (_: Exception) {
            "?"
        }
        val header = getString(
            R.string.log_report_header,
            version,
            Build.VERSION.RELEASE,
            "${Build.MANUFACTURER} ${Build.MODEL} (SDK ${Build.VERSION.SDK_INT})"
        )
        val body = StringBuilder(header).append("\n\n")
        logs.forEach { (key, message) ->
            body.append(key).append('\n').append(message).append('\n')
        }
        return body.toString()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.logs), text))
        Toast.makeText(this, R.string.logs_copied, Toast.LENGTH_SHORT).show()
    }

    private fun shareReport(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_logs_title))
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_logs_title)))
    }
}
