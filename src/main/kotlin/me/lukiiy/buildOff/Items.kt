package me.lukiiy.buildOff

import io.papermc.paper.datacomponent.DataComponentTypes
import me.lukiiy.flow.FDefaults
import me.lukiiy.flow.FUtils.asMini
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object Items {
    val BAD = ItemStack(Material.RED_CONCRETE).apply {
        setData(DataComponentTypes.ITEM_NAME, "Bad".asMini().color(FDefaults.RED))
    }

    val OK = ItemStack(Material.YELLOW_CONCRETE).apply {
        setData(DataComponentTypes.ITEM_NAME, "Ok".asMini().color(FDefaults.ORANGE))
    }

    val GOOD = ItemStack(Material.LIME_CONCRETE).apply {
        setData(DataComponentTypes.ITEM_NAME, "Good".asMini().color(FDefaults.GREEN))
    }
}