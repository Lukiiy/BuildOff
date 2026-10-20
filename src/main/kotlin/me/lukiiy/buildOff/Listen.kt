package me.lukiiy.buildOff

import io.papermc.paper.entity.LookAnchor
import io.papermc.paper.event.player.PlayerPickBlockEvent
import me.lukiiy.flow.FDefaults
import me.lukiiy.flow.FUtils.asMini
import me.lukiiy.flow.FlowPlayer
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.*
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause
import org.bukkit.event.vehicle.VehicleEnterEvent
import org.bukkit.inventory.ItemStack
import java.util.EnumSet

class Listen(private val game: Game) : Listener {
    val okTp: EnumSet<TeleportCause> = EnumSet.of(TeleportCause.COMMAND, TeleportCause.PLUGIN, TeleportCause.UNKNOWN)

    @EventHandler
    fun move(e: PlayerMoveEvent) {
        if (game.gameState == State.BUILDING) {
            val p = e.player
            val plot = game.plots.firstOrNull { it.owner == p.uniqueId } ?: return

            if (!plot.bounds.isInBoundsVertically(e.to)) {
                e.isCancelled = true

                p.sendMessage("⚠ You cannot leave your plot area!".asMini().color(FDefaults.RED))
            }
        }
    }

    @EventHandler
    fun blockPlace(e: BlockPlaceEvent) {
        if (game.gameState != State.BUILDING) {
            e.isCancelled = true
            return
        }

        val plot = game.plots.firstOrNull { it.owner == e.player.uniqueId } ?: return

        if (!plot.bounds.contains(e.block.location)) {
            e.isCancelled = true

            e.player.sendMessage("⚠ Build within your plot boundary!".asMini().color(FDefaults.RED))
        }
    }

    @EventHandler
    fun blockBreak(e: BlockBreakEvent) {
        if (game.gameState != State.BUILDING) {
            e.isCancelled = true
            return
        }

        val plot = game.plots.firstOrNull { it.owner == e.player.uniqueId } ?: return

        if (!plot.bounds.contains(e.block.location)) e.isCancelled = true
    }

    @EventHandler
    fun interact(e: PlayerInteractEvent) {
        val p = e.player
        if (p.world != game.world) return

        if (game.gameState == State.VOTING) {
            val item = e.item ?: return

            val score = when (item.type) {
                Material.RED_DYE -> 1
                Material.YELLOW_DYE -> 2
                Material.LIME_DYE -> 3
                else -> return
            }

            e.isCancelled = true

            game.vote(p, score)
            return
        }

        if (game.gameState == State.BUILDING) {
            val plot = game.plots.firstOrNull { it.owner == p.uniqueId } ?: return
            val clicked = e.clickedBlock ?: return

            if (!plot.bounds.contains(clicked.location)) e.isCancelled = true
        }
    }

    @EventHandler
    fun entityInteract(e: PlayerInteractEntityEvent) {
        if (game.gameState != State.BUILDING) {
            e.isCancelled = true
            return
        }

        val player = e.player
        val plot = game.plots.firstOrNull { it.owner == player.uniqueId } ?: return
        val entity = e.rightClicked

        if (entity.persistentDataContainer.has(BuildOff.instance.key!!) && entity.type == EntityType.WANDERING_TRADER && player.inventory.itemInMainHand.type.isBlock && plot.setFloor(player.inventory.itemInMainHand.type)) {
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f)
            player.sendActionBar("☄ The floor has changed!".asMini().color(FDefaults.ORANGE))
        }

        if (!plot.bounds.contains(entity.location)) e.isCancelled = true
    }

    @EventHandler
    fun creatureSpawn(e: CreatureSpawnEvent) {
        if (game.gameState != State.BUILDING) {
            e.isCancelled = true

            return
        }

        val plot = game.plots.firstOrNull { it.bounds.contains(e.location) }
        if (plot == null) {
            e.isCancelled = true
            return
        }

        e.entity.apply {
            setAI(false)

            lookAt(game.getPlayers().filterIsInstance<FlowPlayer>().firstOrNull { it.player.uniqueId == plot.owner }?.player?.location ?: plot.spawn, LookAnchor.FEET)

            health = 0.1
            isPersistent = false
            isCollidable = false

            (this as? ArmorStand)?.setArms(true)
        }
    }

    @EventHandler
    fun projectileLaunch(e: ProjectileLaunchEvent) {
        if (e.entity.shooter is Player) e.isCancelled = true
    }

    @EventHandler
    fun vehicleEnter(e: VehicleEnterEvent) {
        if (e.entered is Player) e.isCancelled = true
    }

    @EventHandler
    fun bucketEmpty(e: PlayerBucketEmptyEvent) {
        val plot = game.plots.firstOrNull { it.owner == e.player.uniqueId } ?: return

        if (!plot.bounds.contains(e.block.location) || game.gameState != State.BUILDING) e.isCancelled = true
    }

    @EventHandler
    fun blockRedstone(e: BlockRedstoneEvent) {
        e.newCurrent = 0
    }

    @EventHandler
    fun pistonExtend(e: BlockPistonExtendEvent) {
        e.isCancelled = true
    }

    @EventHandler
    fun dispense(e: BlockDispenseEvent) {
        e.isCancelled = true
    }

    @EventHandler
    fun blockTransform(e: BlockFromToEvent) {
        val plot = game.plots.firstOrNull { it.bounds.contains(e.block.location) } ?: run {
            e.isCancelled = true

            return
        }

        if (game.gameState != State.BUILDING || !plot.bounds.contains(e.toBlock.location)) e.isCancelled = true
    }

    @EventHandler
    fun blockForm(e: BlockFormEvent) {
        val plot = game.plots.firstOrNull { it.bounds.contains(e.block.location) } ?: run {
            e.isCancelled = true
            return
        }

        if (game.gameState != State.BUILDING) e.isCancelled = true
    }

    @EventHandler
    fun blockExplode(e: BlockExplodeEvent) {
        e.isCancelled = true
    }

    @EventHandler
    fun entityExplode(e: EntityExplodeEvent) {
        e.isCancelled = true
    }

    @EventHandler
    fun consume(e: PlayerItemConsumeEvent) {
        if (game.gameState == State.BUILDING) e.isCancelled = true
    }

    @EventHandler
    fun drop(e: PlayerDropItemEvent) {
        if (game.gameState != State.BUILDING) e.isCancelled = true
    }

    @EventHandler
    fun pickBlock(e: PlayerPickBlockEvent) {
        if (!e.isIncludeData) return

        e.isCancelled = true

        e.player.inventory.setItem(e.targetSlot, ItemStack.of(e.block.type))
    }

    @EventHandler
    fun teleport(e: PlayerTeleportEvent) {
        if (game.gameState == State.BUILDING && !okTp.contains(e.cause)) e.isCancelled = true
    }

    @EventHandler
    fun bed(e: PlayerBedEnterEvent) {
        e.isCancelled = true

        if (!e.player.world.isDayTime || e.player.world.isThundering) e.player.sendActionBar("You may not rest now; there's a build competition happening".asMini())
    }

    @EventHandler
    fun dmg(e: EntityDamageEvent) {
        if (e.entity is Player || e.entity.persistentDataContainer.has(BuildOff.instance.key!!)) e.isCancelled = true
    }
}