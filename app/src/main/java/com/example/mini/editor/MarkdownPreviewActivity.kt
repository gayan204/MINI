package com.example.mini.editor

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.mini.R
import io.noties.markwon.Markwon

class MarkdownPreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_markdown_preview)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val markdownText = intent.getStringExtra("MARKDOWN_TEXT") ?: ""
        val textView: TextView = findViewById(R.id.markdown_preview)

        val markwon = Markwon.create(this)
        markwon.setMarkdown(textView, markdownText)
    }
}
