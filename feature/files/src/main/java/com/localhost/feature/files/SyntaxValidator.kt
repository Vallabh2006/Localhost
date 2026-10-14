package com.localhost.feature.files

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Stack

data class SyntaxIssue(
    val line: Int,
    val column: Int,
    val message: String,
    val isError: Boolean = true
)

object SyntaxValidator {

    fun validate(content: String, extension: String): List<SyntaxIssue> {
        if (content.isBlank()) return emptyList()

        return when (extension.lowercase()) {
            "json" -> validateJson(content)
            "py" -> validatePython(content)
            "php" -> validatePhp(content)
            "js", "ts", "jsx", "tsx" -> validateJs(content)
            "html", "htm", "xml", "svg" -> validateXml(content)
            "env" -> validateEnv(content)
            else -> validateGenericBrackets(content)
        }
    }

    private fun validateJson(content: String): List<SyntaxIssue> {
        val trimmed = content.trim()
        val issues = mutableListOf<SyntaxIssue>()

        // Check for trailing commas which is common mistake
        val trailingCommaRegex = Regex(",\\s*[\\]}]")
        val lines = content.lines()
        for ((idx, line) in lines.withIndex()) {
            if (trailingCommaRegex.containsMatchIn(line)) {
                issues.add(
                    SyntaxIssue(
                        line = idx + 1,
                        column = line.indexOf(',') + 1,
                        message = "Trailing comma is not allowed in JSON"
                    )
                )
            }
        }

        try {
            if (trimmed.startsWith("{")) {
                JSONObject(content)
            } else if (trimmed.startsWith("[")) {
                JSONArray(content)
            } else {
                return listOf(
                    SyntaxIssue(
                        line = 1,
                        column = 1,
                        message = "JSON must start with '{' or '['"
                    )
                )
            }
        } catch (e: JSONException) {
            val msg = e.message ?: "Invalid JSON syntax"
            var lineNum = 1
            var colNum = 1

            val lineMatch = Regex("(?:line|character|at|after) (\\d+)").find(msg)
            if (lineMatch != null) {
                val num = lineMatch.groupValues[1].toIntOrNull() ?: 1
                if (num <= lines.size) {
                    lineNum = num
                } else {
                    lineNum = lines.size
                }
            }

            issues.add(
                SyntaxIssue(
                    line = lineNum,
                    column = colNum,
                    message = msg.substringBefore("\n")
                )
            )
        }

        return issues.take(5)
    }

    private fun validatePython(content: String): List<SyntaxIssue> {
        val issues = mutableListOf<SyntaxIssue>()
        val lines = content.lines()
        val stack = Stack<Triple<Char, Int, Int>>() // char, line, col

        var inTripleDouble = false
        var inTripleSingle = false

        val compoundKeywords = listOf("def", "class", "if", "elif", "else", "for", "while", "try", "except", "finally", "with", "async def", "async for", "async with")

        for ((idx, rawLine) in lines.withIndex()) {
            val lineNum = idx + 1
            val lineStr = rawLine
            val trimmed = lineStr.trim()

            // Skip comment-only or empty lines
            if (trimmed.startsWith("#") || trimmed.isEmpty()) continue

            // Check missing colon on compound statements
            if (!inTripleDouble && !inTripleSingle) {
                for (kw in compoundKeywords) {
                    if (trimmed.startsWith("$kw ") || trimmed == kw || (kw == "else" && trimmed == "else:") || (kw == "try" && trimmed == "try:") || (kw == "finally" && trimmed == "finally:")) {
                        val withoutComment = trimmed.substringBefore("#").trimEnd()
                        if (!withoutComment.endsWith(":") && !withoutComment.endsWith("\\")) {
                            // Only if it doesn't span multiple lines with open parenthesis
                            if (stack.isEmpty()) {
                                issues.add(
                                    SyntaxIssue(
                                        line = lineNum,
                                        column = lineStr.length,
                                        message = "Expected ':' at end of '$kw' statement"
                                    )
                                )
                            }
                        }
                        break
                    }
                }
            }

            var inDoubleQuote = false
            var inSingleQuote = false
            var i = 0

            while (i < lineStr.length) {
                val ch = lineStr[i]

                if (i + 2 < lineStr.length && lineStr.substring(i, i + 3) == "\"\"\"") {
                    inTripleDouble = !inTripleDouble
                    i += 3
                    continue
                }
                if (i + 2 < lineStr.length && lineStr.substring(i, i + 3) == "'''") {
                    inTripleSingle = !inTripleSingle
                    i += 3
                    continue
                }

                if (!inTripleDouble && !inTripleSingle) {
                    if (ch == '"' && (i == 0 || lineStr[i - 1] != '\\')) {
                        inDoubleQuote = !inDoubleQuote
                    } else if (ch == '\'' && (i == 0 || lineStr[i - 1] != '\\')) {
                        inSingleQuote = !inSingleQuote
                    } else if (!inDoubleQuote && !inSingleQuote) {
                        if (ch == '#') break
                        if (ch == '(' || ch == '[' || ch == '{') {
                            stack.push(Triple(ch, lineNum, i + 1))
                        } else if (ch == ')' || ch == ']' || ch == '}') {
                            if (stack.isEmpty()) {
                                issues.add(
                                    SyntaxIssue(
                                        line = lineNum,
                                        column = i + 1,
                                        message = "Unmatched closing '$ch'"
                                    )
                                )
                            } else {
                                val top = stack.pop()
                                val expected = when (top.first) {
                                    '(' -> ')'
                                    '[' -> ']'
                                    '{' -> '}'
                                    else -> ' '
                                }
                                if (ch != expected) {
                                    issues.add(
                                        SyntaxIssue(
                                            line = lineNum,
                                            column = i + 1,
                                            message = "Mismatched bracket: expected '$expected' for '${top.first}' at line ${top.second}, found '$ch'"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                i++
            }

            if (inDoubleQuote || inSingleQuote) {
                if (!lineStr.endsWith("\\")) {
                    issues.add(
                        SyntaxIssue(
                            line = lineNum,
                            column = lineStr.length,
                            message = "Unclosed string literal"
                        )
                    )
                }
            }
        }

        while (stack.isNotEmpty()) {
            val unclosed = stack.pop()
            issues.add(
                SyntaxIssue(
                    line = unclosed.second,
                    column = unclosed.third,
                    message = "Unclosed '${unclosed.first}'"
                )
            )
        }

        return issues.take(5)
    }

    private fun validatePhp(content: String): List<SyntaxIssue> {
        val issues = mutableListOf<SyntaxIssue>()
        val lines = content.lines()
        val stack = Stack<Triple<Char, Int, Int>>()

        val hasOpeningTag = content.contains("<?php") || content.contains("<?=")
        if (!hasOpeningTag && (content.contains("$") && content.contains(";"))) {
            issues.add(
                SyntaxIssue(
                    line = 1,
                    column = 1,
                    message = "Missing PHP opening tag '<?php'",
                    isError = false
                )
            )
        }

        for ((idx, lineStr) in lines.withIndex()) {
            val lineNum = idx + 1
            val trimmed = lineStr.trim()

            if (trimmed.startsWith("//") || trimmed.startsWith("#") || trimmed.startsWith("/*") || trimmed.startsWith("*")) continue

            for ((col, ch) in lineStr.withIndex()) {
                if (ch == '{' || ch == '(' || ch == '[') {
                    stack.push(Triple(ch, lineNum, col + 1))
                } else if (ch == '}' || ch == ')' || ch == ']') {
                    if (stack.isEmpty()) {
                        issues.add(SyntaxIssue(line = lineNum, column = col + 1, message = "Unmatched closing '$ch'"))
                    } else {
                        val top = stack.pop()
                        val expected = when (top.first) {
                            '{' -> '}'
                            '(' -> ')'
                            '[' -> ']'
                            else -> ' '
                        }
                        if (ch != expected) {
                            issues.add(SyntaxIssue(line = lineNum, column = col + 1, message = "Expected '$expected', found '$ch'"))
                        }
                    }
                }
            }
        }

        while (stack.isNotEmpty()) {
            val top = stack.pop()
            issues.add(SyntaxIssue(line = top.second, column = top.third, message = "Unclosed '${top.first}'"))
        }

        return issues.take(5)
    }

    private fun validateJs(content: String): List<SyntaxIssue> {
        return validateGenericBrackets(content)
    }

    private fun validateXml(content: String): List<SyntaxIssue> {
        val issues = mutableListOf<SyntaxIssue>()
        val tagRegex = Regex("<(/)?([a-zA-Z0-9_\\-:]+)([^>]*)>")
        val stack = Stack<Pair<String, Int>>()
        val lines = content.lines()

        for ((idx, line) in lines.withIndex()) {
            val lineNum = idx + 1
            for (match in tagRegex.findAll(line)) {
                val isClosing = match.groupValues[1] == "/"
                val tagName = match.groupValues[2].lowercase()
                val rawTag = match.value

                if (rawTag.endsWith("/>") || tagName in listOf("br", "hr", "img", "input", "meta", "link")) {
                    continue
                }

                if (!isClosing) {
                    stack.push(Pair(tagName, lineNum))
                } else {
                    if (stack.isEmpty()) {
                        issues.add(SyntaxIssue(line = lineNum, column = 1, message = "Unexpected closing tag '</$tagName>' without opening tag"))
                    } else {
                        val top = stack.pop()
                        if (top.first != tagName) {
                            issues.add(SyntaxIssue(line = lineNum, column = 1, message = "Mismatched tag: expected '</${top.first}>' opened at line ${top.second}, found '</$tagName>'"))
                        }
                    }
                }
            }
        }

        while (stack.isNotEmpty()) {
            val unclosed = stack.pop()
            issues.add(SyntaxIssue(line = unclosed.second, column = 1, message = "Unclosed tag '<${unclosed.first}>'"))
        }

        return issues.take(5)
    }

    private fun validateEnv(content: String): List<SyntaxIssue> {
        val issues = mutableListOf<SyntaxIssue>()
        val lines = content.lines()

        for ((idx, line) in lines.withIndex()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            if (!trimmed.contains("=")) {
                issues.add(
                    SyntaxIssue(
                        line = idx + 1,
                        column = 1,
                        message = "Invalid .env format: missing '=' in '$trimmed'"
                    )
                )
            } else {
                val key = trimmed.substringBefore("=").trim()
                if (key.contains(" ")) {
                    issues.add(
                        SyntaxIssue(
                            line = idx + 1,
                            column = 1,
                            message = "Key cannot contain spaces: '$key'"
                        )
                    )
                }
            }
        }

        return issues
    }

    private fun validateGenericBrackets(content: String): List<SyntaxIssue> {
        val issues = mutableListOf<SyntaxIssue>()
        val stack = Stack<Triple<Char, Int, Int>>()
        val lines = content.lines()

        for ((idx, line) in lines.withIndex()) {
            val lineNum = idx + 1
            for ((col, ch) in line.withIndex()) {
                if (ch == '{' || ch == '(' || ch == '[') {
                    stack.push(Triple(ch, lineNum, col + 1))
                } else if (ch == '}' || ch == ')' || ch == ']') {
                    if (stack.isEmpty()) {
                        issues.add(SyntaxIssue(line = lineNum, column = col + 1, message = "Unmatched closing '$ch'"))
                    } else {
                        val top = stack.pop()
                        val expected = when (top.first) {
                            '{' -> '}'
                            '(' -> ')'
                            '[' -> ']'
                            else -> ' '
                        }
                        if (ch != expected) {
                            issues.add(SyntaxIssue(line = lineNum, column = col + 1, message = "Mismatched: expected '$expected', found '$ch'"))
                        }
                    }
                }
            }
        }

        while (stack.isNotEmpty()) {
            val top = stack.pop()
            issues.add(SyntaxIssue(line = top.second, column = top.third, message = "Unclosed '${top.first}'"))
        }

        return issues.take(5)
    }
}
