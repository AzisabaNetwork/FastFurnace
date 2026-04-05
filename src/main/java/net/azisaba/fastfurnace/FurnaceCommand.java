package net.azisaba.fastfurnace;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class FurnaceCommand implements CommandExecutor {

    private final FastFurnace plugin;
    private final ConfigManager config;

    public FurnaceCommand(FastFurnace plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("コンソールからは実行できません。");
            return true;
        }

        if (!sender.hasPermission("fastfurnace.admin")) {
            sender.sendMessage(ChatColor.RED + "権限がありません。");
            return true;
        }

        Player player = (Player) sender;
        ItemStack furnace = new ItemStack(Material.STONE_SWORD);
        ItemMeta meta = furnace.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(config.getFurnaceName() + "(0)");
            meta.setLore(config.getFurnaceLore());
            meta.getPersistentDataContainer().set(plugin.getFurnaceKey(), PersistentDataType.BYTE, (byte) 1);
            meta.setUnbreakable(true);
            meta.addItemFlags(
                    ItemFlag.HIDE_ATTRIBUTES,
                    ItemFlag.HIDE_UNBREAKABLE,
                    ItemFlag.HIDE_ENCHANTS
            );
            furnace.setItemMeta(meta);
        }

        player.getInventory().addItem(furnace);
        player.sendMessage(ChatColor.AQUA + "相対的時空間かまどを付与しました。");
        return true;
    }

}
