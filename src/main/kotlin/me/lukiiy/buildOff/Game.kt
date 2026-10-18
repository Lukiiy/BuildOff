package me.lukiiy.buildOff

import me.lukiiy.buildOff.arena.MapMaker
import me.lukiiy.flow.Countdown
import me.lukiiy.flow.FDefaults
import me.lukiiy.flow.FUtils.asMini
import me.lukiiy.flow.FUtils.softReset
import me.lukiiy.flow.Flow
import me.lukiiy.flow.FlowPlayer
import me.lukiiy.flow.Minigame
import me.lukiiy.flow.MinigameException
import me.lukiiy.flow.component.BasePlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import java.io.IOException
import java.nio.file.Files
import java.time.Duration
import java.util.UUID

class Game : Minigame() { // todo text + theme selection method
    lateinit var world: World
    var gameState: State = State.COUNTING
    val plots = mutableListOf<Plot>()
    val boards = mutableMapOf<UUID, Board>()
    var currentPlotIdx = -1
    var theme = ""

    val currentPlot: Plot?
        get() = plots.getOrNull(currentPlotIdx)

    val alive: List<FlowPlayer>
        get() = getPlayers().filterIsInstance<FlowPlayer>().filter { it.state == BasePlayer.State.PLAYING }

    private val buildTimer by lazy {
        Timer(entry().buildTime.value.toInt(), TimerDirection.DOWN, onTick = {
            boards.values.forEach { b -> b.update("Building: ${it.formattedTime}") }
        }, onEnd = {
            boards.values.forEach { it.update("Time's Over!") }
            broadcast("Time's Over!".asMini())

            gameState = State.COUNTING

            forEachPlayer {
                it.player.apply {
                    gameMode = GameMode.ADVENTURE
                    allowFlight = true
                    isFlying = true
                }
            }

            Bukkit.getGlobalRegionScheduler().runDelayed(BuildOff.instance, { _ -> startVotingPhase() }, 100L)
        }).apply {
            onTime(120) { broadcast("⌚ 2 minutes remaining!".asMini().color(FDefaults.YELLOW)) }
            onTime(60) { broadcast("⌚ 1 minute remaining!".asMini().color(FDefaults.ORANGE)) }
            onTime(30) { broadcast("⌚ 30 seconds remaining!".asMini().color(FDefaults.ORANGE)) }
            onTime(10) { broadcast("⌚ 10 seconds remaining!".asMini().color(FDefaults.RED)) }
            onTime(5) { broadcast("⌚ 5 seconds remaining!".asMini().color(FDefaults.RED)) }
            onTime(4) { broadcast("⌚ 4 seconds remaining!".asMini().color(FDefaults.RED)) }
            onTime(3) { broadcast("⌚ 3 seconds remaining!".asMini().color(FDefaults.RED)) }
            onTime(2) { broadcast("⌚ 2 seconds remaining!".asMini().color(FDefaults.RED)) }
            onTime(1) { broadcast("⌚ 1 second remaining!".asMini().color(FDefaults.RED)) }
        }
    }

    private var voteTimer: Timer? = null

    override fun listeners(): List<Listener?> = listOf(Listen(this))

    override fun prepare() {
        world = BuildOff.instance.mapMaker.create() ?: throw MinigameException("Failed to generate world!")

        BuildOff.instance.themeFile.apply {
            loadThemes()

            if (getThemes().isEmpty()) throw MinigameException("No themes detected.")
        }
    }

    override fun onStart() {
        val players = getPlayers().filterIsInstance<FlowPlayer>()
        if (players.isEmpty()) return

        plots.addAll(MapMaker.genGrid(world, players.map { it.player.uniqueId }))
        theme = BuildOff.instance.themeFile.getRandomTheme() ?: "???"

        players.forEach {
            val p = it.player
            val plot = plots.first { plot -> plot.owner == p.uniqueId }

            p.teleportAsync(plot.spawn).thenAccept {
                p.scheduler.run(BuildOff.instance, {
                    p.softReset(GameMode.ADVENTURE)

                    p.allowFlight = true
                    p.isFlying = true

                    p.inventory.clear()
                }, null)
            }

            boards[p.uniqueId] = Board(p)
        }

        broadcast(Component.newline().append(" » ".asMini().color(FDefaults.DARK_GRAY)).append("ℹ Build a structure based on a theme within the time limit!".asMini().color(FDefaults.YELLOW)).appendNewline())
        val totalSeconds = 5

        Countdown(BuildOff.instance, Duration.ofSeconds(totalSeconds.toLong()), { c ->
            val secondsLeft = (c + 1).toInt()

            val color = when {
                secondsLeft > 3 -> FDefaults.GREEN
                secondsLeft == 2 -> FDefaults.ORANGE
                secondsLeft == 1 -> FDefaults.RED
                else -> FDefaults.YELLOW
            }

            // Action bar countdown :3
            val active = "▌".repeat(secondsLeft)
            val inactive = "▌".repeat((totalSeconds - secondsLeft))

            forEachPlayer {
                it!!.player.apply {
                    sendActionBar("Game Start ".asMini().append("»".asMini().decorate(TextDecoration.BOLD)).appendSpace().append(active.asMini().color(color)).append(inactive.asMini().color(FDefaults.DARK_GRAY)).append(" $secondsLeft".asMini()))
                    playSound(this, Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1f, 1f + ((totalSeconds - secondsLeft) * .15f))
                }
            }
        }, {
            gameState = State.BUILDING

            addSystem(buildTimer)
            forEachPlayer {
                it!!.player.apply {
                    sendActionBar(Component.empty())

                    showTitle(Title.title("The theme is:".asMini().color(FDefaults.WHITE), theme.asMini().color(FDefaults.ORANGE), Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(4), Duration.ofSeconds(1))))
                }
            }

            broadcast(Component.newline().append(" » ".asMini().color(FDefaults.DARK_GRAY)).append("The chosen theme is: ".asMini().color(FDefaults.LIGHT_YELLOW)).append(theme.asMini().color(FDefaults.LIME)).appendNewline())
        }).start()
    }

    private fun startVotingPhase() {
        removeSystem(buildTimer)

        gameState = State.VOTING

        forEachPlayer {
            it.player.apply {
                inventory.apply {
                    clear()
                    setItem(0, Items.BAD)
                    setItem(1, Items.OK)
                    setItem(2, Items.GOOD)
                }

                showTitle(Title.title("TIME'S UP!".asMini().color(FDefaults.RED), "Voting is starting...".asMini(), Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(1))))
            }
        }

        advanceVote()
    }

    private fun advanceVote() {
        currentPlotIdx++

        if (currentPlotIdx >= plots.size) {
            finish()

            return
        }

        val plot = currentPlot ?: return

        forEachPlayer { fp ->
            val p = fp.player

            p.teleport(plot.spawn)
            p.showTitle(Title.title("Voting Plot".asMini().color(FDefaults.YELLOW), "Plot #${currentPlotIdx + 1}".asMini(), Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(1))))
        }

        voteTimer?.let { removeSystem(it) }

        voteTimer = Timer(entry().voteTime.value.toInt(), TimerDirection.DOWN, onTick = { t ->
            boards.values.forEach { b -> b.update("Voting: ${t.formattedTime}") }
        }, onEnd = {
            advanceVote()
        }).also { addSystem(it) }
    }

    fun vote(voter: Player, score: Int) {
        val plot = currentPlot ?: return

        if (plot.owner == voter.uniqueId) {
            voter.sendActionBar("You can't vote on your own plot.".asMini().color(FDefaults.RED))
            voter.playSound(voter.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)

            return
        }

        if (plot.votes[voter.uniqueId] == score) return
        plot.votes[voter.uniqueId] = score

        voter.sendMessage(" » ".asMini().color(FDefaults.DARK_GRAY).append("Voted $score points!".asMini().color(FDefaults.GREEN)))
        voter.playSound(voter.location, Sound.ENTITY_BAT_HURT, 1f, 1f + (score * .2f))
    }

    private fun finish() {
        gameState = State.ENDED

        voteTimer?.let { removeSystem(it) }

        val winnerPlot = plots.maxByOrNull { it.totalScore }
        val valid = alive.filter { it.player.uniqueId == winnerPlot?.owner }.map { it.player }
        val winnerComp = if (valid.isEmpty()) "Nobody".asMini() else Component.text(valid.joinToString { it.name })

        forEachPlayer { fp ->
            val p = fp.player

            if (valid.contains(p)) {
                p.showTitle(Title.title("VICTORY!".asMini().color(FDefaults.YELLOW), "Score: ${winnerPlot?.totalScore ?: 0}".asMini(), Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))))
                p.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, .75f, 1f)
            } else {
                p.showTitle(Title.title("Game Over!".asMini().color(FDefaults.RED), "Winner: ".asMini().append(winnerComp), Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))))
                p.playSound(p, Sound.ENTITY_GENERIC_EXPLODE, .75f, .5f)
            }

            boards[p.uniqueId]?.destroy()
        }

        winnerPlot?.let { plot -> forEachPlayer { it.player.teleport(plot.spawn) } }

        Bukkit.getGlobalRegionScheduler().runDelayed(BuildOff.instance, { stop() }, 200L)
    }

    override fun onStop() {
        Flow.getInstance().manager.lobby?.let { forEachPlayer { fp -> it.sendToLobby(fp!!) }}

        val wFolder = world.worldFolder

        Bukkit.getServer().unloadWorld(world, false)

        if (wFolder.exists()) {
            try {
                Files.walk(wFolder.toPath()).use { it.sorted(Comparator.reverseOrder()).forEach { p -> p.toFile().delete() } }
            } catch (e: IOException) {
                BuildOff.instance.logger.severe("Could not delete build battle world ${wFolder.name}: ${e.message}")
            }
        }
    }

    class Board(player: Player) {
        private val board = OnlyBoard(player, "Build Battle".asMini().color(FDefaults.YELLOW).decorate(TextDecoration.BOLD))

        fun update(line: String) = board.updateLines(Component.text(line).color(FDefaults.WHITE))
        fun destroy() = board.destroy()
    }

    fun entry(): Entry = entry as Entry
}