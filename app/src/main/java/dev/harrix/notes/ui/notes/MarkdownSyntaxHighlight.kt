package dev.harrix.notes.ui.notes

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration

/**
 * Lightweight Markdown token colors aligned with VS Code
 * `editor.tokenColorCustomizations` (Default Light Modern).
 */
@Immutable
data class MarkdownHighlightColors(
    val heading: SpanStyle,
    val quote: SpanStyle,
    val listMarker: SpanStyle,
    val bold: SpanStyle,
    val italic: SpanStyle,
    val inlineCode: SpanStyle,
    val codeBlock: SpanStyle,
    val linkText: SpanStyle,
    val linkUrl: SpanStyle,
    val separator: SpanStyle,
    val strikethrough: SpanStyle,
) {
    companion object {
        /** VS Code light token colors for Markdown scopes. */
        val Light =
            fromTokens(
                heading = Color(0xFF3861B2),
                quote = Color(0xFF1CB978),
                listMarker = Color(0xFFD07826),
                emphasis = Color(0xFF1F2939),
                inlineCode = Color(0xFFF13D3D),
                codeBlock = Color(0xFF1CB978),
                linkText = Color(0xFF4C43C2),
                linkUrl = Color(0xFF1CB978),
                separator = Color(0xFF3861B2),
                strikethrough = Color(0xFFAD1C48),
            )

        /** Softened counterparts for dark surfaces. */
        val Dark =
            fromTokens(
                heading = Color(0xFF7BA3E8),
                quote = Color(0xFF3DDB9A),
                listMarker = Color(0xFFE09A50),
                emphasis = Color(0xFFE0E4DB),
                inlineCode = Color(0xFFFF6B6B),
                codeBlock = Color(0xFF3DDB9A),
                linkText = Color(0xFF9B93E8),
                linkUrl = Color(0xFF3DDB9A),
                separator = Color(0xFF7BA3E8),
                strikethrough = Color(0xFFFF8A9A),
            )

        private fun fromTokens(
            heading: Color,
            quote: Color,
            listMarker: Color,
            emphasis: Color,
            inlineCode: Color,
            codeBlock: Color,
            linkText: Color,
            linkUrl: Color,
            separator: Color,
            strikethrough: Color,
        ) = MarkdownHighlightColors(
            heading = SpanStyle(color = heading, fontWeight = FontWeight.Bold),
            quote = SpanStyle(color = quote),
            listMarker = SpanStyle(color = listMarker),
            bold = SpanStyle(color = emphasis, fontWeight = FontWeight.Bold),
            italic = SpanStyle(color = emphasis, fontStyle = FontStyle.Italic),
            inlineCode = SpanStyle(color = inlineCode, fontStyle = FontStyle.Italic),
            codeBlock = SpanStyle(color = codeBlock, fontStyle = FontStyle.Italic),
            linkText = SpanStyle(color = linkText, fontStyle = FontStyle.Italic),
            linkUrl = SpanStyle(color = linkUrl, textDecoration = TextDecoration.Underline),
            separator = SpanStyle(color = separator, fontWeight = FontWeight.Bold),
            strikethrough =
            SpanStyle(color = strikethrough, textDecoration = TextDecoration.LineThrough),
        )
    }
}

@Composable
fun rememberMarkdownHighlightColors(): MarkdownHighlightColors {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return remember(darkTheme) {
        if (darkTheme) MarkdownHighlightColors.Dark else MarkdownHighlightColors.Light
    }
}

/**
 * Identity visual transformation: same characters, Markdown span styles only.
 * Single linear pass, no regex — suitable for edit-as-you-type.
 */
class MarkdownSyntaxVisualTransformation(
    private val colors: MarkdownHighlightColors,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(
            text = highlightMarkdown(text.text, colors),
            offsetMapping = OffsetMapping.Identity,
        )
}

/** Highlight full Markdown source for edit mode / one-shot use. */
fun highlightMarkdown(
    text: String,
    colors: MarkdownHighlightColors,
): AnnotatedString {
    if (text.isEmpty()) return AnnotatedString("")
    val builder = AnnotatedString.Builder(text.length)
    builder.append(text)
    var index = 0
    var inFence = false
    val length = text.length
    while (index < length) {
        val lineEnd = text.indexOf('\n', index).let { if (it < 0) length else it }
        inFence = styleLine(builder, text, index, lineEnd, inFence, colors)
        index = if (lineEnd < length) lineEnd + 1 else length
    }
    return builder.toAnnotatedString()
}

/**
 * Highlight each line for LazyColumn view mode.
 * Empty lines become a non-breaking space so row height is preserved.
 */
fun highlightMarkdownLines(
    lines: List<String>,
    colors: MarkdownHighlightColors,
): List<AnnotatedString> {
    if (lines.isEmpty()) return emptyList()
    val result = ArrayList<AnnotatedString>(lines.size)
    var inFence = false
    for (line in lines) {
        if (line.isEmpty()) {
            result.add(NBSP)
            continue
        }
        val builder = AnnotatedString.Builder(line.length)
        builder.append(line)
        inFence = styleLine(builder, line, 0, line.length, inFence, colors)
        result.add(builder.toAnnotatedString())
    }
    return result
}

private val NBSP = AnnotatedString("\u00A0")

/**
 * Applies styles for `[lineStart, lineEnd)`.
 * @return whether a fenced code block is open after this line
 */
private fun styleLine(
    builder: AnnotatedString.Builder,
    text: CharSequence,
    lineStart: Int,
    lineEnd: Int,
    inFence: Boolean,
    colors: MarkdownHighlightColors,
): Boolean {
    if (lineStart >= lineEnd) return inFence

    val contentStart = skipAsciiWhitespace(text, lineStart, lineEnd)

    if (isFenceLine(text, contentStart, lineEnd)) {
        builder.addStyle(colors.codeBlock, lineStart, lineEnd)
        return !inFence
    }

    if (inFence) {
        builder.addStyle(colors.codeBlock, lineStart, lineEnd)
        return true
    }

    if (isSeparatorLine(text, contentStart, lineEnd)) {
        builder.addStyle(colors.separator, lineStart, lineEnd)
        return false
    }

    if (isHeading(text, contentStart, lineEnd)) {
        builder.addStyle(colors.heading, lineStart, lineEnd)
        return false
    }

    if (contentStart < lineEnd && text[contentStart] == '>') {
        builder.addStyle(colors.quote, lineStart, lineEnd)
        return false
    }

    val listMarkerEnd = listMarkerEnd(text, contentStart, lineEnd)
    if (listMarkerEnd > contentStart) {
        builder.addStyle(colors.listMarker, contentStart, listMarkerEnd)
        styleInline(builder, text, listMarkerEnd, lineEnd, colors)
        return false
    }

    styleInline(builder, text, lineStart, lineEnd, colors)
    return false
}

private fun styleInline(
    builder: AnnotatedString.Builder,
    text: CharSequence,
    start: Int,
    end: Int,
    colors: MarkdownHighlightColors,
) {
    var i = start
    while (i < end) {
        i =
            when (text[i]) {
                '`' -> styleInlineCode(builder, text, i, end, colors)
                '[' -> styleLinkOrSkip(builder, text, i, end, colors)
                '!' -> styleImageOrSkip(builder, text, i, end, colors)
                '*', '_' -> styleEmphasis(builder, text, i, end, text[i], colors)
                '~' -> styleStrikeOrSkip(builder, text, i, end, colors)
                else -> i + 1
            }
    }
}

private fun styleInlineCode(
    builder: AnnotatedString.Builder,
    text: CharSequence,
    i: Int,
    end: Int,
    colors: MarkdownHighlightColors,
): Int {
    val close = indexOfChar(text, '`', i + 1, end)
    if (close < 0) return i + 1
    builder.addStyle(colors.inlineCode, i, close + 1)
    return close + 1
}

private fun styleLinkOrSkip(
    builder: AnnotatedString.Builder,
    text: CharSequence,
    i: Int,
    end: Int,
    colors: MarkdownHighlightColors,
): Int {
    val next = styleLink(builder, text, i, end, colors)
    return if (next >= 0) next else i + 1
}

private fun styleImageOrSkip(
    builder: AnnotatedString.Builder,
    text: CharSequence,
    i: Int,
    end: Int,
    colors: MarkdownHighlightColors,
): Int {
    if (i + 1 >= end || text[i + 1] != '[') return i + 1
    val next = styleLink(builder, text, i + 1, end, colors)
    return if (next >= 0) next else i + 1
}

private fun styleStrikeOrSkip(
    builder: AnnotatedString.Builder,
    text: CharSequence,
    i: Int,
    end: Int,
    colors: MarkdownHighlightColors,
): Int {
    if (i + 1 >= end || text[i + 1] != '~') return i + 1
    val close = indexOfPair(text, '~', i + 2, end)
    if (close < 0) return i + 1
    builder.addStyle(colors.strikethrough, i, close + 2)
    return close + 2
}

/** Returns index after the link, or -1 if not a valid `[text](url)`. */
private fun styleLink(
    builder: AnnotatedString.Builder,
    text: CharSequence,
    openBracket: Int,
    end: Int,
    colors: MarkdownHighlightColors,
): Int {
    val closeBracket = indexOfChar(text, ']', openBracket + 1, end)
    if (closeBracket < 0 || closeBracket + 1 >= end || text[closeBracket + 1] != '(') {
        return -1
    }
    val closeParen = indexOfChar(text, ')', closeBracket + 2, end)
    if (closeParen < 0) return -1

    if (openBracket + 1 < closeBracket) {
        builder.addStyle(colors.linkText, openBracket + 1, closeBracket)
    }
    if (closeBracket + 2 < closeParen) {
        builder.addStyle(colors.linkUrl, closeBracket + 2, closeParen)
    }
    return closeParen + 1
}

private fun styleEmphasis(
    builder: AnnotatedString.Builder,
    text: CharSequence,
    i: Int,
    end: Int,
    marker: Char,
    colors: MarkdownHighlightColors,
): Int {
    val doubled = i + 1 < end && text[i + 1] == marker
    if (doubled) {
        val close = indexOfPair(text, marker, i + 2, end)
        if (close >= 0 && close > i + 2) {
            builder.addStyle(colors.bold, i, close + 2)
            return close + 2
        }
    } else {
        val close = indexOfChar(text, marker, i + 1, end)
        if (close > i + 1 && (close + 1 >= end || text[close + 1] != marker)) {
            builder.addStyle(colors.italic, i, close + 1)
            return close + 1
        }
    }
    return i + 1
}

private fun skipAsciiWhitespace(
    text: CharSequence,
    start: Int,
    end: Int,
): Int {
    var i = start
    while (i < end) {
        val c = text[i]
        if (c != ' ' && c != '\t') break
        i++
    }
    return i
}

private fun isFenceLine(
    text: CharSequence,
    start: Int,
    end: Int,
): Boolean {
    if (start + 2 >= end) return false
    return text[start] == '`' && text[start + 1] == '`' && text[start + 2] == '`'
}

private fun isSeparatorLine(
    text: CharSequence,
    start: Int,
    end: Int,
): Boolean {
    if (start >= end) return false
    val c = text[start]
    if (c != '-' && c != '*' && c != '_') return false
    var count = 0
    var i = start
    while (i < end) {
        val ch = text[i]
        when {
            ch == c -> count++
            ch == ' ' || ch == '\t' -> Unit
            else -> return false
        }
        i++
    }
    return count >= 3
}

private fun isHeading(
    text: CharSequence,
    start: Int,
    end: Int,
): Boolean {
    if (start >= end || text[start] != '#') return false
    var level = 0
    var i = start
    while (i < end && text[i] == '#' && level < 6) {
        level++
        i++
    }
    if (level == 0) return false
    return i >= end || text[i] == ' ' || text[i] == '\t'
}

/** End index of list marker including trailing space, or [start] if none. */
private fun listMarkerEnd(
    text: CharSequence,
    start: Int,
    end: Int,
): Int {
    if (start >= end) return start
    val c = text[start]
    if (c == '-' || c == '*' || c == '+') {
        val after = start + 1
        if (after < end && (text[after] == ' ' || text[after] == '\t')) {
            return after + 1
        }
        return start
    }
    if (c in '0'..'9') {
        var i = start + 1
        while (i < end && text[i] in '0'..'9') i++
        if (i < end && text[i] == '.') {
            val after = i + 1
            if (after < end && (text[after] == ' ' || text[after] == '\t')) {
                return after + 1
            }
        }
    }
    return start
}

private fun indexOfChar(
    text: CharSequence,
    ch: Char,
    from: Int,
    end: Int,
): Int {
    var i = from
    while (i < end) {
        if (text[i] == ch) return i
        i++
    }
    return -1
}

/** Index of first char of a `ch ch` pair at or after [from]. */
private fun indexOfPair(
    text: CharSequence,
    ch: Char,
    from: Int,
    end: Int,
): Int {
    var i = from
    while (i + 1 < end) {
        if (text[i] == ch && text[i + 1] == ch) return i
        i++
    }
    return -1
}
