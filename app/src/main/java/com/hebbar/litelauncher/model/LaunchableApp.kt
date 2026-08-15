package com.hebbar.litelauncher.model

import android.content.ComponentName

/**
 * Clean data model representing an installed, launchable application.
 */
data class LaunchableApp(
    val packageName: String,
    val activityName: String,
    val label: String,
    val installTime: Long = 0L,
    var launchCount: Int = 0,
    var lastLaunchTime: Long = 0L,
    var customLabel: String? = null,
    var isHidden: Boolean = false,
    var category: String? = null
) {
    val componentName: ComponentName
        get() = ComponentName(packageName, activityName)

    val effectiveLabel: String
        get() = customLabel?.takeIf { it.isNotBlank() } ?: label
}
