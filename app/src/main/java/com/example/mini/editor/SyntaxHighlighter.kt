package com.example.mini.editor

import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import java.util.regex.Pattern

class SyntaxHighlighter : TextWatcher {

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
    private var isFormatting = false

    init {
        val keywordRegex = "\\b(" + keywords.joinToString("|") + ")\\b"
        keywordPattern = Pattern.compile(keywordRegex)
        markdownHeaderPattern = Pattern.compile("^(#{1,6})\\s.*\$", Pattern.MULTILINE)
        markdownBoldItalicPattern = Pattern.compile("(\\*\\*|__)(.*?)\\1|(\\*|_)(.*?)\\3")
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

            // Apply Kotlin Keyword spans
            val matcher = keywordPattern.matcher(s)
            while (matcher.find()) {
                s.setSpan(
                    ForegroundColorSpan(Color.parseColor("#CC7832")), // Orange-ish for keywords
                    matcher.start(),
                    matcher.end(),
                    0
                )
            }

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
        } finally {
            isFormatting = false
        }
    }
}
