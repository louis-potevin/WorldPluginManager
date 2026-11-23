package fr.phundir.wplmanager.gui;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Plugins of one world: green when enabled, red when disabled.
 * Click toggles the plugin in this world, shift-click keeps it only in this world (focus).
 */
public final class WorldPluginsMenu extends Menu {
    private final String worldName;
    private final Menu previous;

    public WorldPluginsMenu(MenuManager menus, Player viewer, String worldName, Menu previous) {
        super(menus, viewer);
        this.worldName = worldName;
        this.previous = previous;
    }

    @Override
    protected String title() {
        return messages.get("gui_title_world", "world", worldName);
    }

    @Override
    protected void draw() {
        List<Plugin> plugins = new ArrayList<>(Arrays.asList(Bukkit.getPluginManager().getPlugins()));
        plugins.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        drawEntries(plugins, this::icon, this::onClick);

        drawBack(previous);
        set(SLOT_ACTION, item(MenuItems.Icon.ENABLE_ALL, "gui_enable_all_world", "gui_enable_all_world_lore",
                "world", worldName), shift -> enableAll());
    }

    private ItemStack icon(Plugin target) {
        String name = target.getName();
        if (target == plugin) {
            return item(MenuItems.Icon.LOCKED, "gui_plugin_locked_name", "gui_plugin_locked_lore", "plugin", name);
        }
        boolean enabled = statusStore.isEnabled(name, worldName);
        return item(enabled ? MenuItems.Icon.ENABLED : MenuItems.Icon.DISABLED,
                enabled ? "gui_plugin_enabled_name" : "gui_plugin_disabled_name", "gui_plugin_world_lore",
                "plugin", name, "enabled", String.valueOf(enabledWorlds(name)),
                "worlds", String.valueOf(Bukkit.getWorlds().size()));
    }

    private void onClick(Plugin target, boolean shift) {
        if (target == plugin) {
            return;
        }
        String name = target.getName();
        if (shift) {
            if (!allowed("worldpluginmanager.focus")) {
                return;
            }
            for (World world : Bukkit.getWorlds()) {
                statusStore.setEnabled(name, world.getName(), world.getName().equals(worldName));
            }
        } else {
            boolean enable = !statusStore.isEnabled(name, worldName);
            if (!allowed(enable ? "worldpluginmanager.enpl" : "worldpluginmanager.displ")) {
                return;
            }
            statusStore.setEnabled(name, worldName, enable);
        }
        plugin.onStatusesChanged();
        refresh();
    }

    /** Removes every restriction in this world. */
    private void enableAll() {
        if (!allowed("worldpluginmanager.enpl")) {
            return;
        }
        for (Map.Entry<String, Boolean> entry : new ArrayList<>(statusStore.getStatuses(worldName).entrySet())) {
            statusStore.reset(entry.getKey(), worldName);
        }
        plugin.onStatusesChanged();
        refresh();
    }

    private int enabledWorlds(String pluginName) {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            if (statusStore.isEnabled(pluginName, world.getName())) {
                count++;
            }
        }
        return count;
    }
}
