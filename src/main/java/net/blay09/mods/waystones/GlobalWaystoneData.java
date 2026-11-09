package net.blay09.mods.waystones;

import java.io.File;

import net.blay09.mods.waystones.util.WaystoneEntry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraftforge.common.util.Constants;

public class GlobalWaystoneData extends WorldSavedData {

    private static final String DATA_NAME = "waystones_global";

    private boolean readFromFile;

    public GlobalWaystoneData() {
        super(DATA_NAME);
    }

    public GlobalWaystoneData(String name) {
        super(name);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        NBTTagList list = nbt.getTagList("GlobalWaystones", Constants.NBT.TAG_COMPOUND);
        WaystoneEntry[] entries = new WaystoneEntry[list.tagCount()];
        for (int i = 0; i < entries.length; i++) {
            entries[i] = WaystoneEntry.read(list.getCompoundTagAt(i));
            entries[i].setGlobal(true);
        }
        WaystoneManager.setServerWaystones(entries);
        readFromFile = true;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (WaystoneEntry entry : WaystoneManager.getServerWaystones()) {
            list.appendTag(entry.writeToNBT());
        }
        nbt.setTag("GlobalWaystones", list);
    }

    public static GlobalWaystoneData get(World world) {
        GlobalWaystoneData data = (GlobalWaystoneData) world.mapStorage.loadData(GlobalWaystoneData.class, DATA_NAME);
        if (data == null) {
            data = new GlobalWaystoneData();
            world.mapStorage.setData(DATA_NAME, data);
        }
        return data;
    }

    public static void load(World world) {
        GlobalWaystoneData data = get(world);
        File file = world.getSaveHandler()
            .getMapFileFromName(DATA_NAME);
        if (data.readFromFile) {
            Waystones.LOG.info(
                "Loaded {} global waystones from {}",
                WaystoneManager.getServerWaystones()
                    .size(),
                file);
        } else if (file != null && file.exists()) {
            // MapStorage swallows read errors and hands back empty data, which overwrites the file on the next save
            Waystones.LOG.error(
                "Could not read {} (see the stack trace above). Global waystones are now empty and the file will be "
                    + "overwritten on the next waystone change. Stop the server and restore it from a backup.",
                file);
        } else {
            Waystones.LOG.info("No global waystone data found, starting with no global waystones");
        }
    }

    public static void save(World world) {
        GlobalWaystoneData data = get(world);
        data.markDirty();
    }
}
