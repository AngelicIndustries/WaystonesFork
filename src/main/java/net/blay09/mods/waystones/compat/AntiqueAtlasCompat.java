package net.blay09.mods.waystones.compat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.network.NetworkHandler;
import net.blay09.mods.waystones.network.message.MessageAtlasMarker;
import net.blay09.mods.waystones.network.message.MessageRequestWaystones;
import net.blay09.mods.waystones.util.WaystoneEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class AntiqueAtlasCompat {

    public static final int ICON_SIZE = 16;
    public static final int MARKER_ICON_WIDTH = 12;

    private static final ResourceLocation VILLAGE_MARKER = new ResourceLocation(
        "antiqueatlas",
        "textures/gui/markers/village.png");
    private static final ResourceLocation MARKER_ICON = new ResourceLocation(
        Waystones.MODID,
        "textures/gui/antique_atlas_marker.png");

    // Sent by the server along with the waystones
    private static final Set<WaystoneEntry> markedWaystones = new HashSet<>();
    private static boolean canMarkOnAtlas;

    public static boolean isVillageIconAvailable() {
        return Waystones.hasAntiqueAtlas;
    }

    public static void drawVillageIcon(int x, int y) {
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(VILLAGE_MARKER);
        // The marker is a 16x16 icon in the middle of a 32x32 texture
        Gui.func_152125_a(x, y, 8, 8, 16, 16, ICON_SIZE, ICON_SIZE, 32, 32);
    }

    public static void setAtlasState(WaystoneEntry[] marked, boolean canMark) {
        markedWaystones.clear();
        markedWaystones.addAll(Arrays.asList(marked));
        canMarkOnAtlas = canMark;
    }

    // The menu asks for fresh marker state, the player may have picked up or put away an atlas since the last sync
    public static void requestAtlasState() {
        if (Waystones.hasAntiqueAtlas) {
            NetworkHandler.channel.sendToServer(new MessageRequestWaystones());
        }
    }

    // Global waystones are on every atlas while antiqueAtlasGlobalMarkers is on
    public static boolean canToggleMarker(WaystoneEntry waystone) {
        return Waystones.hasAntiqueAtlas && !(waystone.isGlobal() && Waystones.getConfig().antiqueAtlasGlobalMarkers);
    }

    // False without an atlas to mark
    public static boolean toggleMarker(WaystoneEntry waystone) {
        if (canMarkOnAtlas) {
            NetworkHandler.channel.sendToServer(new MessageAtlasMarker(waystone, !markedWaystones.contains(waystone)));
        }
        return canMarkOnAtlas;
    }

    public static String getMarkerTooltip(WaystoneEntry waystone) {
        if (!canMarkOnAtlas) {
            return Waystones.getConfig().antiqueAtlasRequireItem ? "gui.waystones:atlasCarry"
                : "gui.waystones:atlasNone";
        }
        return markedWaystones.contains(waystone) ? "gui.waystones:atlasRemove" : "gui.waystones:atlasAdd";
    }

    // Full color when marked, dimmed when not, dark without an atlas
    public static void drawMarkerButton(WaystoneEntry waystone, int x, int y, boolean hovered) {
        float brightness = !canMarkOnAtlas ? 0.3f : markedWaystones.contains(waystone) ? 1f : hovered ? 0.8f : 0.5f;
        GL11.glColor4f(brightness, brightness, brightness, 1f);
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(MARKER_ICON);
        Gui.func_152125_a(
            x,
            y,
            0,
            0,
            MARKER_ICON_WIDTH,
            ICON_SIZE,
            MARKER_ICON_WIDTH,
            ICON_SIZE,
            MARKER_ICON_WIDTH,
            ICON_SIZE);
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }
}
