package me.lukiiy.buildOff

import java.io.File

class ThemeFile() {
    private val themes = mutableListOf<String>()

    init {
        loadThemes()
    }

    fun loadThemes(): Boolean {
        val file = File("themes.txt")

        if (!file.exists()) {
            BuildOff.instance.logger.severe("no themes file found.")
            return false
        }

        val lines = file.readLines().map { it.trim() }.filter { it.isNotEmpty() }

        if (lines.isEmpty()) {
            BuildOff.instance.logger.severe("there are no themes listed.")
            return false
        }

        themes.clear()
        themes.addAll(lines)
        return true
    }

    fun getRandomTheme(): String? {
        if (themes.isEmpty()) return null

        return themes.random()
    }

    fun getThemes(): List<String> = themes.toList()
}