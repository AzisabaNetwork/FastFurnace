package net.azisaba.fastfurnace;

import org.bukkit.Material;

public class SmeltRecipe {

    public final Material result;
    public final int xp;

    public SmeltRecipe(Material result, int xp) {
        this.result = result;
        this.xp = xp;
    }

    public Material getResult() {
        return result;
    }

    public int getXp() {
        return xp;
    }

}
