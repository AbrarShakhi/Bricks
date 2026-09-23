package com.abrarshakhi.bricks.core.domain.search

import com.abrarshakhi.bricks.core.domain.model.App
import java.text.Normalizer
import java.util.Locale

class AppSearchEngine {

    fun search(apps: List<App>, query: String, now: Long): List<App> {
        val needle = normalize(query).trim()
        if (needle.isEmpty()) return emptyList()
        return apps
            .mapNotNull { app -> score(app, needle)?.let { app to it } }
            .sortedWith(
                compareByDescending<Pair<App, Int>> { it.second }
                    .thenByDescending { it.first.usage.score(now) }
                    .thenBy { normalize(it.first.label) },
            )
            .map { it.first }
    }

    internal fun score(app: App, needle: String): Int? {
        val label = normalize(app.label)
        val words = label.split(WORD_SEPARATOR).filter { it.isNotEmpty() }
        return when {
            label == needle -> EXACT
            label.startsWith(needle) -> PREFIX - (label.length - needle.length).coerceAtMost(PENALTY_CAP)
            words.any { it.startsWith(needle) } -> WORD_PREFIX
            initials(words).startsWith(needle) -> INITIALS
            label.contains(needle) -> CONTAINS
            app.systemLabel != app.label && normalize(app.systemLabel).contains(needle) -> ORIGINAL_LABEL
            app.key.packageName.lowercase(Locale.ROOT).contains(needle) -> PACKAGE
            else -> subsequenceScore(label, needle)
        }
    }

    private fun subsequenceScore(label: String, needle: String): Int? {
        if (needle.length < MIN_FUZZY_LENGTH) return null
        var from = 0
        var gaps = 0
        needle.forEach { ch ->
            if (ch == ' ') return@forEach
            val index = label.indexOf(ch, from)
            if (index < 0) return null
            gaps += index - from
            from = index + 1
        }
        return (FUZZY - gaps * FUZZY_GAP_COST).coerceAtLeast(FUZZY_FLOOR)
    }

    private fun initials(words: List<String>): String = words.joinToString("") { it.take(1) }

    companion object {
        private val WORD_SEPARATOR = Regex("[\\s._\\-:/&+()]+")
        private val DIACRITICS = Regex("\\p{Mn}+")
        private const val EXACT = 1000
        private const val PREFIX = 900
        private const val PENALTY_CAP = 50
        private const val WORD_PREFIX = 800
        private const val INITIALS = 700
        private const val CONTAINS = 600
        private const val ORIGINAL_LABEL = 500
        private const val PACKAGE = 400
        private const val FUZZY = 300
        private const val FUZZY_GAP_COST = 10
        private const val FUZZY_FLOOR = 100
        private const val MIN_FUZZY_LENGTH = 3

        fun normalize(value: String): String =
            Normalizer.normalize(value, Normalizer.Form.NFD)
                .replace(DIACRITICS, "")
                .lowercase(Locale.ROOT)
    }
}
