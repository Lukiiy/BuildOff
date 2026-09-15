package me.lukiiy.buildOff

import org.bukkit.plugin.java.JavaPlugin

class BuildOff : JavaPlugin() {
    override fun onEnable() {}

    override fun onDisable() {}

    companion object {
        fun getInstance(): BuildOff = getPlugin(BuildOff::class.java)
    }
}
