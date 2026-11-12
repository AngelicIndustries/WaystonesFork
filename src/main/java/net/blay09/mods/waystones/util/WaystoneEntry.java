package net.blay09.mods.waystones.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import net.blay09.mods.waystones.PlayerWaystoneData;
import net.blay09.mods.waystones.WaystoneConfig;
import net.blay09.mods.waystones.WaystoneManager;
import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.block.TileWaystone;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

public class WaystoneEntry {

    private final String name;
    private final int dimensionId;
    private final BlockPos pos;
    private boolean isGlobal;
    private boolean isVillage;

    public WaystoneEntry(String name, int dimensionId, BlockPos pos) {
        this.name = name;
        this.dimensionId = dimensionId;
        this.pos = pos;
    }

    public WaystoneEntry(TileWaystone tileWaystone) {
        this.name = tileWaystone.getWaystoneName();
        this.dimensionId = tileWaystone.getWorldObj().provider.dimensionId;
        this.pos = new BlockPos(tileWaystone);
        this.isVillage = tileWaystone.isVillageWaystone();
    }

    public String getName() {
        return name;
    }

    public int getDimensionId() {
        return dimensionId;
    }

    public BlockPos getPos() {
        return pos;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    public void setGlobal(boolean isGlobal) {
        this.isGlobal = isGlobal;
    }

    public boolean isVillage() {
        return isVillage;
    }

    public void setVillage(boolean isVillage) {
        this.isVillage = isVillage;
    }

    public static WaystoneEntry read(ByteBuf buf) {
        WaystoneEntry entry = new WaystoneEntry(
            ByteBufUtils.readUTF8String(buf),
            buf.readInt(),
            BlockPos.fromLong(buf.readLong()));
        entry.setVillage(buf.readBoolean());
        return entry;
    }

    public static WaystoneEntry read(NBTTagCompound tagCompound) {
        WaystoneEntry entry = new WaystoneEntry(
            tagCompound.getString("Name"),
            tagCompound.getInteger("Dimension"),
            BlockPos.fromLong(tagCompound.getLong("Position")));
        entry.setVillage(tagCompound.getBoolean("Village"));
        return entry;
    }

    public void write(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, name);
        buf.writeInt(dimensionId);
        buf.writeLong(pos.toLong());
        buf.writeBoolean(isVillage);
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tagCompound = new NBTTagCompound();
        tagCompound.setString("Name", name);
        tagCompound.setInteger("Dimension", dimensionId);
        tagCompound.setLong("Position", pos.toLong());
        tagCompound.setBoolean("Village", isVillage);
        return tagCompound;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WaystoneEntry that = (WaystoneEntry) o;
        return dimensionId == that.dimensionId && pos.equals(that.pos);
    }

    @Override
    public int hashCode() {
        int result = dimensionId;
        result = 31 * result + pos.hashCode();
        return result;
    }

    public static boolean tileAndEntryShareCoords(WaystoneEntry entry, TileWaystone tile) {
        if (entry == null || tile == null) {
            return false;
        }
        return entry.getPos()
            .getX() == tile.xCoord
            && entry.getPos()
                .getY() == tile.yCoord
            && entry.getPos()
                .getZ() == tile.zCoord;
    }

    public static WaystoneEntry[] getCombinedWaystones_(EntityPlayer player) {
        List<WaystoneEntry> allWaystones = new ArrayList<>();
        allWaystones.addAll(
            Arrays.asList(
                PlayerWaystoneData.fromPlayer(player)
                    .getWaystones()));
        allWaystones.addAll(WaystoneManager.getServerWaystones());

        List<String> pinnedNames = PlayerWaystoneData.getPinnedWaystoneNames(player);

        List<WaystoneEntry> pinned = new ArrayList<>();
        List<WaystoneEntry> unpinned = new ArrayList<>();

        for (WaystoneEntry entry : allWaystones) {
            if (pinnedNames.contains(entry.getName())) {
                pinned.add(entry);
            } else {
                unpinned.add(entry);
            }
        }

        List<WaystoneEntry> combined = new ArrayList<>();
        for (String pinnedName : pinnedNames) {
            for (WaystoneEntry entry : pinned) {
                if (entry.getName()
                    .equals(pinnedName)) {
                    combined.add(entry);
                    break;
                }
            }
        }

        combined.addAll(unpinned);

        return combined.toArray(new WaystoneEntry[0]);
    }

    public static WaystoneEntry[] getCombinedWaystones__(EntityPlayer player) {
        List<WaystoneEntry> allWaystones = new ArrayList<>();
        allWaystones.addAll(
            Arrays.asList(
                PlayerWaystoneData.fromPlayer(player)
                    .getWaystones()));
        allWaystones.addAll(WaystoneManager.getServerWaystones());

        List<String> pinnedNames = PlayerWaystoneData.getPinnedWaystoneNames(player);

        List<WaystoneEntry> pinned = new ArrayList<>();
        List<WaystoneEntry> unpinned = new ArrayList<>();

        for (WaystoneEntry entry : allWaystones) {
            if (pinnedNames.contains(entry.getName())) {
                pinned.add(entry);
            } else {
                unpinned.add(entry);
            }
        }

        // Sort based on sorting mode
        Comparator<WaystoneEntry> comparator;
        if (WaystoneConfig.sortingMode == 1) {
            // Distance sorting
            comparator = (a, b) -> {
                double distA = player.getDistanceSq(
                    a.getPos()
                        .getX(),
                    a.getPos()
                        .getY(),
                    a.getPos()
                        .getZ());
                double distB = player.getDistanceSq(
                    b.getPos()
                        .getX(),
                    b.getPos()
                        .getY(),
                    b.getPos()
                        .getZ());
                return Double.compare(distA, distB);
            };
        } else {
            // Alphabetical sorting
            comparator = (a, b) -> a.getName()
                .compareToIgnoreCase(b.getName());
        }

        // pinned.sort(comparator); Those shouldn't be sorted
        unpinned.sort(comparator);

        List<WaystoneEntry> combined = new ArrayList<>();
        combined.addAll(pinned);
        combined.addAll(unpinned);

        return combined.toArray(new WaystoneEntry[0]);
    }

    public static WaystoneEntry[] getCombinedWaystones(EntityPlayer player, boolean honorInterDim) {
        List<WaystoneEntry> allWaystones = new ArrayList<>();
        allWaystones.addAll(
            Arrays.asList(
                PlayerWaystoneData.fromPlayer(player)
                    .getWaystones()));
        allWaystones.addAll(WaystoneManager.getServerWaystones());

        // Filter by inter-dimension rules if requested
        if (honorInterDim && !player.capabilities.isCreativeMode) {
            int playerDim = player.worldObj.provider.dimensionId;
            List<WaystoneEntry> filtered = new ArrayList<>();
            for (WaystoneEntry entry : allWaystones) {
                if (entry.getDimensionId() == playerDim) {
                    // Same dimension, always include
                    filtered.add(entry);
                } else if (entry.isGlobal() && Waystones.getConfig().globalInterDimension) {
                    // Global waystone, check globalInterDimension
                    filtered.add(entry);
                } else if (!entry.isGlobal() && Waystones.getConfig().interDimension) {
                    // Normal waystone, check interDimension
                    filtered.add(entry);
                }
            }
            allWaystones = filtered;
        }

        List<String> pinnedNames = PlayerWaystoneData.getPinnedWaystoneNames(player);

        List<WaystoneEntry> pinned = new ArrayList<>();
        List<WaystoneEntry> unpinned = new ArrayList<>();

        for (WaystoneEntry entry : allWaystones) {
            if (pinnedNames.contains(entry.getName())) {
                pinned.add(entry);
            } else {
                unpinned.add(entry);
            }
        }

        // Sort unpinned based on sorting mode (pinned keep their manual order)
        Comparator<WaystoneEntry> comparator;
        if (WaystoneConfig.sortingMode == 1) {
            // Distance sorting
            comparator = (a, b) -> {
                double distA = player.getDistanceSq(
                    a.getPos()
                        .getX(),
                    a.getPos()
                        .getY(),
                    a.getPos()
                        .getZ());
                double distB = player.getDistanceSq(
                    b.getPos()
                        .getX(),
                    b.getPos()
                        .getY(),
                    b.getPos()
                        .getZ());
                return Double.compare(distA, distB);
            };
        } else {
            // Alphabetical sorting (default)
            comparator = (a, b) -> a.getName()
                .compareToIgnoreCase(b.getName());
        }

        unpinned.sort(comparator);

        // Build combined list: pinned first (in pin order), then sorted unpinned
        List<WaystoneEntry> combined = new ArrayList<>();
        for (String pinnedName : pinnedNames) {
            for (WaystoneEntry entry : pinned) {
                if (entry.getName()
                    .equals(pinnedName)) {
                    combined.add(entry);
                    break;
                }
            }
        }
        combined.addAll(unpinned);

        return combined.toArray(new WaystoneEntry[0]);
    }
}
