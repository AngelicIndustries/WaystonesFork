package net.blay09.mods.waystones.compat;

import java.util.Set;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import industries.angelic.biomic.api.BiomicBiomeApi;

// Biomic's biomes depend on height (caves, sky islands), a column only reports its surface biome.
// Only use when Biomic is installed (Waystones.hasBiomic).
public class BiomicCompat {

    public static BiomeGenBase getBiome(World world, int x, int y, int z) {
        return BiomicBiomeApi.getBiome(world, x, y, z);
    }

    public static Set<String> getBiomeTags(World world, int x, int y, int z) {
        return BiomicBiomeApi.getBiomeTags(world, x, y, z);
    }
}
