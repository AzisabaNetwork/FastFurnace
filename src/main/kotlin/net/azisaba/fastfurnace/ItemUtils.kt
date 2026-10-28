package net.azisaba.fastfurnace

import net.azisaba.itemstash.ItemStash
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

fun Player.giveOrStash(item: ItemStack) {
    val leftovers = inventory.addItem(item)
    for (leftover in leftovers.values) {
        ItemStash.getInstance().addItemToStash(uniqueId, leftover)
    }
}
