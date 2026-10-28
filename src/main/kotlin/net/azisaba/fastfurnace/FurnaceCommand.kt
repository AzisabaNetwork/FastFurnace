package net.azisaba.fastfurnace

import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class FurnaceCommand(
    private val plugin: FastFurnace,
    private val config: ConfigManager,
) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("コンソールからは実行できません。")
            return true
        }

        if (!sender.hasPermission("fastfurnace.admin")) {
            sender.sendMessage("${ChatColor.RED}権限がありません。")
            return true
        }

        val furnace = ItemStack(Material.STONE_SWORD)
        furnace.itemMeta?.let { meta ->
            meta.setDisplayName("${config.furnaceName}(0)")
            meta.lore = config.furnaceLore
            meta.persistentDataContainer.set(plugin.furnaceKey, PersistentDataType.BYTE, 1.toByte())
            meta.isUnbreakable = true
            meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_ENCHANTS,
            )
            furnace.itemMeta = meta
        }

        sender.giveOrStash(furnace)
        sender.sendMessage("${ChatColor.AQUA}相対的時空間かまどを付与しました。")
        return true
    }

}
