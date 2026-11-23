package fr.phundir.wplmanager.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.Map;
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

    /** Explicitly configured statuses for a world (empty if none). */
    public Map<String, Boolean> getStatuses(String worldName) {
        Map<String, Boolean> statuses = statusByWorld.get(worldName);
        return statuses == null ? Collections.<String, Boolean>emptyMap() : Collections.unmodifiableMap(statuses);
    }
}
