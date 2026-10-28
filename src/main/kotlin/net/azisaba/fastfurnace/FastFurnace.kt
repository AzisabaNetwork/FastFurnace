package net.azisaba.fastfurnace

import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin

class FastFurnace : JavaPlugin() {

    lateinit var furnaceKey: NamespacedKey
        private set

    override fun onEnable() {
        saveDefaultConfig()
        val configManager = ConfigManager(this)
        configManager.loadConfigData()

        furnaceKey = NamespacedKey(this, "fast_furnace_item")

        server.pluginManager.registerEvents(FurnaceListener(this, configManager), this)
        getCommand("fastfurnace")?.setExecutor(FurnaceCommand(this, configManager))

        // 他プラグインが追加したかまどレシピも拾えるよう、起動完了後にもう一度読み込む
        server.scheduler.runTask(this, Runnable { configManager.loadRecipes() })
    }

}
