package dev.harrix.notes

/**
 * Shared New note body builder.
 *
 * @hsk-sync:new-note — keep aligned with HSK OnNewMarkdown and VS Code new-note.js.
 */
object NewNoteContent {
    private val personalFrontmatterKeys = setOf("author", "author-email")

    data class BeginningTemplate(
        val id: String,
        val label: String,
        val content: String,
    )

    data class PersonalData(
        val enabled: Boolean = false,
        val author: String = "noname",
        val authorEmail: String = "",
    )

    val defaultBeginningTemplates: List<BeginningTemplate> =
        listOf(
            BeginningTemplate(
                id = "beginning-of-md",
                label = "beginning-of-md.md",
                content = "---\nlang: ru\n---\n",
            ),
            BeginningTemplate(
                id = "beginning-of-article",
                label = "beginning-of-article.md",
                content =
                "---\ndate: [DATE]\ncategories: [it]\ntags: []\n" +
                    "license: CC BY 4.0\nlicense-url: <YOUR_LICENSE_URL>\n" +
                    "permalink-source: <YOUR_PERMALINK_SOURCE>/[YEAR]/blob/main/[NAME]/[NAME].md\n" +
                    "permalink: <YOUR_SITE>/articles/[YEAR]/[NAME]/\nlang: ru\n---\n",
            ),
            BeginningTemplate(
                id = "beginning-of-md-en",
                label = "beginning-of-md-en.md",
                content = "---\nlang: en\n---\n",
            ),
        )

    fun applyPersonalDataToBeginning(
        beginning: String,
        personal: PersonalData,
    ): String {
        val text = beginning
        if (text.isBlank()) {
            return text
        }

        val enabled = personal.enabled
        val author = personal.author
        val authorEmail = personal.authorEmail

        val stripped = text.removePrefix("\uFEFF")
        if (!stripped.startsWith("---")) {
            if (!enabled) {
                return text
            }
            val lines = mutableListOf("---", "author: $author")
            if (authorEmail.isNotEmpty()) {
                lines += "author-email: $authorEmail"
            }
            lines += "---"
            return lines.joinToString("\n") + "\n" + stripped.trimStart('\n')
        }

        val lines = stripped.split('\n')
        var endIdx = -1
        for (i in 1 until lines.size) {
            if (lines[i].trim() == "---") {
                endIdx = i
                break
            }
        }
        if (endIdx < 0) {
            return text
        }

        val bodyLines = mutableListOf<String>()
        for (line in lines.subList(1, endIdx)) {
            val key =
                if (':' in line) {
                    line.substringBefore(':').trim().lowercase()
                } else {
                    ""
                }
            if (key in personalFrontmatterKeys) {
                continue
            }
            bodyLines += line
        }

        if (enabled) {
            var insertAt = 0
            for (i in bodyLines.indices) {
                if (bodyLines[i].substringBefore(':').trim().lowercase() == "lang") {
                    insertAt = i
                    break
                }
                insertAt = i + 1
            }
            val personalLines = mutableListOf("author: $author")
            if (authorEmail.isNotEmpty()) {
                personalLines += "author-email: $authorEmail"
            }
            bodyLines.addAll(insertAt, personalLines)
        }

        val rebuilt = mutableListOf("---")
        rebuilt += bodyLines
        rebuilt += "---"
        rebuilt += lines.subList(endIdx + 1, lines.size)
        var result = rebuilt.joinToString("\n")
        if (text.endsWith("\n")) {
            result += "\n"
        }
        return result
    }

    /**
     * Build note file content: beginning + personal data + `# heading`.
     * When [isCanvas], injects `type: canvas` and an `img/canvas.png` image link.
     *
     * @hsk-sync:new-note
     */
    fun build(
        beginning: String,
        heading: String,
        personal: PersonalData,
        isCanvas: Boolean = false,
    ): String {
        var withPersonal = applyPersonalDataToBeginning(beginning, personal).trimEnd()
        if (isCanvas) {
            withPersonal = injectFrontmatterKey(withPersonal, "type", "canvas")
        }
        val title = heading.trim()
        return if (isCanvas) {
            "$withPersonal\n# $title\n\n![canvas](img/canvas.png)\n\n"
        } else {
            "$withPersonal\n# $title\n\n\n"
        }
    }

    /**
     * Inserts or replaces a YAML frontmatter key. If there is no frontmatter block,
     * creates a minimal one.
     */
    fun injectFrontmatterKey(
        text: String,
        key: String,
        value: String,
    ): String {
        val keyLower = key.trim().lowercase()
        val stripped = text.removePrefix("\uFEFF")
        if (!stripped.startsWith("---")) {
            return "---\n$keyLower: $value\n---\n${stripped.trimStart('\n')}"
        }
        val lines = stripped.split('\n')
        var endIdx = -1
        for (i in 1 until lines.size) {
            if (lines[i].trim() == "---") {
                endIdx = i
                break
            }
        }
        if (endIdx < 0) {
            return text
        }
        val bodyLines = mutableListOf<String>()
        var replaced = false
        for (line in lines.subList(1, endIdx)) {
            val lineKey =
                if (':' in line) {
                    line.substringBefore(':').trim().lowercase()
                } else {
                    ""
                }
            if (lineKey == keyLower) {
                if (!replaced) {
                    bodyLines += "$keyLower: $value"
                    replaced = true
                }
            } else {
                bodyLines += line
            }
        }
        if (!replaced) {
            bodyLines += "$keyLower: $value"
        }
        val rebuilt = mutableListOf("---")
        rebuilt += bodyLines
        rebuilt += "---"
        rebuilt += lines.subList(endIdx + 1, lines.size)
        var result = rebuilt.joinToString("\n")
        if (text.endsWith("\n") && !result.endsWith("\n")) {
            result += "\n"
        }
        return result
    }
}
