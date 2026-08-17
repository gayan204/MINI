package com.example.mini.editor

import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import java.util.regex.Pattern

/**
 * Applies syntax highlighting to the editor based on file type.
 * - Kotlin keywords are only highlighted for .kt files.
 * - Markdown elements (headers, bold/italic, lists) are highlighted for .md files.
 * - Plain text files (.txt or unknown) receive no keyword highlighting.
 */
class SyntaxHighlighter(private var fileExtension: String = "") : TextWatcher {

    private val keywords = arrayOf(
        "abstract", "actual", "as", "break", "class", "companion", "continue", "crossinline", "data", "do",
        "dynamic", "else", "enum", "expect", "external", "false", "final", "finally", "for", "fun", "if",
        "import", "in", "infix", "init", "inline", "inner", "interface", "internal", "is", "lateinit", "noinline",
        "null", "object", "open", "operator", "out", "override", "package", "private", "protected", "public",
        "reified", "return", "sealed", "super", "suspend", "tailrec", "this", "throw", "true", "try", "typealias",
        "val", "var", "vararg", "when", "where", "while"
    )

    private val keywordPattern: Pattern
    private val markdownHeaderPattern: Pattern
    private val markdownBoldItalicPattern: Pattern
    private val markdownListPattern: Pattern
    private var isFormatting = false

    init {
        val keywordRegex = "\\b(" + keywords.joinToString("|") + ")\\b"
        keywordPattern = Pattern.compile(keywordRegex)
        markdownHeaderPattern = Pattern.compile("^(#{1,6})\\s.*$", Pattern.MULTILINE)
        markdownBoldItalicPattern = Pattern.compile("(\\*\\*|__)(.*?)\\1|(\\*|_)(.*?)\\3")
        markdownListPattern = Pattern.compile("^(\\s*)([-*+]|\\d+\\.)\\s.+$", Pattern.MULTILINE)
    }

    /** Update the file extension when a new file is opened */
    fun setFileExtension(ext: String) {
        fileExtension = ext.lowercase()
    }

    /** Directly re-apply highlighting to current editable (safe alternative to re-assigning editor.text) */
    fun rehighlight(editable: android.text.Editable?) {
        afterTextChanged(editable)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        if (isFormatting || s == null) return
        isFormatting = true

        try {
            // Remove previous spans
            val spans = s.getSpans(0, s.length, ForegroundColorSpan::class.java)
            for (span in spans) {
                s.removeSpan(span)
            }

            when (fileExtension) {
                "kt", "kts" -> applyKotlinHighlighting(s)
                "md", "markdown" -> applyMarkdownHighlighting(s)
                // .txt or unknown: no keyword highlighting
            }

        } finally {
            isFormatting = false
        }
    }

    private fun applyKotlinHighlighting(s: Editable) {
        // Apply Kotlin Keyword spans
        val matcher = keywordPattern.matcher(s)
        while (matcher.find()) {
            s.setSpan(
                ForegroundColorSpan(Color.parseColor("#CC7832")), // Orange for keywords
                matcher.start(),
                matcher.end(),
                0
            )
        }
    }

    private fun applyMarkdownHighlighting(s: Editable) {
        // Apply Markdown Header spans
        val headerMatcher = markdownHeaderPattern.matcher(s)
        while (headerMatcher.find()) {
            s.setSpan(
                ForegroundColorSpan(Color.parseColor("#2E86C1")), // Blue for headers
                headerMatcher.start(),
                headerMatcher.end(),
                0
            )
        }

        // Apply Markdown Bold/Italic spans
        val boldItalicMatcher = markdownBoldItalicPattern.matcher(s)
        while (boldItalicMatcher.find()) {
            s.setSpan(
                ForegroundColorSpan(Color.parseColor("#8E44AD")), // Purple for bold/italics
                boldItalicMatcher.start(),
                boldItalicMatcher.end(),
                0
            )
        }

        // Apply Markdown List spans (- item, * item, 1. item)
        val listMatcher = markdownListPattern.matcher(s)
        while (listMatcher.find()) {
            s.setSpan(
                ForegroundColorSpan(Color.parseColor("#27AE60")), // Green for list items
                listMatcher.start(),
                listMatcher.end(),
                0
            )
        }
    }
}
