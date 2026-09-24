package me.lukiiy.buildOff.arena

import me.lukiiy.buildOff.Plot
import me.lukiiy.flow.MinigameException
import org.bukkit.*
import org.bukkit.block.structure.Mirror
import org.bukkit.block.structure.StructureRotation
import org.bukkit.generator.ChunkGenerator
import java.io.File
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.sqrt

class MapMaker {
    fun create(): World? {
        val creator = WorldCreator("buildoff_" + UUID.randomUUID()).generator(VoidGen()).generateStructures(false).environment(World.Environment.NORMAL).type(WorldType.FLAT)

        val world = creator.createWorld()?.apply {
            setGameRule(GameRules.SPAWN_MOBS, false)
            setGameRule(GameRules.ADVANCE_WEATHER, false)
            setGameRule(GameRules.SPECTATORS_GENERATE_CHUNKS, false)
            isAutoSave = false
            spawnLocation = Location(this, 0.0, 97.0, 0.0).toCenterLocation()
        } ?: return null

        return world
    }

    class VoidGen : ChunkGenerator()

    companion object {
        fun genGrid(world: World, players: List<UUID>): List<Plot> {
            val count = players.size
            if (count == 0) return emptyList()

            val file = File(Bukkit.getWorldContainer().getParentFile(), "buildoff").listFiles { `_`: File?, name: String? -> name!!.endsWith(".nbt") }?.first() ?: throw MinigameException("Uhm, where's the structure file again?")
            val structure = Bukkit.getStructureManager().loadStructure(file)

            val size = structure.size
            val sizeX = size.blockX
            val sizeZ = size.blockZ
            val stepX = sizeX - 1
            val stepZ = sizeZ - 1
            val gridSize = ceil(sqrt(count.toDouble())).toInt()
            val plots = mutableListOf<Plot>()

            players.forEachIndexed { idx, uuid ->
                val gridX = idx % gridSize
                val gridZ = idx / gridSize
                val origin = Location(world, (gridX * stepX).toDouble(), 64.0, (gridZ * stepZ).toDouble())

                structure.place(origin, false, StructureRotation.NONE, Mirror.NONE, 0, 1.0f, ThreadLocalRandom.current())
                plots.add(getPlot(uuid, world, origin, sizeX, size.blockY, sizeZ))
            }

            return plots
        }

        private fun getPlot(owner: UUID, world: World, origin: Location, sizeX: Int, sizeY: Int, sizeZ: Int): Plot { // TODO: i mean, one structure is enough so why the trouble to code this if im just gonna repeat basically the same results
            val glass = mutableListOf<Location>()

            for (x in origin.blockX until (origin.blockX + sizeX)) {
                for (y in origin.blockY until (origin.blockY + sizeY)) {
                    for (z in origin.blockZ until (origin.blockZ + sizeZ)) {
                        val block = world.getBlockAt(x, y, z)

                        if (block.type == Material.GLASS) {
                            glass.add(block.location.clone())

                            block.type = Material.AIR
                        }
                    }
                }
            }

            if (glass.size < 2) throw MinigameException("structure needs at least 2 corners for the build zones. Place 2 glass blocks.")

            val minX = glass.minOf { it.blockX }
            val minY = glass.minOf { it.blockY }
            val minZ = glass.minOf { it.blockZ }
            val maxX = glass.maxOf { it.blockX }
            val maxY = glass.maxOf { it.blockY }
            val maxZ = glass.maxOf { it.blockZ }

            val bounds = MapBounds(Location(world, minX.toDouble(), minY.toDouble(), minZ.toDouble()), Location(world, maxX.toDouble(), maxY.toDouble(), maxZ.toDouble()))

            val centerX = (minX + maxX) / 2.0 + .5
            val centerZ = (minZ + maxZ) / 2.0 + .5

            return Plot(owner, bounds, Location(world, centerX, minY.toDouble() + 1.0, centerZ))
        }
    }
}