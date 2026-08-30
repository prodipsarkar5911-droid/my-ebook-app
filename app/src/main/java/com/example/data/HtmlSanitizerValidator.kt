package com.example.data

import java.util.regex.Pattern

data class ValidationResult(
    val isStrictHtml32: Boolean,
    val scorePercentage: Int,
    val paragraphCount: Int,
    val paragraphsWithIndentCount: Int,
    val headingsCount: Map<String, Int>,
    val colorSpansCount: Int,
    val fontSpansCount: Int,
    val forbiddenHtml5TagsFound: List<String>,
    val inlineStylesFoundCount: Int,
    val headingsWithIllegalIndent: List<String>,
    val rawExtractedHtml: String,
    val auditSummary: List<String>
)

object HtmlSanitizerValidator {

    private val HTML5_FORBIDDEN_TAGS = listOf(
        "main", "section", "article", "header", "footer", "nav", "aside",
        "figure", "figcaption", "audio", "video", "source", "canvas", "dialog"
    )

    fun cleanExtractedHtml(rawOutput: String): String {
        var clean = rawOutput.trim()

        // Extract code inside ```html ... ``` or ``` ... ```
        val codeBlockPattern = Pattern.compile("```(?:html)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE)
        val matcher = codeBlockPattern.matcher(clean)
        if (matcher.find()) {
            clean = matcher.group(1)?.trim() ?: clean
        }

        // Remove markdown tags if any leaked through
        clean = clean.replace("^`{3,}.*".toRegex(RegexOption.MULTILINE), "")
        return clean.trim()
    }

    fun validateHtml32(rawOutput: String): ValidationResult {
        val html = cleanExtractedHtml(rawOutput)
        val audit = mutableListOf<String>()

        // 1. Check HTML5 forbidden tags
        val forbiddenFound = mutableListOf<String>()
        for (tag in HTML5_FORBIDDEN_TAGS) {
            val tagRegex = "<$tag(\\s+[^>]*)?>|</$tag>".toRegex(RegexOption.IGNORE_CASE)
            if (tagRegex.containsMatchIn(html)) {
                forbiddenFound.add("<$tag>")
            }
        }

        // 2. Check inline styles
        val inlineStyleRegex = "style\\s*=\\s*[\"'][^\"']*[\"']".toRegex(RegexOption.IGNORE_CASE)
        val inlineStylesCount = inlineStyleRegex.findAll(html).count()

        // 3. Check Paragraphs & Indentation
        val allParagraphRegex = "<p(\\s+[^>]*)?>".toRegex(RegexOption.IGNORE_CASE)
        val allParagraphsCount = allParagraphRegex.findAll(html).count()

        val indentParagraphRegex = "<p\\s+class=[\"']indent[\"']>".toRegex(RegexOption.IGNORE_CASE)
        val indentParagraphsCount = indentParagraphRegex.findAll(html).count()

        // 4. Check Headings & ensure NO class="indent" on headings
        val headingsCount = mutableMapOf<String, Int>()
        val illegalIndentHeadings = mutableListOf<String>()

        for (level in 1..6) {
            val tag = "h$level"
            val hRegex = "<$tag(\\s+[^>]*)?>".toRegex(RegexOption.IGNORE_CASE)
            val count = hRegex.findAll(html).count()
            if (count > 0) {
                headingsCount[tag] = count
            }

            val illegalHIndent = "<$tag\\s+[^>]*class=[\"'][^\"']*indent[^\"']*[\"']".toRegex(RegexOption.IGNORE_CASE)
            if (illegalHIndent.containsMatchIn(html)) {
                illegalIndentHeadings.add("<$tag>")
            }
        }

        // 5. Dynamic Color & Font Spans
        val colorSpanRegex = "<span\\s+class=[\"']color\\d+[\"']>".toRegex(RegexOption.IGNORE_CASE)
        val colorSpansCount = colorSpanRegex.findAll(html).count()

        val fontSpanRegex = "<span\\s+class=[\"']font\\d+[\"']>".toRegex(RegexOption.IGNORE_CASE)
        val fontSpansCount = fontSpanRegex.findAll(html).count()

        // Calculate score
        var score = 100
        if (forbiddenFound.isNotEmpty()) {
            score -= 25
            audit.add("Violation: Found modern HTML5 tags (${forbiddenFound.joinToString(", ")}). HTML 3.2 disallows these.")
        } else {
            audit.add("Passed: Strict HTML 3.2 tags only (no HTML5 structural tags detected).")
        }

        if (inlineStylesCount > 0) {
            score -= 25
            audit.add("Violation: Found $inlineStylesCount inline style attribute(s). Rules require structure-based markup.")
        } else {
            audit.add("Passed: 100% pure structure without inline styles.")
        }

        if (allParagraphsCount > 0) {
            if (indentParagraphsCount == allParagraphsCount) {
                audit.add("Passed: All $allParagraphsCount <p> tags correctly have class=\"indent\".")
            } else {
                val missing = allParagraphsCount - indentParagraphsCount
                score -= minOf(30, missing * 10)
                audit.add("Warning: $missing of $allParagraphsCount <p> tag(s) missing class=\"indent\".")
            }
        } else {
            audit.add("Notice: No body paragraphs detected.")
        }

        if (illegalIndentHeadings.isNotEmpty()) {
            score -= 20
            audit.add("Violation: Heading tags (${illegalIndentHeadings.joinToString()}) have indent class.")
        } else if (headingsCount.isNotEmpty()) {
            audit.add("Passed: Headings (<h1>-<h6>) properly hierarchy-mapped without indent class.")
        }

        if (colorSpansCount > 0 || fontSpansCount > 0) {
            audit.add("Passed: Dynamic color spans ($colorSpansCount) & font spans ($fontSpansCount) tracked.")
        }

        val finalScore = maxOf(0, minOf(100, score))
        val isStrict = forbiddenFound.isEmpty() && inlineStylesCount == 0 && (allParagraphsCount == 0 || indentParagraphsCount == allParagraphsCount) && illegalIndentHeadings.isEmpty()

        return ValidationResult(
            isStrictHtml32 = isStrict,
            scorePercentage = finalScore,
            paragraphCount = allParagraphsCount,
            paragraphsWithIndentCount = indentParagraphsCount,
            headingsCount = headingsCount,
            colorSpansCount = colorSpansCount,
            fontSpansCount = fontSpansCount,
            forbiddenHtml5TagsFound = forbiddenFound,
            inlineStylesFoundCount = inlineStylesCount,
            headingsWithIllegalIndent = illegalIndentHeadings,
            rawExtractedHtml = html,
            auditSummary = audit
        )
    }

    /**
     * Client-side sanitization pass to enforce strict rules on any loose HTML input.
     */
    fun sanitizeToStrictHtml32(inputHtml: String): String {
        var clean = cleanExtractedHtml(inputHtml)

        // Strip modern HTML5 containers while preserving their inner content
        for (tag in HTML5_FORBIDDEN_TAGS) {
            clean = clean.replace("<$tag(\\s+[^>]*)?>".toRegex(RegexOption.IGNORE_CASE), "")
            clean = clean.replace("</$tag>".toRegex(RegexOption.IGNORE_CASE), "")
        }

        // Remove all inline style attributes
        clean = clean.replace("style\\s*=\\s*[\"'][^\"']*[\"']".toRegex(RegexOption.IGNORE_CASE), "")

        // Normalize <p> tags to <p class="indent">
        clean = clean.replace("<p(\\s*|\\s+[^>]*)>".toRegex(RegexOption.IGNORE_CASE), "<p class=\"indent\">")

        // Remove class="indent" from headings
        for (i in 1..6) {
            clean = clean.replace("<h$i\\s+[^>]*class=[\"'][^\"']*indent[^\"']*[\"']>".toRegex(RegexOption.IGNORE_CASE), "<h$i>")
        }

        return clean.trim()
    }
}
