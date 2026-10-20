package me.lukiiy.buildOff.arena

import io.papermc.paper.entity.LookAnchor
import me.lukiiy.buildOff.BuildOff
import me.lukiiy.buildOff.Plot
import me.lukiiy.flow.MinigameException
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.*
import org.bukkit.block.structure.Mirror
import org.bukkit.block.structure.StructureRotation
import org.bukkit.entity.Display
import org.bukkit.entity.TextDisplay
import org.bukkit.entity.WanderingTrader
import org.bukkit.generator.ChunkGenerator
import org.bukkit.persistence.PersistentDataType
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
        private val floorNPCDisplay = MiniMessage.miniMessage().deserialize("<gradient:#FFD05E:#FF8C5E>Floor Change</gradient>")

        fun genGrid(world: World, players: List<UUID>): List<Plot> {
            if (players.isEmpty()) return emptyList()

            val file = File(Bukkit.getWorldContainer().parentFile, "buildoff").listFiles { _, name -> name.endsWith(".nbt") }?.randomOrNull() ?: throw MinigameException("Uhm, where's the structure file again?")
            val structure = Bukkit.getStructureManager().loadStructure(file)

            val size = structure.size
            val stepX = size.blockX - 1
            val stepZ = size.blockZ - 1
            val gridSize = ceil(sqrt(players.size.toDouble())).toInt()
            val palette = structure.palettes.firstOrNull() ?: throw MinigameException("no palette found")

            val glass = mutableListOf<Vector>()
            var villagerSpot: Vector? = null

            for (blockInfo in palette.blocks) {
                val pos = blockInfo.location
                val mat = blockInfo.blockData.material

                if (mat == Material.GLASS) glass.add(Vector(pos.x, pos.y, pos.z))
                if (mat == Material.YELLOW_STAINED_GLASS) villagerSpot = Vector(pos.x, pos.y, pos.z)
            }

            if (glass.size < 2) throw MinigameException("structure needs at least 2 corners for the build zones. Place 2 glass blocks.")

            val minX = glass.minOf { it.x }
            val minY = glass.minOf { it.y }
            val minZ = glass.minOf { it.z }
            val maxX = glass.maxOf { it.x }
            val maxY = glass.maxOf { it.y }
            val maxZ = glass.maxOf { it.z }
            val minRel = Vector(minX, minY - 1, minZ)
            val maxRel = Vector(maxX, maxY, maxZ)
            val spawnRel = Vector((minX + maxX) / 2 + .5, minY + 5, (minZ + maxZ) / 2 + .5)

            val plots = mutableListOf<Plot>()

            players.forEachIndexed { idx, uuid ->
                val origin = Location(world, (idx % gridSize * stepX).toDouble(), 64.0, (idx / gridSize * stepZ).toDouble())

                structure.place(origin, false, StructureRotation.NONE, Mirror.NONE, 0, 1f, ThreadLocalRandom.current())
                glass.forEach { origin.clone().add(it).block.type = Material.AIR }

                villagerSpot?.let { rel ->
                    val loc = origin.clone().add(rel)
                    loc.block.type = Material.AIR

                    val center = origin.clone().add(spawnRel).apply { y = loc.y }

                    loc.world.spawn(loc.add(.5, 0.0, .5), WanderingTrader::class.java) {
                        it.apply {
                            lookAt(center, LookAnchor.FEET)
                            setAI(false)
                            persistentDataContainer.set(BuildOff.instance.key!!, PersistentDataType.BOOLEAN, true)

                            despawnDelay = 0
                        }
                    }

                    loc.world.spawn(loc.add(0.0, 2.5, 0.0), TextDisplay::class.java) {
                        it.apply {
                            lookAt(center, LookAnchor.FEET)
                            text(floorNPCDisplay)
                            persistentDataContainer.set(BuildOff.instance.key!!, PersistentDataType.BOOLEAN, true)

                            billboard = Display.Billboard.FIXED
                        }
                    }
                }

                plots.add(Plot(uuid, MapBounds(origin.clone().add(minRel), origin.clone().add(maxRel)), origin.clone().add(spawnRel)))
            }

            return plots
        }
    }
}