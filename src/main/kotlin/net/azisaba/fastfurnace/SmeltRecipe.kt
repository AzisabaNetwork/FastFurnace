package net.azisaba.fastfurnace

import org.bukkit.Material

/**
 * @param mcmmoXp mcMMO の精錬スキルに入る経験値（1個あたり）
 * @param vanillaXp バニラの経験値（1個あたり）
 */
data class SmeltRecipe(val result: Material, val mcmmoXp: Float, val vanillaXp: Float)
