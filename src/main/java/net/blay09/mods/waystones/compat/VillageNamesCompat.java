package net.blay09.mods.waystones.compat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Random;

import net.blay09.mods.waystones.Waystones;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import astrotibs.villagenames.banner.BannerGenerator;
import astrotibs.villagenames.name.NameGenerator;
import astrotibs.villagenames.nbt.VNWorldDataStructure;
import astrotibs.villagenames.utility.FunctionsVN;

public class VillageNamesCompat {

    public static String ensureVillageName(World world, int ax, int ay, int az) {

        VNWorldDataStructure data = VNWorldDataStructure.forWorld(world, "villagenames3_Village", "NamedStructures");

        NBTTagCompound root = data.getData();

        // 1) Look for an existing VN village entry NEAR this point
        for (Object key : root.func_150296_c()) {
            NBTTagCompound entry = root.getTagList(key.toString(), 10)
                .getCompoundTagAt(0);

            int x = entry.getInteger("signX");
            int y = entry.getInteger("signY");
            int z = entry.getInteger("signZ");

            double dx = x - ax;
            double dz = z - az;

            // VN uses <= 10000 (100 block radius squared)
            if (dx * dx + dz * dz <= 10000) {
                return (entry.getString("namePrefix") + " "
                    + entry.getString("nameRoot")
                    + " "
                    + entry.getString("nameSuffix")).trim();
            }
        }

        // 2) Generate deterministic VN name
        Random rand = new Random();
        rand.setSeed(world.getSeed() + FunctionsVN.getUniqueLongForXYZ(ax, ay, az));

        String[] name = NameGenerator.newRandomName("Village", rand);

        int[] townColors = getTownColors(world, rand, ax, az);
        int townColor = townColors[0];
        int townColor2 = townColors[1];

        // 3) Store VN entry
        NBTTagList list = new NBTTagList();
        NBTTagCompound tag = new NBTTagCompound();

        tag.setInteger("signX", ax);
        tag.setInteger("signY", ay);
        tag.setInteger("signZ", az);
        tag.setInteger("townColor", townColor);
        tag.setInteger("townColor2", townColor2);
        tag.setString("namePrefix", name[1]);
        tag.setString("nameRoot", name[2]);
        tag.setString("nameSuffix", name[3]);
        tag.setBoolean("fromEntity", true);

        list.appendTag(tag);

        String key = (name[1] + " " + name[2] + " " + name[3]).trim() + ", x" + ax + " y" + ay + " z" + az;

        root.setTag(key, list);
        data.markDirty();

        return (name[1] + " " + name[2] + " " + name[3]).trim();
    }

    // The village's banner colors. Village Names 5.0 picks them by village type, 4.x (GTNH's 4.6 too) without one.
    private static int[] getTownColors(World world, Random rand, int x, int z) {
        try {
            for (Method method : BannerGenerator.class.getMethods()) {
                if (!method.getName()
                    .equals("randomBannerArrays")) {
                    continue;
                }
                Object[] banner;
                if (method.getParameterTypes().length == 4) {
                    banner = (Object[]) method.invoke(null, rand, getVillageType(world, x, z), -1, -1);
                } else if (method.getParameterTypes().length == 3) {
                    banner = (Object[]) method.invoke(null, rand, -1, -1);
                } else {
                    continue;
                }
                @SuppressWarnings("unchecked")
                ArrayList<Integer> colors = (ArrayList<Integer>) banner[1];
                int townColor = 15 - colors.get(0);
                return new int[] { townColor, colors.size() > 1 ? 15 - colors.get(1) : townColor };
            }
            Waystones.LOG.warn("No known Village Names banner generator, using random village colors");
        } catch (ReflectiveOperationException | RuntimeException e) {
            Waystones.LOG.warn("Village Names banner generator failed, using random village colors", e);
        }
        return new int[] { rand.nextInt(16), rand.nextInt(16) };
    }

    // As Village Names 5.0 does for its own villages; only called with 5.0
    private static Object getVillageType(World world, int x, int z) {
        return FunctionsVN.VillageType.changeVillageTypeToNullIfDefaultedPlains(
            FunctionsVN.VillageType.getVillageTypeFromBiome(world, x, z),
            world,
            x,
            z);
    }
}
