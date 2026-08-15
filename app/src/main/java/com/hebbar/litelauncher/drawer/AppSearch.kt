package com.hebbar.litelauncher.drawer

import com.hebbar.litelauncher.model.LaunchableApp
import java.util.Locale

object AppSearch {

    /**
     * Perform instant search on a list of apps given a query string and custom aliases mapping.
     */
    fun filter(
        apps: List<LaunchableApp>,
        query: String,
        aliases: Map<String, String> = emptyMap()
    ): List<LaunchableApp> {
        val trimmed = query.trim().lowercase(Locale.ROOT)
        if (trimmed.isEmpty()) return apps

        val results = mutableListOf<Pair<LaunchableApp, Int>>()

        for (app in apps) {
            val rawLabel = app.effectiveLabel
            val labelLower = rawLabel.lowercase(Locale.ROOT)
            val pkgLower = app.packageName.lowercase(Locale.ROOT)
            val customAlias = aliases[app.packageName]?.lowercase(Locale.ROOT)

            val score = calculateMatchScore(rawLabel, labelLower, pkgLower, customAlias, trimmed)
            if (score > 0) {
                results.add(Pair(app, score))
            }
        }

        // Higher score comes first
        return results.sortedByDescending { it.second }.map { it.first }
    }

    private fun calculateMatchScore(
        rawLabel: String,
        labelLower: String,
        packageNameLower: String,
        aliasLower: String?,
        queryLower: String
    ): Int {
        // Exact alias match or exact label match (highest priority)
        if (aliasLower == queryLower || labelLower == queryLower) return 100

        // Starts with query
        if (labelLower.startsWith(queryLower)) return 80
        if (aliasLower != null && aliasLower.startsWith(queryLower)) return 75

        // Word initial / Acronym match (e.g., "yt" -> "YouTube", "wa" -> "WhatsApp")
        val initials = getInitials(rawLabel)
        if (initials.startsWith(queryLower) || initials == queryLower) return 70

        // Word boundary match (e.g., query matches start of any word in title)
        val words = labelLower.split(" ", "-", "_", ".")
        for (w in words) {
            if (w.startsWith(queryLower)) return 60
        }

        // Contains substring in label
        if (labelLower.contains(queryLower)) return 40

        // Substring in package name (e.g., "calc" -> "com.android.calculator2")
        if (packageNameLower.contains(queryLower)) return 20

        return 0
    }

    private fun getInitials(text: String): String {
        val sb = StringBuilder()
        var nextIsInitial = true
        for (ch in text) {
            if (ch.isLetterOrDigit()) {
                if (nextIsInitial) {
                    sb.append(ch.lowercaseChar())
                    nextIsInitial = false
                } else if (ch.isUpperCase()) {
                    sb.append(ch.lowercaseChar())
                }
            } else {
                nextIsInitial = true
            }
        }
        return sb.toString()
    }
}
