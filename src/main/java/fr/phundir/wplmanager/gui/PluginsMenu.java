package fr.phundir.wplmanager.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * First screen of the "by plugin" view: every plugin, green when enabled in every world,
 * yellow when enabled in some, red when disabled everywhere. Clicking a plugin opens
 * {@link PluginWorldsMenu}.
 */
public final class PluginsMenu extends Menu {

    public PluginsMenu(MenuManager menus, Player viewer) {
        super(menus, viewer);
    }

    @Override
    protected String title() {
        return messages.get("gui_title_plugins");
    }

    @Override
    protected void draw() {
        List<Plugin> plugins = new ArrayList<>(Arrays.asList(Bukkit.getPluginManager().getPlugins()));
        plugins.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        drawEntries(plugins, this::icon, (target, shift) -> {
            if (target != plugin) {
                menus.open(viewer, new PluginWorldsMenu(menus, viewer, target.getName(), this));
            }
        });
        set(SLOT_SWITCH, item(MenuItems.Icon.SWITCH_VIEW, "gui_view_worlds", null),
                shift -> menus.open(viewer, new WorldsMenu(menus, viewer)));
    }

    private ItemStack icon(Plugin target) {
        String name = target.getName();
        if (target == plugin) {
            return item(MenuItems.Icon.LOCKED, "gui_plugin_locked_name", "gui_plugin_locked_lore", "plugin", name);
        }
        int enabled = enabledWorlds(name);
        int worlds = Bukkit.getWorlds().size();
        MenuItems.Icon icon = enabled == worlds ? MenuItems.Icon.ENABLED
                : enabled == 0 ? MenuItems.Icon.DISABLED
                : MenuItems.Icon.PARTIAL;
        return item(icon, "gui_plugin_name", "gui_plugin_lore",
                "plugin", name, "enabled", String.valueOf(enabled), "worlds", String.valueOf(worlds));
    }
}
