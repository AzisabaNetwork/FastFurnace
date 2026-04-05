package net.azisaba.fastfurnace;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class FastFurnace extends JavaPlugin {

    private ConfigManager configManager;
    private NamespacedKey furnaceKey;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        configManager = new ConfigManager(this);
        configManager.loadConfigData();

        furnaceKey = new NamespacedKey(this, "fast_furnace_item");

        this.getServer().getPluginManager().registerEvents(new FurnaceListener(this, configManager), this);
        this.getCommand("fastfurnace").setExecutor(new FurnaceCommand(this, configManager));
    }

    public NamespacedKey getFurnaceKey() {
        return furnaceKey;
    }

}