package com.example.mini.editor

import android.content.Context
import android.widget.EditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AutoSaveManager(
    private val context: Context,
    private val editor: EditText
) {
    private var job: Job? = null
    private val tempFile = File(context.cacheDir, "autosave_buffer.tmp")

    fun start() {
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(10_000) // 10 seconds
                saveState()
            }
        }
    }

    fun stop() {
        job?.cancel()
    }

    private suspend fun saveState() {
        try {
            val content = withContext(Dispatchers.Main) { editor.text.toString() }
            withContext(Dispatchers.IO) { tempFile.writeText(content) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun recoverStateIfAvailable() {
        try {
            if (tempFile.exists()) {
                val content = tempFile.readText()
                if (content.isNotEmpty()) {
                    editor.setText(content)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
