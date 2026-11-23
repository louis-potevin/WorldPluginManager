package fr.phundir.wplmanager.gui;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * First screen of the "by world" view: one icon per world, with the number of plugins
 * disabled there. Clicking a world opens {@link WorldPluginsMenu}.
 */
public final class WorldsMenu extends Menu {

    public WorldsMenu(MenuManager menus, Player viewer) {
        super(menus, viewer);
    }

    @Override
    protected String title() {
        return messages.get("gui_title_worlds");
    }

    @Override
    protected void draw() {
        List<World> worlds = new ArrayList<>(Bukkit.getWorlds());
        drawEntries(worlds,
                world -> item(iconOf(world), "gui_world_name", "gui_world_lore",
                        "world", world.getName(), "disabled", String.valueOf(disabledCount(world))),
                (world, shift) -> menus.open(viewer, new WorldPluginsMenu(menus, viewer, world.getName(), this)));
        set(SLOT_SWITCH, item(MenuItems.Icon.SWITCH_VIEW, "gui_view_plugins", null),
                shift -> menus.open(viewer, new PluginsMenu(menus, viewer)));
    }

    /** Compared by name: the Environment constants differ between versions. */
    static MenuItems.Icon iconOf(World world) {
        switch (world.getEnvironment().name()) {
            case "NORMAL":
                return MenuItems.Icon.OVERWORLD;
            case "NETHER":
                return MenuItems.Icon.NETHER;
            case "THE_END":
                return MenuItems.Icon.THE_END;
            default:
                return MenuItems.Icon.OTHER_WORLD;
        }
    }

    private int disabledCount(World world) {
        int count = 0;
        for (Plugin installed : Bukkit.getPluginManager().getPlugins()) {
            if (!statusStore.isEnabled(installed.getName(), world.getName())) {
                count++;
            }
        }
        return count;
    }
}
