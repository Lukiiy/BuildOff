package me.lukiiy.buildOff

import me.lukiiy.buildOff.arena.MapBounds
import org.bukkit.Location
import org.bukkit.Material
import java.util.UUID

data class Plot(val owner: UUID, val bounds: MapBounds, val spawn: Location, val votes: MutableMap<UUID, Int> = mutableMapOf()) {
    val totalScore: Int
        get() = votes.values.sum()

    fun setFloor(material: Material) {
        val world = bounds.first.world ?: return
        val floorY = bounds.minY - 1

        for (x in bounds.minX..bounds.maxX) {
            for (z in bounds.minZ..bounds.maxZ) world.getBlockAt(x, floorY, z).type = material
        }
    }
}