package net.azisaba.fastfurnace

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.RecipeChoice
import java.util.EnumMap

class ConfigManager(private val plugin: FastFurnace) {

    var furnaceName: String = DEFAULT_FURNACE_NAME
        private set
    var furnaceLore: List<String> = emptyList()
        private set
    val fuels: MutableMap<Material, Int> = EnumMap(Material::class.java)
    val recipes: MutableMap<Material, SmeltRecipe> = EnumMap(Material::class.java)

    fun loadConfigData() {
        plugin.reloadConfig()
        val config = plugin.config
        furnaceName = config.getString("furnace-name", DEFAULT_FURNACE_NAME) ?: DEFAULT_FURNACE_NAME
        furnaceLore = config.getStringList("furnace-lore")

        fuels.clear()
        config.getConfigurationSection("fuels")?.let { section ->
            for (key in section.getKeys(false)) {
                val material = parseMaterial(key, "fuels") ?: continue
                fuels[material] = section.getInt(key)
            }
        }

        loadRecipes()
    }

    /**
     * かまどレシピ一覧を作り直す。
     * use-vanilla-recipes が有効ならサーバーに登録されている全てのかまどレシピを取り込み、
     * その上で smelting セクションの設定で上書きする。
     */
    fun loadRecipes() {
        val config = plugin.config
        val defaultXp = config.getInt("default-xp", 0)
        val disabled = config.getStringList("disabled-smelting")
            .mapNotNull { parseMaterial(it, "disabled-smelting") }
            .toSet()

        recipes.clear()
        if (config.getBoolean("use-vanilla-recipes", true)) {
            val iterator = Bukkit.recipeIterator()
            while (iterator.hasNext()) {
                val recipe = iterator.next() as? FurnaceRecipe ?: continue
                val result = recipe.result.type
                if (result == Material.AIR) continue
                for (input in inputMaterials(recipe)) {
                    recipes.putIfAbsent(input, SmeltRecipe(result, defaultXp))
                }
            }
        }

        config.getConfigurationSection("smelting")?.let { section ->
            for (key in section.getKeys(false)) {
                val input = parseMaterial(key, "smelting") ?: continue
                val resultName = section.getString("$key.result")
                val result = resultName?.let { parseMaterial(it, "smelting.$key.result") }
                    ?: recipes[input]?.result
                if (result == null) {
                    plugin.logger.warning("smelting.$key の result が指定されていません。")
                    continue
                }
                recipes[input] = SmeltRecipe(result, section.getInt("$key.xp", defaultXp))
            }
        }

        disabled.forEach { recipes.remove(it) }
    }

    private fun inputMaterials(recipe: FurnaceRecipe): Collection<Material> =
        when (val choice = recipe.inputChoice) {
            is RecipeChoice.MaterialChoice -> choice.choices
            // NBT 付きの入力は名前・説明文付きアイテムを弾く仕様と合わないため対象外
            is RecipeChoice.ExactChoice -> emptyList()
            else -> listOf(recipe.input.type)
        }.filter { it != Material.AIR }

    private fun parseMaterial(name: String, path: String): Material? {
        val material = Material.matchMaterial(name)
        if (material == null) {
            plugin.logger.warning("$path: 不明なアイテム '$name' を無視しました。")
        }
        return material
    }

    companion object {
        private const val DEFAULT_FURNACE_NAME = "§5相対的時空間§7かまど"
    }

}
