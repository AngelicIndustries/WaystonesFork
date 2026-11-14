package net.blay09.mods.waystones.compat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import xonin.backhand.api.core.BackhandUtils;

// Only use when Backhand is installed (Waystones.hasBackhand)
public class BackhandCompat {

    public static ItemStack getOffhandItem(EntityPlayer player) {
        return BackhandUtils.getOffhandItem(player);
    }
}
