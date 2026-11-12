package net.blay09.mods.waystones.block;

import net.blay09.mods.waystones.Waystones;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.EnumSkyBlock;

public class TileWaystone extends TileEntity {

    public static final int VARIANT_STONE = 0;
    public static final int VARIANT_SANDSTONE = 1;
    public static final int VARIANT_MOSSY = 2;
    public static final int VARIANT_STONEBRICK = 3;
    public static final int VARIANT_NETHER = 4;
    public static final int VARIANT_END = 5;
    public static final int VARIANT_MOSSY_STONEBRICK = 6;

    private String waystoneName = "";
    private String waystoneOwner = "";
    private int dummyInventoryVariant = VARIANT_STONE;
    private boolean forceGlobalOnActivation;
    private boolean villageWaystone;
    private static int warpGeneration = 0;
    private transient int lastSeenWarpGeneration = 0;
    private transient int lastLightValue = -1;
    private transient int lightUpdateTimer = 0;

    public static void notifyWarpOccurred() {
        warpGeneration++;
    }

    @Override
    public void updateEntity() {
        if (!worldObj.isRemote) return;

        if (warpGeneration != lastSeenWarpGeneration) {
            lastSeenWarpGeneration = warpGeneration;
            lightUpdateTimer = 0;
        }

        if (--lightUpdateTimer > 0) return;

        int maxLight = (int) (Waystones.getConfig().waystoneLightLevel * 15f);
        int currentLight = getBlockType().getLightValue(worldObj, xCoord, yCoord, zCoord);

        if (currentLight != lastLightValue) {
            lastLightValue = currentLight;
            worldObj.updateLightByType(EnumSkyBlock.Block, xCoord, yCoord, zCoord);
            worldObj.updateLightByType(EnumSkyBlock.Block, xCoord, yCoord + 1, zCoord);
        }

        // Fast ticks while charging, stop when full (warp notification restarts)
        lightUpdateTimer = (currentLight >= maxLight) ? Integer.MAX_VALUE : 4;
    }

    @Override
    public void writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
        tagCompound.setString("WaystoneName", waystoneName);
        tagCompound.setString("WaystoneOwner", waystoneOwner);
        tagCompound.setBoolean("ForceGlobalOnActivation", forceGlobalOnActivation);
        tagCompound.setBoolean("VillageWaystone", villageWaystone);
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
        waystoneName = tagCompound.getString("WaystoneName");
        waystoneOwner = tagCompound.getString("WaystoneOwner");
        forceGlobalOnActivation = tagCompound.getBoolean("ForceGlobalOnActivation");
        villageWaystone = tagCompound.getBoolean("VillageWaystone");
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        super.onDataPacket(net, pkt);
        readFromNBT(pkt.func_148857_g());
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tagCompound = new NBTTagCompound();
        writeToNBT(tagCompound);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tagCompound);
    }

    public String getWaystoneName() {
        return waystoneName;
    }

    public void setWaystoneName(String waystoneName) {
        this.waystoneName = waystoneName;
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        markDirty();
    }

    public String getWaystoneOwner() {
        return waystoneOwner;
    }

    public void setWaystoneOwner(String waystoneOwner) {
        this.waystoneOwner = waystoneOwner;
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        markDirty();
    }

    public int getVariant() {
        if (worldObj != null) {
            Block block = worldObj.getBlock(xCoord, yCoord, zCoord);
            if (block instanceof BlockWaystone) {
                return ((BlockWaystone) block).getDefaultVariant();
            }
        }
        return dummyInventoryVariant;
    }

    public void setDummyInventoryVariant(int variant) {
        dummyInventoryVariant = normalizeVariant(variant);
    }

    private static int normalizeVariant(int variant) {
        switch (variant) {
            case VARIANT_SANDSTONE:
            case VARIANT_MOSSY:
            case VARIANT_STONEBRICK:
            case VARIANT_NETHER:
            case VARIANT_END:
            case VARIANT_MOSSY_STONEBRICK:
                return variant;
            default:
                return VARIANT_STONE;
        }
    }

    public boolean shouldForceGlobalOnActivation() {
        return forceGlobalOnActivation;
    }

    public void setForceGlobalOnActivation(boolean forceGlobalOnActivation) {
        this.forceGlobalOnActivation = forceGlobalOnActivation;
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
        markDirty();
    }

    public boolean isVillageWaystone() {
        return villageWaystone;
    }

    public void setVillageWaystone(boolean villageWaystone) {
        this.villageWaystone = villageWaystone;
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
        markDirty();
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return AxisAlignedBB.getBoundingBox(xCoord, yCoord, zCoord, xCoord + 1, yCoord + 2, zCoord + 1);
    }

}
