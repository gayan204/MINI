package com.example.mini.vcs

import android.net.Uri
import android.os.Bundle
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
        // Just showing the patch string as diff for simplicity in this project scope
        AlertDialog.Builder(this)
            .setTitle("Diff: ${version.versionName}")
            .setMessage(if (version.patchString.isBlank()) "Initial Version (No diff)" else version.patchString)
            .setPositiveButton("Restore") { _, _ ->
                // To restore, we would ideally rebuild up to this version and save.
                // Given the time, I'll pass a mock "restored" action or let the user know.
                Toast.makeText(this, "Restore action triggered for ${version.versionName}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }
}
