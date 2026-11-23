package fr.phundir.wplmanager.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores, per world, which plugins are enabled or disabled.
 * Plugins without an entry are considered enabled.
 * <p>
 * Thread-safe: it is read from network threads by the ProtocolLib packet blocker.
 */
public final class WorldStatusStore {
    private static final String SECTION = "pluginWorldStatus";

    private final JavaPlugin plugin;
    /** world name -> (plugin name -> enabled) */
    private final Map<String, Map<String, Boolean>> statusByWorld = new ConcurrentHashMap<>();

    public WorldStatusStore(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        statusByWorld.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(SECTION);
        if (section == null) {
            return;
        }
        for (String worldName : section.getKeys(false)) {
            ConfigurationSection worldSection = section.getConfigurationSection(worldName);
            if (worldSection == null) {
                continue;
            }
            Map<String, Boolean> statuses = new ConcurrentHashMap<>();
            for (String pluginName : worldSection.getKeys(false)) {
                statuses.put(pluginName, worldSection.getBoolean(pluginName));
            }
            statusByWorld.put(worldName, statuses);
        }
    }

    public void save() {
        plugin.getConfig().set(SECTION, statusByWorld);
        plugin.saveConfig();
    }

    public boolean isEnabled(String pluginName, String worldName) {
        Map<String, Boolean> statuses = statusByWorld.get(worldName);
        return statuses == null || statuses.getOrDefault(pluginName, true);
    }

    /** Changes a status in memory; call {@link #save()} to persist it. */
    public void setEnabled(String pluginName, String worldName, boolean enabled) {
        statusByWorld.computeIfAbsent(worldName, k -> new ConcurrentHashMap<>()).put(pluginName, enabled);
    }

    /**
     * Removes the plugin's explicit status in one world (it becomes enabled by default).
     *
     * @return {@code true} if there was something to remove
     */
    public boolean reset(String pluginName, String worldName) {
        Map<String, Boolean> statuses = statusByWorld.get(worldName);
        if (statuses == null || statuses.remove(pluginName) == null) {
            return false;
        }
        if (statuses.isEmpty()) {
            statusByWorld.remove(worldName);
        }
        return true;
    }

    /**
     * Removes the plugin's explicit statuses in every world.
     *
     * @return {@code true} if there was something to remove
     */
    public boolean resetEverywhere(String pluginName) {
        boolean changed = false;
        for (String worldName : new HashSet<>(statusByWorld.keySet())) {
            changed |= reset(pluginName, worldName);
        }
        return changed;
    }

    /** Worlds where the plugin has an explicit status. */
    public Set<String> getConfiguredWorlds(String pluginName) {
        Set<String> worlds = new HashSet<>();
        for (Map.Entry<String, Map<String, Boolean>> entry : statusByWorld.entrySet()) {
            if (entry.getValue().containsKey(pluginName)) {
                worlds.add(entry.getKey());
            }
        }
        return worlds;
    }

    /** Names of the plugins with an explicit status in at least one world. */
    public Set<String> getConfiguredPlugins() {
        Set<String> plugins = new HashSet<>();
        for (Map<String, Boolean> statuses : statusByWorld.values()) {
            plugins.addAll(statuses.keySet());
        }
        return plugins;
    }

    /** {@code true} if at least one plugin is disabled in this world (cheap pre-check for hot paths). */
    public boolean hasDisabledPlugins(String worldName) {
        Map<String, Boolean> statuses = statusByWorld.get(worldName);
        return statuses != null && statuses.containsValue(Boolean.FALSE);
    }

    /** Names of the plugins explicitly disabled in at least one world. */
    public Set<String> getDisabledPlugins() {
        Set<String> disabled = new HashSet<>();
        for (Map<String, Boolean> statuses : statusByWorld.values()) {
            for (Map.Entry<String, Boolean> entry : statuses.entrySet()) {
                if (!entry.getValue()) {
                    disabled.add(entry.getKey());
                }
            }
        }
        return disabled;
    }

    /** Explicitly configured statuses for a world (empty if none). */
    public Map<String, Boolean> getStatuses(String worldName) {
        Map<String, Boolean> statuses = statusByWorld.get(worldName);
        return statuses == null ? Collections.<String, Boolean>emptyMap() : Collections.unmodifiableMap(statuses);
    }
}
