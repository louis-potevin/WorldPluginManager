package fr.phundir.wplmanager.gui;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Worlds of one plugin: green where it is enabled, red where it is disabled.
 * Click toggles the plugin in that world, shift-click keeps it only in that world (focus).
 */
public final class PluginWorldsMenu extends Menu {
    private final String pluginName;
    private final Menu previous;

    public PluginWorldsMenu(MenuManager menus, Player viewer, String pluginName, Menu previous) {
        super(menus, viewer);
        this.pluginName = pluginName;
        this.previous = previous;
    }

    @Override
    protected String title() {
        return messages.get("gui_title_plugin", "plugin", pluginName);
    }

    @Override
    protected void draw() {
        List<World> worlds = new ArrayList<>(Bukkit.getWorlds());
        drawEntries(worlds, this::icon, this::onClick);

        drawBack(previous);
        set(SLOT_ACTION, item(MenuItems.Icon.ENABLE_ALL, "gui_enable_everywhere", "gui_enable_everywhere_lore",
                "plugin", pluginName), shift -> enableEverywhere());
    }

    private ItemStack icon(World world) {
        boolean enabled = statusStore.isEnabled(pluginName, world.getName());
        return item(enabled ? MenuItems.Icon.ENABLED : MenuItems.Icon.DISABLED,
                enabled ? "gui_world_enabled_name" : "gui_world_disabled_name", "gui_plugin_world_entry_lore",
                "world", world.getName(), "plugin", pluginName);
    }

    private void onClick(World target, boolean shift) {
        if (shift) {
            if (!allowed("worldpluginmanager.focus")) {
                return;
            }
            for (World world : Bukkit.getWorlds()) {
                statusStore.setEnabled(pluginName, world.getName(), world.getName().equals(target.getName()));
            }
        } else {
            boolean enable = !statusStore.isEnabled(pluginName, target.getName());
            if (!allowed(enable ? "worldpluginmanager.enpl" : "worldpluginmanager.displ")) {
                return;
            }
            statusStore.setEnabled(pluginName, target.getName(), enable);
        }
        plugin.onStatusesChanged();
        refresh();
    }

    /** Removes the plugin's restrictions in every world. */
    private void enableEverywhere() {
        if (!allowed("worldpluginmanager.reset")) {
            return;
        }
        if (statusStore.resetEverywhere(pluginName)) {
            plugin.onStatusesChanged();
        }
        refresh();
    }
}
