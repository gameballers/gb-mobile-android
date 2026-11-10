package com.gameball.gameball

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every regex the SDK compiles must also compile on a device.
 *
 * Android's java.util.regex runs on ICU, which rejects a `}` that does not close a `{n,m}`
 * quantifier. The JVM these tests run on reads it as a literal, so such a pattern passes every
 * unit test and throws PatternSyntaxException on a phone. Personalization's token pattern did
 * that in 4.0.0, from an object initializer on the main thread, crashing the app after the
 * first sync. Compiling the patterns here proves nothing, so this reads them from the source
 * and applies ICU's rule. Each case in the second test was checked against ICU itself.
 */
class RegexPortabilityTest {

    @Test
    fun `every regex in the SDK escapes a closing brace that does not end a quantifier`() {
        val root = File("src/main")
        assertTrue("run from the module directory: ${root.absolutePath} is missing", root.isDirectory)
        val found = regexLiterals(root.walk().filter { it.extension == "kt" || it.extension == "java" })
        // A scan that missed these would pass without having looked.
        listOf("IamTime.kt", "Personalization.kt").forEach { name ->
            assertTrue("the scan missed $name; found $found", found.any { it.file == name })
        }
        assertEquals(
            "Android's regex engine (ICU) rejects these; escape the closing brace as \\}",
            emptyList<Found>(),
            found.filter { bareClosingBrace(it.pattern) }
        )
    }

    @Test
    fun `the brace rule matches what ICU accepts and rejects`() {
        listOf("a}", "}", "a{2}}", """\{([A-Za-z_][A-Za-z0-9_]*)}""").forEach {
            assertTrue("ICU rejects $it", bareClosingBrace(it))
        }
        listOf(
            """a\}""", "[}]", "[a-z}]", """\Q}\E""", "a{2}", "a{2,}", "a{2,3}", """\d{2}:\d{2}""",
            """\{([A-Za-z_][A-Za-z0-9_]*)\}"""
        ).forEach {
            assertFalse("ICU accepts $it", bareClosingBrace(it))
        }
    }

    @Test
    fun `the scan reads every way the SDK writes a regex, and nothing else`() {
        val source = File.createTempFile("Patterns", ".kt").apply {
            writeText(
                """
                val a = Regex(${Q3}a}$Q3)
                val copy = ${Q3}not a pattern}$Q3
                val b = Regex("b\\}")
                val c = Pattern.compile("c}")
                val d = ${Q3}d{2}$Q3.toRegex()
                val e = "e}".toRegex()
                val label = "not a pattern either}"
                """.trimIndent()
            )
            deleteOnExit()
        }
        assertEquals(
            listOf("a}", "b\\}", "c}", "d{2}", "e}"),
            regexLiterals(sequenceOf(source)).map { it.pattern }.sorted()
        )
    }

    private data class Found(val file: String, val pattern: String) {
        override fun toString() = "$file: $pattern"
    }

    /**
     * The string literals, raw or quoted, passed to `Regex(…)` or `Pattern.compile(…)` or with
     * `.toRegex()` called on them.
     */
    private fun regexLiterals(files: Sequence<File>): List<Found> = files.flatMap { file ->
        val text = file.readText()
        val raw = RAW_STRING.findAll(text)
            .filter { isRegexArgument(text, it.range) }
            .map { Found(file.name, it.groupValues[1]) }
        val quoted = QUOTED_FORMS.asSequence().flatMap { it.findAll(text) }
            .map { Found(file.name, unescape(it.groupValues[1])) }
        raw + quoted
    }.toList()

    private fun isRegexArgument(text: String, literal: IntRange): Boolean {
        val before = text.substring(maxOf(0, literal.first - 40), literal.first)
        val after = text.substring(literal.last + 1, minOf(text.length, literal.last + 21))
        return CALL_BEFORE.containsMatchIn(before) || TO_REGEX_AFTER.containsMatchIn(after)
    }

    /**
     * Whether [pattern] has a `}` ICU rejects: one that is not escaped, quoted (`\Q…\E`), inside
     * a character class, or the end of a `{n}`, `{n,}` or `{n,m}` quantifier.
     */
    private fun bareClosingBrace(pattern: String): Boolean {
        var i = 0
        var classDepth = 0
        while (i < pattern.length) {
            val c = pattern[i]
            when {
                pattern.startsWith("""\Q""", i) -> {
                    val end = pattern.indexOf("""\E""", i + 2)
                    i = if (end < 0) pattern.length else end + 2
                    continue
                }
                c == '\\' -> i++ // the escaped character is skipped below
                c == '[' -> classDepth++
                c == ']' && classDepth > 0 -> classDepth--
                classDepth > 0 -> Unit
                c == '{' -> QUANTIFIER.find(pattern, i)?.takeIf { it.range.first == i }
                    ?.let { i = it.range.last }
                c == '}' -> return true
            }
            i++
        }
        return false
    }

    /** A quoted literal's text as the compiler reads it, for the escapes a pattern uses. */
    private fun unescape(literal: String) = Regex("""\\(.)""").replace(literal) { it.groupValues[1] }

    /*
     * No repeated group runs once per character here: the JVM's engine recurses for each pass
     * through one, and a scan from every quote in a file overflows the stack.
     */
    private companion object {
        const val Q3 = "\"\"\""
        val QUANTIFIER = Regex("""\{\d+(,\d*)?\}""")

        /** Raw strings, paired left to right; one cannot contain three quotes. */
        val RAW_STRING = Regex("(?s)$Q3(.*?)$Q3")
        val CALL_BEFORE = Regex("""(?:\bRegex|Pattern\.compile)\(\s*$""")
        val TO_REGEX_AFTER = Regex("""^\s*\.toRegex\(""")

        val CALL = """(?:\bRegex|Pattern\.compile)\(\s*"""
        /** A quoted string, not the empty pair a raw string's quotes look like. */
        val QUOTED = """(?<!")"([^"\\]*(?:\\.[^"\\]*)*)"(?!")"""
        val QUOTED_FORMS = listOf(Regex("$CALL$QUOTED"), Regex("""$QUOTED\s*\.toRegex\("""))
    }
}
