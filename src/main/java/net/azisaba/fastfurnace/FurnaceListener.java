package net.azisaba.fastfurnace;

import com.gmail.nossr50.api.ExperienceAPI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import xyz.acrylicstyle.storageBox.utils.StorageBox;
import xyz.acrylicstyle.storageBox.utils.StorageBoxUtils;

import java.util.Map;
import java.util.Random;

public class FurnaceListener implements Listener {

    private final FastFurnace plugin;
    private final ConfigManager config;
    private final Random random = new Random();

    public FurnaceListener(FastFurnace plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().toString().contains("RIGHT")) return;
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        if (offHand.getType() != Material.STONE_SWORD || !offHand.hasItemMeta()) return;
        ItemMeta offMeta = offHand.getItemMeta();
        if (offMeta == null) return;

        PersistentDataContainer pdc = offMeta.getPersistentDataContainer();

        boolean isFurnace = false;
        if (pdc.has(plugin.getFurnaceKey(), PersistentDataType.BYTE)) {
            isFurnace = true;
        } else if (offMeta.hasDisplayName() && offMeta.getDisplayName().contains(config.getFurnaceName())) {
            pdc.set(plugin.getFurnaceKey(), PersistentDataType.BYTE, (byte) 1);
            offHand.setItemMeta(offMeta);
            isFurnace = true;
        }

        if (!isFurnace) return;
        if (mainHand.getType() == Material.AIR) return;

        StorageBox box = StorageBox.getStorageBox(mainHand);

        if (box == null) {
            ItemMeta mainMeta = mainHand.getItemMeta();
            if (mainMeta != null && (mainMeta.hasDisplayName() || mainMeta.hasLore())) {
                player.sendMessage(ChatColor.RED + "このアイテムは精錬できません。");
                return;
            }
        } else {
            ItemStack innerItem = box.getComponentItemStack();
            if (innerItem != null && innerItem.hasItemMeta()) {
                ItemMeta innerMeta = innerItem.getItemMeta();
                if (innerMeta != null && (innerMeta.hasDisplayName() || innerMeta.hasLore())) {
                    player.sendMessage(ChatColor.RED + "このアイテムは精錬できません。");
                    return;
                }
            }
        }

        Material targetMat = box != null ? box.getType() : mainHand.getType();
        long maxProcessable = box != null ? box.getAmount() : mainHand.getAmount();
        long targetAmount = 0;
        if (targetMat == null || targetMat == Material.AIR || maxProcessable <= 0) return;
        int currentFuel = getNumberFromName(offMeta.getDisplayName());
        event.setCancelled(true);

        if (config.getFuels().containsKey(targetMat)) {
            if (player.isSneaking()) {
                if (targetMat == Material.LAVA_BUCKET) {
                    boolean hasBucketBox = StorageBoxUtils.getStorageBoxForType(player.getInventory(), new ItemStack(Material.BUCKET)) != null;
                    targetAmount = hasBucketBox ? maxProcessable : Math.min(maxProcessable, 64);
                } else {
                    targetAmount = maxProcessable;
                }
            } else {
                targetAmount = 1;
            }

            int fuelValuePerItem = config.getFuels().get(targetMat);
            long addedFuel = fuelValuePerItem * targetAmount;
            currentFuel += addedFuel;

            consumeItem(player, mainHand, box, targetAmount);

            if (targetMat == Material.LAVA_BUCKET) {
                giveOrStoreItems(player, Material.BUCKET, targetAmount);
            }

            updateFurnaceName(offHand, offMeta, currentFuel);
            player.sendMessage(ChatColor.GREEN + "相対的時空間かまどの燃料は" + ChatColor.LIGHT_PURPLE + currentFuel + ChatColor.GREEN + "です。");
            return;
        }

        if (config.getRecipes().containsKey(targetMat)) {
            SmeltRecipe recipe = config.getRecipes().get(targetMat);

            if (player.isSneaking()) {
                boolean hasResultBox = StorageBoxUtils.getStorageBoxForType(player.getInventory(), new ItemStack(recipe.getResult())) != null;
                targetAmount = hasResultBox ? maxProcessable : Math.min(maxProcessable, 64);
            } else {
                targetAmount = 1;
            }

            long processCount = Math.min(currentFuel, targetAmount);

            if (processCount <= 0) {
                player.sendMessage(ChatColor.GREEN + "相対的時空間かまどの燃料が足りません。");
                return;
            }

            currentFuel -= processCount;
            consumeItem(player, mainHand, box, processCount);
            updateFurnaceName(offHand, offMeta, currentFuel);

            int smeltingLevel = Math.min(ExperienceAPI.getLevel(player, "SMELTING"), 1000);
            long totalResultAmount = processCount;

            if (processCount > 1000) {
                double chance = smeltingLevel / 2000.0;
                totalResultAmount += (long) (processCount * chance);
            } else {
                for (long i = 0; i < processCount; i++) {
                    if (random.nextInt(2000) <= smeltingLevel) {
                        totalResultAmount++;
                    }
                }
            }
            giveOrStoreItems(player, recipe.getResult(), totalResultAmount);
            if (recipe.getXp() > 0) {
                int xpToAdd = (int) (recipe.getXp() * processCount);
                try {
                    ExperienceAPI.addRawXP(player, "Smelting", (float) xpToAdd, "UNKNOWN");
                } catch (Throwable t) {
                    ExperienceAPI.addXP(player, "SMELTING", xpToAdd);
                }
            }
            player.sendMessage(ChatColor.GREEN + "相対的時空間かまどの残り燃料は" + ChatColor.LIGHT_PURPLE + currentFuel + ChatColor.GREEN + " です");
            return;
        }
        player.sendMessage(ChatColor.RED + "このアイテムは精錬できません。");
    }

    private void consumeItem(Player player, ItemStack mainHand, StorageBox box, long consumeAmount) {
        if (box != null) {
            box.setAmount(box.getAmount() - consumeAmount);
            player.getInventory().setItemInMainHand(box.getItemStack());
        } else {
            mainHand.setAmount(mainHand.getAmount() - (int) consumeAmount);
        }
    }

    private void giveOrStoreItems(Player player, Material material, long amount) {
        if (amount <= 0) return;
        Map.Entry<Integer, StorageBox> entry = StorageBoxUtils.getStorageBoxForType(player.getInventory(), new ItemStack(material));
        if (entry != null) {
            StorageBox outBox = entry.getValue();
            outBox.setAmount(outBox.getAmount() + amount);
            player.getInventory().setItem(entry.getKey(), outBox.getItemStack());
        } else {
            int maxStack = material.getMaxStackSize();
            while (amount > 0) {
                int giveAmount = (int) Math.min(amount, maxStack);
                player.getInventory().addItem(new ItemStack(material, giveAmount));
                amount -= giveAmount;
            }
        }
    }

    private void updateFurnaceName(ItemStack offHand, ItemMeta meta, int newFuel) {
        String displayName = meta.getDisplayName();
        String newName = displayName.replaceAll("\\(\\d+\\)", "(" + newFuel + ")");
        meta.setDisplayName(newName);
        offHand.setItemMeta(meta);
    }

    private int getNumberFromName(String name) {
        String[] parts = name.split("\\(");
        if (parts.length > 1) {
            String numberStr = parts[1].replaceAll("\\)", "");
            try {
                return Integer.parseInt(numberStr);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}