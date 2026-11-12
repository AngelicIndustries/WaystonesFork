package net.blay09.mods.waystones.util;

import java.util.HashMap;

import net.minecraft.client.resources.I18n;

public class DimensionUtil {

    private static HashMap<Integer, String> idToNameMap = new HashMap<>();

    public static void setEntry(int id, String name) {
        idToNameMap.put(id, name);
    }

    // TODO: move to AngelLib
    public static String idToName(int id) {
        if (idToNameMap.containsKey(id)) {
            return idToNameMap.get(id);
        }
        return "Dimension ID " + id;
    }

    public static String localizeDimName(String name) {
        String key = "dim.waystones:" + name.replaceAll(" ", "");
        String localized = I18n.format(key);
        // Dimensions without a (filled in) lang entry show the provider's own name instead of the key
        return localized.isEmpty() || localized.equals(key) ? name : localized;
    }
}
