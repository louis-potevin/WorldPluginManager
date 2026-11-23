package fr.phundir.wplmanager.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Plugin lookup helpers.
 */
public final class Plugins {

    private Plugins() {
    }

    /**
     * Finds a loaded plugin by name, ignoring case
     * ({@code worldedit} finds {@code WorldEdit}).
     *
     * @return the plugin, or {@code null} if none matches
     */
    public static Plugin findByName(String name) {
        Plugin exact = Bukkit.getPluginManager().getPlugin(name);
        if (exact != null) {
            return exact;
        }
        for (Plugin candidate : Bukkit.getPluginManager().getPlugins()) {
            if (candidate.getName().equalsIgnoreCase(name)) {
                return candidate;
            }
        }
        return null;
    }
}
