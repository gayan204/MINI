package com.example.mini

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.example.mini.editor.AutoSaveManager
import com.example.mini.editor.MarkdownPreviewActivity
import com.example.mini.editor.SyntaxHighlighter
import com.example.mini.editor.UndoRedoManager
import com.example.mini.file.FileManager
import com.example.mini.vcs.VersionControlManager
import com.example.mini.vcs.VersionHistoryActivity
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var editor: EditText
    private lateinit var undoRedoManager: UndoRedoManager
    private lateinit var autoSaveManager: AutoSaveManager
    private lateinit var fileManager: FileManager
    private lateinit var versionControlManager: VersionControlManager
    private lateinit var syntaxHighlighter: SyntaxHighlighter

    private var currentFileUri: Uri? = null
    private var currentFileExtension: String = "" // tracks active file type mode

    private val openFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            currentFileUri = it
            // Fix: Decode URI path properly to extract real file extension
            val ext = extractExtension(it)
            currentFileExtension = ext
            syntaxHighlighter.setFileExtension(ext)
            lifecycleScope.launch {
                val content = withContext(Dispatchers.IO) { fileManager.readFile(it) }
                editor.setText(content)
                Toast.makeText(this@MainActivity, "File opened", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val saveFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
        uri?.let {
            currentFileUri = it
            // Fix: Decode URI path properly to extract real file extension
            val ext = extractExtension(it)
            currentFileExtension = ext
            syntaxHighlighter.setFileExtension(ext)
            lifecycleScope.launch {
                val textToSave = editor.text.toString()
                withContext(Dispatchers.IO) { fileManager.saveFile(it, textToSave) }
                Toast.makeText(this@MainActivity, "File saved", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val versionHistoryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val restoredContent = result.data?.getStringExtra("RESTORED_CONTENT")
            if (restoredContent != null) {
                editor.setText(restoredContent)
                Toast.makeText(this, "Version restored", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        editor = findViewById(R.id.editor)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        // Setup Editor Features
        undoRedoManager = UndoRedoManager(editor)
        syntaxHighlighter = SyntaxHighlighter() // No extension = plain text, no keyword highlight
        editor.addTextChangedListener(undoRedoManager)
        editor.addTextChangedListener(syntaxHighlighter)

        fileManager = FileManager(this)
        versionControlManager = VersionControlManager(this)
        // Fix: Pass lifecycleScope to prevent coroutine leak
        autoSaveManager = AutoSaveManager(this, editor, lifecycleScope)
        autoSaveManager.recoverStateIfAvailable()

        // Fix: Use OnBackPressedCallback instead of deprecated onBackPressed()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        autoSaveManager.start()
    }

    override fun onStop() {
        super.onStop()
        autoSaveManager.stop()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.editor_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_undo -> {
                undoRedoManager.undo()
                true
            }
            R.id.action_redo -> {
                undoRedoManager.redo()
                true
            }
            R.id.action_wrap -> {
                item.isChecked = !item.isChecked
                editor.setHorizontallyScrolling(!item.isChecked)
                true
            }
            R.id.action_search -> {
                showSearchDialog()
                true
            }
            R.id.action_replace -> {
                showReplaceDialog()
                true
            }
            R.id.action_preview_markdown -> {
                val intent = Intent(this, MarkdownPreviewActivity::class.java).apply {
                    putExtra("MARKDOWN_TEXT", editor.text.toString())
                }
                startActivity(intent)
                true
            }
            R.id.action_read_only -> {
                item.isChecked = !item.isChecked
                editor.isEnabled = !item.isChecked
                true
            }
            R.id.action_save_version -> {
                if (currentFileUri != null) {
                    showSaveVersionDialog()
                } else {
                    Toast.makeText(this, "Save the file first before versioning.", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_file_type -> {
                showFileTypeDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Fix: Properly decode content URI to extract file extension.
     * content:// URIs have encoded paths like "primary:Documents/file.kt"
     */
    private fun extractExtension(uri: Uri): String {
        val path = uri.lastPathSegment ?: return ""
        val decoded = Uri.decode(path)
        return decoded.substringAfterLast('.', "").lowercase()
    }

    private fun showFileTypeDialog() {
        val types = arrayOf("Plain Text (.txt)", "Kotlin (.kt)", "Markdown (.md)")
        val extensions = arrayOf("", "kt", "md")
        val currentIndex = extensions.indexOf(currentFileExtension).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("File Type")
            .setSingleChoiceItems(types, currentIndex) { dialog, which ->
                currentFileExtension = extensions[which]
                syntaxHighlighter.setFileExtension(currentFileExtension)
                // Fix: Directly trigger re-highlight via afterTextChanged instead of re-assigning text
                syntaxHighlighter.rehighlight(editor.text)
                Toast.makeText(this, "${types[which]} mode applied", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSaveVersionDialog() {
        val input = EditText(this).apply { hint = "Version Name (e.g., v1.0)" }
        AlertDialog.Builder(this)
            .setTitle("Save Version")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val versionName = input.text.toString()
                if (versionName.isNotBlank() && currentFileUri != null) {
                    // Fix: Run DB operation on IO thread to avoid ANR
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            versionControlManager.saveVersion(currentFileUri!!, editor.text.toString(), versionName)
                        }
                        Toast.makeText(this@MainActivity, "Version saved", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSearchDialog() {
        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Search")
            .setView(input)
            .setPositiveButton("Find") { _, _ ->
                val query = input.text.toString()
                val text = editor.text.toString()
                val index = text.indexOf(query, ignoreCase = true)
                if (index != -1) {
                    editor.setSelection(index, index + query.length)
                    editor.requestFocus()
                } else {
                    Toast.makeText(this, "Not found", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showReplaceDialog() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val searchInput = EditText(this).apply { hint = "Search" }
        val replaceInput = EditText(this).apply { hint = "Replace with" }
        layout.addView(searchInput)
        layout.addView(replaceInput)

        AlertDialog.Builder(this)
            .setTitle("Replace")
            .setView(layout)
            .setPositiveButton("Replace All") { _, _ ->
                val query = searchInput.text.toString()
                val replacement = replaceInput.text.toString()
                val newText = editor.text.toString().replace(query, replacement, ignoreCase = true)
                editor.setText(newText)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_new -> {
                editor.setText("")
                currentFileUri = null
                currentFileExtension = ""
                syntaxHighlighter.setFileExtension("")
                Toast.makeText(this, "New file created", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_open -> {
                openFileLauncher.launch(arrayOf("text/plain", "*/*"))
            }
            R.id.nav_save -> {
                if (currentFileUri != null) {
                    lifecycleScope.launch {
                        val textToSave = editor.text.toString()
                        withContext(Dispatchers.IO) { fileManager.saveFile(currentFileUri!!, textToSave) }
                        Toast.makeText(this@MainActivity, "File saved", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    saveFileLauncher.launch("Untitled.txt")
                }
            }
            R.id.nav_save_as -> {
                saveFileLauncher.launch("Untitled.txt")
            }
            R.id.nav_history -> {
                if (currentFileUri != null) {
                    val intent = Intent(this, VersionHistoryActivity::class.java).apply {
                        putExtra("FILE_URI", currentFileUri.toString())
                    }
                    versionHistoryLauncher.launch(intent)
                } else {
                    Toast.makeText(this, "Save the file first before viewing history.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}