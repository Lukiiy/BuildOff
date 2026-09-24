package me.lukiiy.buildOff

import me.lukiiy.flow.FDefaults
import me.lukiiy.flow.FUtils.asMini
import me.lukiiy.flow.GameEntry
import me.lukiiy.flow.setting.DoubleSetting
import java.util.function.Supplier

class Entry : GameEntry("buildbattle", "Build Battle", Supplier { Game() }, "Build Battle".asMini().color(FDefaults.ORANGE)) {
    val buildTime: DoubleSetting = setting(DoubleSetting("buildTime", "Build time (Seconds)", "", 60.0, 600.0, 300.0, 30.0))
    val voteTime: DoubleSetting = setting(DoubleSetting("voteTime", "Vote time/p", "", 5.0, 30.0, 15.0, 1.0))
}