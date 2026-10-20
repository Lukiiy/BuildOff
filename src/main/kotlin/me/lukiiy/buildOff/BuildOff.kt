package me.lukiiy.buildOff

import me.lukiiy.buildOff.arena.MapMaker
import me.lukiiy.flow.Flow
import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin

class BuildOff : JavaPlugin() {
    val mapMaker = MapMaker()
    val themeFile = ThemeFile()

    var key: NamespacedKey? = null
        private set

    override fun onEnable() {
        key = NamespacedKey(this, "mapEntity")

        Flow.getInstance().manager.register(Entry())
    }

    companion object {
        val instance: BuildOff
            get() = getPlugin(BuildOff::class.java)
    }
}
