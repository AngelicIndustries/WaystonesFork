package net.blay09.mods.waystones.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Predicate;

import net.blay09.mods.waystones.WaystoneConfig;
import net.blay09.mods.waystones.Waystones;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;

// Names for unnamed waystones, ported from upstream Waystones (MIT, BlayTheNinth). Syllables originally by MrPork.
public class NameGenerator {

    public static final String[] DEFAULT_BEGINNINGS = { "Kr", "Ca", "Ra", "Rei", "Mar", "Luk", "Cro", "Cru", "Ray",
        "Bre", "Zed", "Mor", "Jag", "Mer", "Jar", "Mad", "Cry", "Zur", "Mjol", "Zork", "Creo", "Azak", "Azur", "Mrok",
        "Drak" };

    public static final String[] DEFAULT_MIDDLES = { "ir", "mi", "air", "sor", "mee", "clo", "red", "cra", "ark", "arc",
        "mur", "zer", "miri", "lori", "cres", "zoir", "urak", "marac", "slamar", "salmar" };

    public static final String[] DEFAULT_ENDINGS = { "d", "ed", "es", "er", "ark", "arc", "der", "med", "ure", "zur",
        "mur", "tron", "cred" };

    public static final String[] DEFAULT_BIOME_WORDS = { "OCEAN=Ocean", "BEACH=Beach", "RIVER=River", "MUSHROOM=Island",
        "SWAMP=Swamps", "JUNGLE=Jungle", "MESA=Mesa", "SAVANNA=Savanna", "SANDY=Desert", "CONIFEROUS=Taiga",
        "SNOWY=Icelands", "MOUNTAIN=Hills", "FOREST=Forest", "HILLS=Hills", "PLAINS=Plains" };

    public static String generate(World world, int x, int z, Random rand, Predicate<String> isTaken) {
        WaystoneConfig config = Waystones.getConfig();

        List<String> customNames = new ArrayList<>(Arrays.asList(config.customWaystoneNames));
        Collections.shuffle(customNames, rand);
        for (String customName : customNames) {
            String name = customName.trim();
            if (!name.isEmpty() && !isTaken.test(name)) {
                return name;
            }
        }

        String name = pick(config.nameBeginnings, rand) + pick(config.nameMiddles, rand)
            + pick(config.nameEndings, rand);
        String biomeWord = getBiomeWord(config.nameBiomeWords, world.getBiomeGenForCoords(x, z));
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

    private static String getBiomeWord(String[] biomeWords, BiomeGenBase biome) {
        if (biome == null) {
            return null;
        }
        for (String entry : biomeWords) {
            int separator = entry.indexOf('=');
            BiomeDictionary.Type type = null;
            if (separator > 0) {
                try {
                    type = BiomeDictionary.Type.valueOf(
                        entry.substring(0, separator)
                            .trim()
                            .toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {}
            }
            if (type == null) {
                Waystones.LOG.warn("Invalid nameBiomeWords entry, expected <biome dictionary type>=<word>: {}", entry);
                continue;
            }
            if (BiomeDictionary.isBiomeOfType(biome, type)) {
                String word = entry.substring(separator + 1)
                    .trim();
                return word.isEmpty() ? null : word;
            }
        }
        return null;
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
