package net.azisaba.fastfurnace

import com.gmail.nossr50.api.ExperienceAPI
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import xyz.acrylicstyle.storageBox.utils.StorageBox
import xyz.acrylicstyle.storageBox.utils.StorageBoxUtils
import java.util.Random

class FurnaceListener(
    private val plugin: FastFurnace,
    private val config: ConfigManager,
) : Listener {

    private val random = Random()

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.hand != EquipmentSlot.HAND) return

        val player = event.player
        val offHand = player.inventory.itemInOffHand
        val mainHand = player.inventory.itemInMainHand

        if (offHand.type != Material.STONE_SWORD || !offHand.hasItemMeta()) return
        val offMeta = offHand.itemMeta ?: return

        val pdc = offMeta.persistentDataContainer
        val isFurnace = when {
            pdc.has(plugin.furnaceKey, PersistentDataType.BYTE) -> true
            offMeta.hasDisplayName() && offMeta.displayName.contains(config.furnaceName) -> {
                pdc.set(plugin.furnaceKey, PersistentDataType.BYTE, 1.toByte())
                offHand.itemMeta = offMeta
                true
            }
            else -> false
        }

        if (!isFurnace) return
        if (mainHand.type == Material.AIR) return

        val box = StorageBox.getStorageBox(mainHand)
        val targetMat: Material? = if (box != null) box.type else mainHand.type

        if (targetMat == null || targetMat == Material.AIR || (targetMat !in config.fuels && targetMat !in config.recipes)) {
            return
        }

        val checkedItem = if (box != null) box.componentItemStack else mainHand
        if (checkedItem != null && checkedItem.hasItemMeta()) {
            val meta = checkedItem.itemMeta
            if (meta != null && (meta.hasDisplayName() || meta.hasLore())) {
                player.sendMessage("${ChatColor.RED}このアイテムは精錬できません。")
                return
            }
        }

        val maxProcessable = box?.amount ?: mainHand.amount.toLong()
        if (maxProcessable <= 0) return
        var currentFuel = getNumberFromName(offMeta.displayName)
        event.isCancelled = true

        val fuelValuePerItem = config.fuels[targetMat]
        if (fuelValuePerItem != null) {
            val targetAmount = when {
                !player.isSneaking -> 1L
                targetMat == Material.LAVA_BUCKET && !hasStorageBoxFor(player, Material.BUCKET) -> minOf(maxProcessable, 64L)
                else -> maxProcessable
            }

            currentFuel += fuelValuePerItem * targetAmount

            consumeItem(player, mainHand, box, targetAmount)

            if (targetMat == Material.LAVA_BUCKET) {
                giveOrStoreItems(player, Material.BUCKET, targetAmount)
            }

            updateFurnaceName(offHand, offMeta, currentFuel)
            player.sendMessage("${ChatColor.GREEN}相対的時空間かまどの燃料は${ChatColor.LIGHT_PURPLE}$currentFuel${ChatColor.GREEN}です。")
            return
        }

        val recipe = config.recipes[targetMat]
        if (recipe != null) {
            val targetAmount = when {
                !player.isSneaking -> 1L
                hasStorageBoxFor(player, recipe.result) -> maxProcessable
                else -> minOf(maxProcessable, 64L)
            }

            val processCount = minOf(currentFuel, targetAmount)

            if (processCount <= 0) {
                player.sendMessage("${ChatColor.GREEN}相対的時空間かまどの燃料が足りません。")
                return
            }

            currentFuel -= processCount
            consumeItem(player, mainHand, box, processCount)
            updateFurnaceName(offHand, offMeta, currentFuel)

            val smeltingLevel = minOf(ExperienceAPI.getLevel(player, "SMELTING"), 1000)
            var totalResultAmount = processCount

            if (processCount > 1000) {
                val chance = smeltingLevel / 2000.0
                totalResultAmount += (processCount * chance).toLong()
            } else {
                for (i in 0 until processCount) {
                    if (random.nextInt(2000) <= smeltingLevel) {
                        totalResultAmount++
                    }
                }
            }
            giveOrStoreItems(player, recipe.result, totalResultAmount)
            if (recipe.xp > 0) {
                val xpToAdd = (recipe.xp * processCount).toInt()
                try {
                    ExperienceAPI.addRawXP(player, "Smelting", xpToAdd.toFloat(), "UNKNOWN")
                } catch (t: Throwable) {
                    ExperienceAPI.addXP(player, "SMELTING", xpToAdd)
                }
            }
            player.sendMessage("${ChatColor.GREEN}相対的時空間かまどの残り燃料は${ChatColor.LIGHT_PURPLE}$currentFuel${ChatColor.GREEN} です")
            return
        }
        player.sendMessage("${ChatColor.RED}このアイテムは精錬できません。")
    }

    private fun hasStorageBoxFor(player: Player, material: Material): Boolean =
        StorageBoxUtils.getStorageBoxForType(player.inventory, ItemStack(material)) != null

    private fun consumeItem(player: Player, mainHand: ItemStack, box: StorageBox?, consumeAmount: Long) {
        if (box != null) {
            box.amount = box.amount - consumeAmount
            player.inventory.setItemInMainHand(box.itemStack)
        } else {
            mainHand.amount = mainHand.amount - consumeAmount.toInt()
        }
    }

    private fun giveOrStoreItems(player: Player, material: Material, amount: Long) {
        if (amount <= 0) return
        val entry = StorageBoxUtils.getStorageBoxForType(player.inventory, ItemStack(material))
        if (entry != null) {
            val outBox = entry.value
            outBox.amount = outBox.amount + amount
            player.inventory.setItem(entry.key, outBox.itemStack)
        } else {
            var remaining = amount
            val maxStack = material.maxStackSize
            while (remaining > 0) {
                val giveAmount = minOf(remaining, maxStack.toLong()).toInt()
                player.giveOrStash(ItemStack(material, giveAmount))
                remaining -= giveAmount
            }
        }
    }

    private fun updateFurnaceName(offHand: ItemStack, meta: ItemMeta, newFuel: Long) {
        meta.setDisplayName(meta.displayName.replace(FUEL_PATTERN, "($newFuel)"))
        offHand.itemMeta = meta
    }

    private fun getNumberFromName(name: String): Long =
        FUEL_PATTERN.find(name)?.groupValues?.get(1)?.toLongOrNull() ?: 0L

    companion object {
        private val FUEL_PATTERN = Regex("""\((\d+)\)""")
    }
}
