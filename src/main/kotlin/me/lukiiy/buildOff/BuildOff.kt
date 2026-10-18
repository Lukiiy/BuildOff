package me.lukiiy.buildOff

import me.lukiiy.buildOff.arena.MapMaker
import me.lukiiy.flow.Flow
import org.bukkit.plugin.java.JavaPlugin

class BuildOff : JavaPlugin() {
    val mapMaker = MapMaker()
    val themeFile = ThemeFile()

    override fun onEnable() {
        Flow.getInstance().manager.register(Entry())
    }

    companion object {
        val instance: BuildOff
            get() = getPlugin(BuildOff::class.java)
    }
}
