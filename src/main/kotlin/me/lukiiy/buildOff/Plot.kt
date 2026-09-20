package me.lukiiy.buildOff

import me.lukiiy.buildOff.arena.MapBounds
import org.bukkit.Location
import java.util.UUID

data class Plot(val owner: UUID, val bounds: MapBounds, val spawnLocation: Location, val votes: MutableMap<UUID, Int> = mutableMapOf()) {
    val totalScore: Int
        get() = votes.values.sum()
}