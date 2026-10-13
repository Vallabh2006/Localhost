package com.localhost.feature.files

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class SyntaxHighlighter(private val extension: String) : VisualTransformation {

    private val keywordColor = Color(0xFFC678DD)
    private val stringColor = Color(0xFF98C379)
    private val commentColor = Color(0xFF7F848E)
    private val numberColor = Color(0xFFD19A66)
    private val functionColor = Color(0xFF61AFEF)
    private val operatorColor = Color(0xFF56B6C2)

    private val keywords = setOf(
        "def", "class", "import", "from", "return", "if", "elif", "else", "while", "for", "in", "try", "except", "finally", "with", "as", "pass", "break", "continue", "lambda", "yield", "async", "await", "global", "nonlocal", "assert", "del", "True", "False", "None",
        "function", "const", "let", "var", "new", "this", "typeof", "instanceof", "throw", "catch", "switch", "case", "default", "export", "interface", "type", "enum", "implements", "extends", "public", "private", "protected", "static", "void", "null", "undefined",
        "echo", "namespace", "use", "require", "require_once", "include", "include_once", "fn", "match", "isset", "empty", "die", "exit"
    )

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val annotated = buildAnnotatedString {
            append(raw)

            val stringRegex = Regex("""(".*?"|'.*?'|`.*?`)""")
            for (match in stringRegex.findAll(raw)) {
                addStyle(SpanStyle(color = stringColor), match.range.first, match.range.last + 1)
            }

            val commentRegex = Regex("""(//[^\n]*|#[^\n]*)""")
            for (match in commentRegex.findAll(raw)) {
                addStyle(SpanStyle(color = commentColor, fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
            }

            val numberRegex = Regex("""\b(\d+(\.\d+)?|0x[0-9a-fA-F]+)\b""")
            for (match in numberRegex.findAll(raw)) {
                addStyle(SpanStyle(color = numberColor), match.range.first, match.range.last + 1)
            }

            val wordRegex = Regex("""\b([a-zA-Z_][a-zA-Z0-9_]*)\b""")
            for (match in wordRegex.findAll(raw)) {
                val word = match.value
                if (keywords.contains(word)) {
                    addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
                }
            }
        }

        return TransformedText(annotated, OffsetMapping.Identity)
    }

    companion object {
        fun extractTokens(text: String): Set<String> {
            val tokenRegex = Regex("""\b([a-zA-Z_][a-zA-Z0-9_]{1,30})\b""")
            return tokenRegex.findAll(text).map { it.value }.toSet()
        }
    }
}
