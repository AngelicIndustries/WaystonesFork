package net.blay09.mods.waystones.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import net.blay09.mods.waystones.WaystoneConfig;
import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.compat.BiomicCompat;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;

// Names for unnamed waystones, ported from upstream Waystones (MIT, BlayTheNinth). Syllables originally by MrPork.
public class NameGenerator {

    private static final String BIOMIC_TAG_PREFIX = "BIOMIC:";

    public static final String[] DEFAULT_BEGINNINGS = { "Kr", "Ca", "Ra", "Rei", "Mar", "Luk", "Cro", "Cru", "Ray",
        "Bre", "Zed", "Mor", "Jag", "Mer", "Jar", "Mad", "Cry", "Zur", "Mjol", "Zork", "Creo", "Azak", "Azur", "Mrok",
        "Drak" };

    public static final String[] DEFAULT_MIDDLES = { "ir", "mi", "air", "sor", "mee", "clo", "red", "cra", "ark", "arc",
        "mur", "zer", "miri", "lori", "cres", "zoir", "urak", "marac", "slamar", "salmar" };

    public static final String[] DEFAULT_ENDINGS = { "d", "ed", "es", "er", "ark", "arc", "der", "med", "ure", "zur",
        "mur", "tron", "cred" };

    // Biomic's caves and sky islands first, Forge has no types for them
    public static final String[] DEFAULT_BIOME_WORDS = { "biomic:CAVE=Caves", "biomic:FLOATING_ISLAND=Skies",
        "OCEAN=Ocean", "BEACH=Beach", "RIVER=River", "MUSHROOM=Fungi", "SWAMP=Swamps", "JUNGLE=Jungle", "MESA=Mesa",
        "SAVANNA=Savanna", "SANDY=Desert", "CONIFEROUS=Taiga", "SNOWY=Icelands", "MOUNTAIN=Hills", "FOREST=Forest",
        "HILLS=Hills", "PLAINS=Plains" };

    public static String generate(World world, int x, int y, int z, Random rand, Predicate<String> isTaken) {
        WaystoneConfig config = Waystones.getConfig();

        List<String> customNames = new ArrayList<>(Arrays.asList(config.customWaystoneNames));
        Collections.shuffle(customNames, rand);
        for (String customName : customNames) {
            String name = customName.trim();
            if (!name.isEmpty() && !isTaken.test(name)) {
                return name;
            }
        }

        BiomeGenBase biome = Waystones.hasBiomic ? BiomicCompat.getBiome(world, x, y, z)
            : world.getBiomeGenForCoords(x, z);
        Set<String> biomicTags = Waystones.hasBiomic ? BiomicCompat.getBiomeTags(world, x, y, z)
            : Collections.<String>emptySet();
        String name = pick(config.nameBeginnings, rand) + pick(config.nameMiddles, rand)
            + pick(config.nameEndings, rand);
        String biomeWord = getBiomeWord(config.nameBiomeWords, biome, biomicTags);
        if (biomeWord != null) {
            name = name + " " + biomeWord;
        }
        name = name.trim();
        if (name.isEmpty()) {
            name = "Waystone";
        }

        String uniqueName = name;
        for (int i = 2; isTaken.test(uniqueName); i++) {
            uniqueName = name + " " + toRoman(i);
        }
        return uniqueName;
    }

    private static String pick(String[] parts, Random rand) {
        return parts.length > 0 ? parts[rand.nextInt(parts.length)] : "";
    }

    private static String getBiomeWord(String[] biomeWords, BiomeGenBase biome, Set<String> biomicTags) {
        if (biome == null) {
            return null;
        }
        for (String entry : biomeWords) {
            int separator = entry.indexOf('=');
            String key = separator > 0 ? entry.substring(0, separator)
                .trim()
                .toUpperCase(Locale.ROOT) : "";
            // Without Biomic its tags never match, they aren't invalid
            if (key.startsWith(BIOMIC_TAG_PREFIX)) {
                if (biomicTags.contains(key.substring(BIOMIC_TAG_PREFIX.length()))) {
                    return getWord(entry, separator);
                }
                continue;
            }
            BiomeDictionary.Type type = null;
            try {
                type = BiomeDictionary.Type.valueOf(key);
            } catch (IllegalArgumentException ignored) {}
            if (type == null) {
                Waystones.LOG.warn(
                    "Invalid nameBiomeWords entry, expected <biome dictionary type>=<word> or biomic:<tag>=<word>: {}",
                    entry);
                continue;
            }
            if (BiomeDictionary.isBiomeOfType(biome, type)) {
                return getWord(entry, separator);
            }
        }
        return null;
    }

    // An entry without a word ends the search with no word
    private static String getWord(String entry, int separator) {
        String word = entry.substring(separator + 1)
            .trim();
        return word.isEmpty() ? null : word;
    }

    private static String toRoman(int number) {
        int[] values = { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
        String[] numerals = { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I" };
        StringBuilder roman = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (number >= values[i]) {
                roman.append(numerals[i]);
                number -= values[i];
            }
        }
        return roman.toString();
    }
}
