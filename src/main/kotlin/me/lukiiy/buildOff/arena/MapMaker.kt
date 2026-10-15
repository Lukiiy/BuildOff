package me.lukiiy.buildOff.arena

import me.lukiiy.buildOff.Plot
import me.lukiiy.flow.MinigameException
import org.bukkit.*
import org.bukkit.block.structure.Mirror
import org.bukkit.block.structure.StructureRotation
import org.bukkit.generator.ChunkGenerator
import org.bukkit.util.Vector
import java.io.File
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.ceil
import kotlin.math.sqrt

class MapMaker {
    fun create(): World? {
        val creator = WorldCreator("buildoff_" + UUID.randomUUID()).generator(VoidGen()).generateStructures(false).environment(World.Environment.NORMAL).type(WorldType.FLAT)

        val world = creator.createWorld()?.apply {
            setGameRule(GameRules.SPAWN_MOBS, false)
            setGameRule(GameRules.ADVANCE_WEATHER, false)
            setGameRule(GameRules.SPECTATORS_GENERATE_CHUNKS, false)
            setGameRule(GameRules.BLOCK_DROPS, false)
            setGameRule(GameRules.MOB_DROPS, false)

            isAutoSave = false
            spawnLocation = Location(this, 0.0, 97.0, 0.0).toCenterLocation()
        } ?: return null

        return world
    }

    class VoidGen : ChunkGenerator()

    companion object {
        fun genGrid(world: World, players: List<UUID>): List<Plot> {
            if (players.isEmpty()) return emptyList()

            val file = File(Bukkit.getWorldContainer().parentFile, "buildoff").listFiles { _, name -> name.endsWith(".nbt") }?.randomOrNull() ?: throw MinigameException("Uhm, where's the structure file again?")
            val structure = Bukkit.getStructureManager().loadStructure(file)

            val size = structure.size
            val stepX = size.blockX - 1
            val stepZ = size.blockZ - 1
            val gridSize = ceil(sqrt(players.size.toDouble())).toInt()

            var glassOff: List<Vector>? = null
            var minRel: Vector? = null
            var maxRel: Vector? = null
            var spawnRel: Vector? = null
            val plots = mutableListOf<Plot>()

            players.forEachIndexed { idx, uuid ->
                val origin = Location(world, (idx % gridSize * stepX).toDouble(), 64.0, (idx / gridSize * stepZ).toDouble())

                structure.place(origin, false, StructureRotation.NONE, Mirror.NONE, 0, 1f, ThreadLocalRandom.current())

                if (glassOff == null) {
                    val glass = mutableListOf<Vector>()

                    for (x in 0 until size.blockX) {
                        for (y in 0 until size.blockY) {
                            for (z in 0 until size.blockZ) {
                                val block = world.getBlockAt(origin.blockX + x, origin.blockY + y, origin.blockZ + z)

                                if (block.type == Material.GLASS) glass.add(Vector(x, y, z))
                            }
                        }
                    }

                    if (glass.size < 2) throw MinigameException("structure needs at least 2 corners for the build zones. Place 2 glass blocks.")

                    glassOff = glass

                    val minX = glass.minOf { it.x }
                    val minY = glass.minOf { it.y }
                    val minZ = glass.minOf { it.z }
                    val maxX = glass.maxOf { it.x }
                    val maxY = glass.maxOf { it.y }
                    val maxZ = glass.maxOf { it.z }

                    minRel = Vector(minX, minY - 1, minZ)
                    maxRel = Vector(maxX, maxY, maxZ)
                    spawnRel = Vector((minX + maxX) / 2 + .5, minY + 5, (minZ + maxZ) / 2 + .5)
                }

                glassOff.forEach { origin.clone().add(it).block.type = Material.AIR }
                plots.add(Plot(uuid, MapBounds(origin.clone().add(minRel!!), origin.clone().add(maxRel!!)), origin.clone().add(spawnRel!!)))
            }

            return plots
        }
    }
}