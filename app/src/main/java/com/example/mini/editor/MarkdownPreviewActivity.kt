package com.example.mini.editor

import android.os.Bundle
import android.webkit.WebView
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
        val webView: WebView = findViewById(R.id.markdown_preview)

        // Build Markwon and convert markdown -> Spanned -> HTML string
        val markwon = Markwon.create(this)
        val spanned = markwon.toMarkdown(markdownText)

        // Render in WebView for proper list, heading, bold/italic support
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="utf-8"/>
            <meta name="viewport" content="width=device-width, initial-scale=1"/>
            <style>
                body {
                    font-family: sans-serif;
                    font-size: 16px;
                    padding: 16px;
                    color: #212121;
                    line-height: 1.6;
                }
                h1, h2, h3, h4, h5, h6 { color: #311B92; }
                ul, ol { padding-left: 20px; }
                li { margin-bottom: 4px; }
                code { background: #f5f5f5; padding: 2px 4px; border-radius: 3px; }
                pre { background: #f5f5f5; padding: 12px; border-radius: 6px; overflow-x: auto; }
                blockquote { border-left: 4px solid #7E57C2; margin: 0; padding-left: 12px; color: #555; }
                strong { font-weight: bold; }
                em { font-style: italic; }
            </style>
            </head>
            <body>
            ${android.text.Html.toHtml(spanned, android.text.Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)}
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}
