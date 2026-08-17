package com.example.mini.vcs

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.mini.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VersionHistoryActivity : AppCompatActivity() {

    private lateinit var versionControlManager: VersionControlManager
    private lateinit var uri: Uri
    private lateinit var versions: List<VersionInfo>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_version_history)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val uriString = intent.getStringExtra("FILE_URI")
        if (uriString == null) {
            Toast.makeText(this, "No file specified", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        uri = Uri.parse(uriString)

        versionControlManager = VersionControlManager(this)
        loadVersions()
    }

    private fun loadVersions() {
        versions = versionControlManager.getVersions(uri)
        val listView: ListView = findViewById(R.id.list_versions)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val displayStrings = versions.map {
            "${it.versionName} - ${dateFormat.format(Date(it.timestamp))}"
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayStrings)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val version = versions[position]
            showDiffDialog(version)
        }
    }

    private fun showDiffDialog(version: VersionInfo) {
        val formattedDiff = formatDiffText(version.patchString)
        AlertDialog.Builder(this)
            .setTitle("Diff: ${version.versionName}")
            .setMessage(formattedDiff)
            .setPositiveButton("Restore") { _, _ ->
                val restoredContent = versionControlManager.getVersionState(uri, version.timestamp)
                if (restoredContent != null) {
                    val resultIntent = Intent().apply {
                        putExtra("RESTORED_CONTENT", restoredContent)
                    }
                    setResult(RESULT_OK, resultIntent)
                    Toast.makeText(this, "Restore action triggered for ${version.versionName}", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to restore version", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun formatDiffText(patchString: String): CharSequence {
        if (patchString.isBlank()) return "Initial Version (No diff)"
        
        val builder = SpannableStringBuilder()
        val lines = patchString.split("\n")
        
        for (line in lines) {
            val start = builder.length
            builder.append(line).append("\n")
            
            val span = when {
                line.startsWith("+") && !line.startsWith("+++") -> ForegroundColorSpan(Color.parseColor("#4CAF50")) // Green
                line.startsWith("-") && !line.startsWith("---") -> ForegroundColorSpan(Color.parseColor("#F44336")) // Red
                line.startsWith("@@") -> ForegroundColorSpan(Color.parseColor("#2196F3")) // Blue
                else -> ForegroundColorSpan(Color.parseColor("#757575")) // Grey for context
            }
            builder.setSpan(span, start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return builder
    }
}
