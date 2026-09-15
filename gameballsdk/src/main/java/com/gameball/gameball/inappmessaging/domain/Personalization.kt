package com.gameball.gameball.inappmessaging.domain

/**
 * Token substitution for message copy.
 *
 * This is live, not dormant: sync sends templates, and campaigns on the alpha account carry
 * {player_name} and {points_balance} today. Anything that fails to fill a template puts
 * placeholders in front of a customer.
 */
internal object Personalization {

    /**
     * Single braces around a bare identifier. Not Liquid, no double braces, no filters, no
     * conditionals.
     *
     * Deliberately strict: a loose pattern lets a value map mangle ordinary copy, so
     * "{ spaced }", "{2}" and a lone "{" are not tokens.
     */
    private val TOKEN = Regex("""\{([A-Za-z_][A-Za-z0-9_]*)}""")

    /**
     * Cheap-scans for a brace first, so a message with no tokens costs one character
     * comparison and never reaches the variables endpoint.
     */
    fun hasToken(text: String?): Boolean =
        text != null && text.indexOf('{') >= 0 && TOKEN.containsMatchIn(text)

    fun tokenNames(vararg texts: String?): Set<String> =
        texts.asSequence()
            .filterNotNull()
            .filter { it.indexOf('{') >= 0 }
            .flatMap { TOKEN.findAll(it).map { match -> match.groupValues[1] } }
            .toSet()

    /**
     * One pass. Matches come from the original text and are applied in reverse, so a
     * replacement can neither invalidate a range still to come nor be rescanned — a
     * substituted value is data, not a template, and a value containing braces is never
     * expanded again.
     *
     * Unknown tokens are left exactly as written, so a caller can still tell resolved from
     * unresolved. [blankUnresolved] is what makes the result presentable.
     */
    fun substitute(text: String?, values: Map<String, String>): String? {
        if (text == null || text.indexOf('{') < 0) return text
        val matches = TOKEN.findAll(text).toList()
        if (matches.isEmpty()) return text
        val builder = StringBuilder(text)
        for (match in matches.asReversed()) {
            val replacement = values[match.groupValues[1]] ?: continue
            builder.replace(match.range.first, match.range.last + 1, replacement)
        }
        return builder.toString()
    }

    /**
     * The guarantee that a raw template never reaches a screen. Applied as a final pass on
     * every display path, including the ones where substitution never ran at all — a
     * timed-out fetch, for instance.
     *
     * Blanking rather than suppressing is deliberate (O22, resolved 24 Aug 2026): a value the
     * SDK could not get means a real backend problem, which should be found and fixed there
     * rather than hidden by withholding the campaign. Per-token defaults are the right
     * long-term answer and are deliberately deferred.
     */
    fun blankUnresolved(text: String?): String? =
        if (text == null || text.indexOf('{') < 0) text else TOKEN.replace(text, "")
}
