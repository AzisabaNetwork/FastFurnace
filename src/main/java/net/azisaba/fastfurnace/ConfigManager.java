package net.azisaba.fastfurnace;

import org.bukkit.Material;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final FastFurnace plugin;
    private String furnaceName;
    private final Map<Material, Integer> fuels = new EnumMap<>(Material.class);
    private final Map<Material, SmeltRecipe> recipes = new EnumMap<>(Material.class);

    private List<String> furnaceLore;

    public ConfigManager(FastFurnace plugin) {
        this.plugin = plugin;
    }

    public void loadConfigData() {
        plugin.reloadConfig();
        furnaceName = plugin.getConfig().getString("furnace-name", "§5相対的時空間§7かまど");

        fuels.clear();
        if (plugin.getConfig().contains("fuels")) {
            for (String key : plugin.getConfig().getConfigurationSection("fuels").getKeys(false)) {
                fuels.put(Material.valueOf(key.toUpperCase()), plugin.getConfig().getInt("fuels." + key));
            }
        }

        recipes.clear();
        if (plugin.getConfig().contains("smelting")) {
            for (String key : plugin.getConfig().getConfigurationSection("smelting").getKeys(false)) {
                Material result = Material.valueOf(plugin.getConfig().getString("smelting." + key + ".result").toUpperCase());
                int xp = plugin.getConfig().getInt("smelting." + key + ".xp");
                recipes.put(Material.valueOf(key.toUpperCase()), new SmeltRecipe(result, xp));
            }
        }
        furnaceLore = plugin.getConfig().getStringList("furnace-lore");
    }

    public String getFurnaceName() {
        return furnaceName;
    }

    public Map<Material, Integer> getFuels() {
        return fuels;
    }

    public Map<Material, SmeltRecipe> getRecipes() {
        return recipes;
    }

    public List<String> getFurnaceLore() {
        return furnaceLore;
    }

}
