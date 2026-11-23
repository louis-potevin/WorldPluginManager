package fr.phundir.wplmanager.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Builds the items shown in the menus, on every server version.
 * <p>
 * Material names changed in 1.13 ({@code LIME_WOOL} used to be {@code WOOL} with data value 5):
 * each icon lists its modern name first, then its pre-1.13 name and data value.
 */
public final class MenuItems {

    public enum Icon {
        OVERWORLD(0, "GRASS_BLOCK", "GRASS"),
        NETHER(0, "NETHERRACK"),
        THE_END(0, "END_STONE", "ENDER_STONE"),
        OTHER_WORLD(0, "STONE"),
        ENABLED(5, "LIME_WOOL", "WOOL"),
        PARTIAL(4, "YELLOW_WOOL", "WOOL"),
        DISABLED(14, "RED_WOOL", "WOOL"),
        LOCKED(0, "BARRIER", "BEDROCK"),
        BACK(0, "ARROW"),
        PREVIOUS(0, "ARROW"),
        NEXT(0, "ARROW"),
        SWITCH_VIEW(0, "BOOK"),
        ENABLE_ALL(0, "EMERALD"),
        FILLER(7, "GRAY_STAINED_GLASS_PANE", "STAINED_GLASS_PANE");

        /** Data value used with the pre-1.13 names (colors of wool, glass...). */
        private final short legacyData;
        /** Modern name first, then fallbacks for older versions. */
        private final String[] names;
        private Material material;
        private boolean legacy;

        Icon(int legacyData, String... names) {
            this.legacyData = (short) legacyData;
            this.names = names;
        }

        private synchronized void resolve() {
            if (material != null) {
                return;
            }
            for (int i = 0; i < names.length; i++) {
                Material found = Material.getMaterial(names[i]);
                if (found != null) {
                    material = found;
                    // Only the fallback names need a data value; the modern ones carry the color.
                    legacy = i > 0;
                    return;
                }
            }
            material = Material.STONE;
        }
    }

    private MenuItems() {
    }

    public static ItemStack create(Icon icon, String name, List<String> lore) {
        icon.resolve();
        ItemStack item = icon.legacy && icon.legacyData != 0
                ? legacyItem(icon.material, icon.legacyData)
                : new ItemStack(icon.material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Only called before 1.13, where colored items are a material plus a data value. */
    @SuppressWarnings("deprecation")
    private static ItemStack legacyItem(Material material, short data) {
        return new ItemStack(material, 1, data);
    }
}
