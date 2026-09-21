package me.lukiiy.buildOff.arena

import org.bukkit.Location

data class MapBounds(val first: Location, val second: Location) {
    val minX = minOf(first.blockX, second.blockX)
    val minY = minOf(first.blockY, second.blockY)
    val minZ = minOf(first.blockZ, second.blockZ)
    val maxX = maxOf(first.blockX, second.blockX)
    val maxY = maxOf(first.blockY, second.blockY)
    val maxZ = maxOf(first.blockZ, second.blockZ)

    operator fun contains(location: Location): Boolean = location.world == first.world && isInBoundsHorizontally(location) && isInBoundsVertically(location)

    fun isInBoundsVertically(location: Location): Boolean = location.blockY in minY..maxY
    fun isInBoundsHorizontally(location: Location): Boolean = location.blockX in minX..maxX && location.blockZ in minZ..maxZ
}