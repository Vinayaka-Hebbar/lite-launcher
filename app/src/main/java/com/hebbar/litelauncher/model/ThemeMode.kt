package com.hebbar.litelauncher.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED_BLACK
}

data class AppGroup(
    val id: String,
    val name: String,
    val packageNames: MutableList<String> = mutableListOf()
)
