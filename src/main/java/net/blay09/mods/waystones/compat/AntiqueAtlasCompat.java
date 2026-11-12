package net.blay09.mods.waystones.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.common.Loader;

public class AntiqueAtlasCompat {

    public static final int ICON_SIZE = 16;

    private static final ResourceLocation VILLAGE_MARKER = new ResourceLocation(
        "antiqueatlas",
        "textures/gui/markers/village.png");

    public static boolean isVillageIconAvailable() {
        return Loader.isModLoaded("antiqueatlas");
    }

    public static void drawVillageIcon(int x, int y) {
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(VILLAGE_MARKER);
        // The marker is a 16x16 icon in the middle of a 32x32 texture
        Gui.func_152125_a(x, y, 8, 8, 16, 16, ICON_SIZE, ICON_SIZE, 32, 32);
    }
}
