package com.example.mini.editor

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.mini.R
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

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

        // Direct Markdown -> HTML using commonmark (no lossy Spanned conversion)
        val parser = Parser.builder().build()
        val document = parser.parse(markdownText)
        val renderer = HtmlRenderer.builder().build()
        val htmlBody = renderer.render(document)

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
                  line-height: 1.7;
                }
                h1 { font-size: 2em;   color: #311B92; border-bottom: 2px solid #EDE7F6; padding-bottom: 4px; }
                h2 { font-size: 1.5em; color: #4527A0; border-bottom: 1px solid #EDE7F6; padding-bottom: 4px; }
                h3 { font-size: 1.2em; color: #512DA8; }
                h4, h5, h6 { color: #6A1B9A; }
                ul, ol { padding-left: 24px; }
                li { margin-bottom: 6px; }
                strong { font-weight: bold; }
                em { font-style: italic; }
                code {
                  background: #F3E5F5;
                  color: #6A1B9A;
                  padding: 2px 6px;
                  border-radius: 4px;
                  font-family: monospace;
                }
                pre {
                  background: #1e1e1e;
                  color: #d4d4d4;
                  padding: 14px;
                  border-radius: 8px;
                  overflow-x: auto;
                }
                pre code { background: none; color: inherit; padding: 0; }
                blockquote {
                  border-left: 4px solid #7E57C2;
                  margin: 0;
                  padding: 8px 16px;
                  background: #F3E5F5;
                  border-radius: 0 4px 4px 0;
                  color: #555;
                }
                hr { border: none; border-top: 1px solid #E0E0E0; margin: 16px 0; }
                a { color: #7E57C2; }
              </style>
            </head>
            <body>
            $htmlBody
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}
