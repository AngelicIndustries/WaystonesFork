package net.blay09.mods.waystones.compat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.blay09.mods.waystones.PlayerWaystoneData;
import net.blay09.mods.waystones.WaystoneManager;
import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.util.WaystoneEntry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.api.AtlasAPI;
import hunternif.mc.atlas.marker.Marker;
import hunternif.mc.atlas.marker.MarkersData;
import hunternif.mc.atlas.network.PacketDispatcher;
import hunternif.mc.atlas.network.client.MarkersPacket;

// Waystone markers on Antique Atlases. Only use when Antique Atlas is installed (Waystones.hasAntiqueAtlas).
// Markers are added through Antique Atlas' own data instead of AtlasAPI.putMarker, which takes the dimension from
// a world and would need the waystone's dimension loaded.
public class AntiqueAtlasMarkers {

    public static final String MARKER_TYPE = "waystone";
    private static final String CARRIED_ATLASES = "AntiqueAtlases";

    public static void registerMarkerTexture() {
        AtlasAPI.markers
            .setTexture(MARKER_TYPE, new ResourceLocation(Waystones.MODID, "textures/gui/markers/waystone.png"));
    }

    // The atlases a player may change: the ones they carry, and with antiqueAtlasRequireItem off, the ones they
    // carried before
    public static List<Integer> getAtlases(EntityPlayer player) {
        return getAtlases(player, !Waystones.getConfig().antiqueAtlasRequireItem);
    }

    private static List<Integer> getAtlases(EntityPlayer player, boolean withCarriedBefore) {
        Set<Integer> carried = new LinkedHashSet<>(AtlasAPI.getPlayerAtlases(player));
        NBTTagCompound tagCompound = PlayerWaystoneData.getOrCreateWaystonesTag(player);
        Set<Integer> carriedBefore = new LinkedHashSet<>();
        for (int atlasID : tagCompound.getIntArray(CARRIED_ATLASES)) {
            carriedBefore.add(atlasID);
        }
        if (carriedBefore.addAll(carried)) {
            int[] atlasIDs = new int[carriedBefore.size()];
            int i = 0;
            for (int atlasID : carriedBefore) {
                atlasIDs[i++] = atlasID;
            }
            tagCompound.setIntArray(CARRIED_ATLASES, atlasIDs);
        }
        return new ArrayList<>(withCarriedBefore ? carriedBefore : carried);
    }

    // The waystones marked on any of the player's atlases
    public static List<WaystoneEntry> getMarkedWaystones(EntityPlayer player, Collection<WaystoneEntry> waystones) {
        Set<String> marked = new HashSet<>();
        for (int atlasID : getAtlases(player)) {
            MarkersData data = getAtlasData(atlasID);
            for (int dimension : data.getVisitedDimensions()) {
                for (Marker marker : data.getMarkersInDimension(dimension)) {
                    if (MARKER_TYPE.equals(marker.getType())) {
                        marked.add(dimension + "/" + marker.getX() + "/" + marker.getZ());
                    }
                }
            }
        }
        List<WaystoneEntry> result = new ArrayList<>();
        for (WaystoneEntry waystone : waystones) {
            if (marked.contains(
                waystone.getDimensionId() + "/"
                    + waystone.getPos()
                        .getX()
                    + "/"
                    + waystone.getPos()
                        .getZ())) {
                result.add(waystone);
            }
        }
        return result;
    }

    public static void mark(EntityPlayer player, WaystoneEntry waystone) {
        for (int atlasID : getAtlases(player)) {
            putMarker(atlasID, getAtlasData(atlasID), waystone);
        }
    }

    public static void unmark(EntityPlayer player, WaystoneEntry waystone) {
        for (int atlasID : getAtlases(player)) {
            deleteMarkers(atlasID, findMarkers(getAtlasData(atlasID), waystone));
        }
    }

    // After a rename, on every atlas the player has had, whether or not they carry it
    public static void relabel(EntityPlayer player, WaystoneEntry waystone) {
        for (int atlasID : getAtlases(player, true)) {
            MarkersData data = getAtlasData(atlasID);
            if (!findMarkers(data, waystone).isEmpty()) {
                putMarker(atlasID, data, waystone);
            }
        }
    }

    public static void markGlobal(WaystoneEntry waystone) {
        if (Waystones.getConfig().antiqueAtlasGlobalMarkers) {
            putMarker(-1, AntiqueAtlasMod.globalMarkersData.getData(), waystone);
        }
    }

    public static void unmarkGlobal(WaystoneEntry waystone) {
        deleteMarkers(-1, findMarkers(AntiqueAtlasMod.globalMarkersData.getData(), waystone));
    }

    // Makes the global markers match the global waystones, or removes them all with antiqueAtlasGlobalMarkers off
    public static void syncGlobalMarkers() {
        MarkersData data = AntiqueAtlasMod.globalMarkersData.getData();
        Collection<WaystoneEntry> globalWaystones = Waystones.getConfig().antiqueAtlasGlobalMarkers
            ? WaystoneManager.getServerWaystones()
            : Collections.<WaystoneEntry>emptyList();
        List<Marker> stale = new ArrayList<>();
        for (int dimension : data.getVisitedDimensions()) {
            for (Marker marker : data.getMarkersInDimension(dimension)) {
                if (MARKER_TYPE.equals(marker.getType()) && !isMarkerOfAny(marker, globalWaystones)) {
                    stale.add(marker);
                }
            }
        }
        deleteMarkers(-1, stale);
        for (WaystoneEntry waystone : globalWaystones) {
            markGlobal(waystone);
        }
    }

    // atlasID -1 stands for the global markers, as in Antique Atlas
    private static void putMarker(int atlasID, MarkersData data, WaystoneEntry waystone) {
        List<Marker> markers = findMarkers(data, waystone);
        if (markers.size() == 1 && markers.get(0)
            .getLabel()
            .equals(waystone.getName())) {
            return;
        }
        deleteMarkers(atlasID, markers);
        int dimension = waystone.getDimensionId();
        Marker marker = data.createAndSaveMarker(
            MARKER_TYPE,
            waystone.getName(),
            dimension,
            waystone.getPos()
                .getX(),
            waystone.getPos()
                .getZ(),
            true);
        PacketDispatcher.sendToAll(
            atlasID == -1 ? new MarkersPacket(dimension, marker) : new MarkersPacket(atlasID, dimension, marker));
    }

    private static void deleteMarkers(int atlasID, List<Marker> markers) {
        World world = MinecraftServer.getServer()
            .getEntityWorld();
        for (Marker marker : markers) {
            if (atlasID == -1) {
                AtlasAPI.markers.deleteGlobalMarker(world, marker.getId());
            } else {
                AtlasAPI.markers.deleteMarker(world, atlasID, marker.getId());
            }
        }
    }

    private static List<Marker> findMarkers(MarkersData data, WaystoneEntry waystone) {
        List<Marker> markers = new ArrayList<>();
        for (Marker marker : data.getMarkersInDimension(waystone.getDimensionId())) {
            if (isMarkerOf(marker, waystone)) {
                markers.add(marker);
            }
        }
        return markers;
    }

    private static boolean isMarkerOfAny(Marker marker, Collection<WaystoneEntry> waystones) {
        for (WaystoneEntry waystone : waystones) {
            if (isMarkerOf(marker, waystone) && marker.getLabel()
                .equals(waystone.getName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMarkerOf(Marker marker, WaystoneEntry waystone) {
        return MARKER_TYPE.equals(marker.getType()) && marker.getDimension() == waystone.getDimensionId()
            && marker.getX() == waystone.getPos()
                .getX()
            && marker.getZ() == waystone.getPos()
                .getZ();
    }

    private static MarkersData getAtlasData(int atlasID) {
        return AntiqueAtlasMod.markersData.getMarkersData(
            atlasID,
            MinecraftServer.getServer()
                .getEntityWorld());
    }
}
