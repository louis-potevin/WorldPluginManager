package fr.phundir.wplmanager.isolation;

import fr.phundir.wplmanager.blocking.BlockingPolicy;
import fr.phundir.wplmanager.config.WorldStatusStore;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;

/**
 * Isolates plugins from the worlds where they are disabled, beyond commands:
 * their event listeners (and, see {@link #refresh()}, their scheduled tasks).
 * <p>
 * Plugins can register listeners at any time, so the server is rescanned
 * periodically and after every status change.
 */
public final class PluginIsolation {
    private final Plugin self;
    private final WorldStatusStore statusStore;
    private final IsolationSettings settings;
    private final ListenerIsolator listeners;
    private BukkitTask scanTask;

    public PluginIsolation(Plugin self, WorldStatusStore statusStore, BlockingPolicy policy, IsolationSettings settings) {
        this.self = self;
        this.statusStore = statusStore;
        this.settings = settings;
        this.listeners = settings.isolateEvents()
                ? new ListenerIsolator(self, policy, settings.getAlwaysAllowedEvents())
                : null;
    }

    /** Starts the periodic scan; the first one runs on the next tick, once every plugin is enabled. */
    public void start() {
        if (listeners == null) {
            return;
        }
        scanTask = Bukkit.getScheduler().runTaskTimer(self, this::refresh, 1L, settings.getScanInterval());
    }

    /** Applies the current statuses to listeners registered since the last scan. */
    public void refresh() {
        Set<String> restricted = statusStore.getDisabledPlugins();
        if (restricted.isEmpty()) {
            return;
        }
        if (listeners != null) {
            listeners.wrap(restricted);
        }
    }

    /** Stops scanning and restores every listener to its original state. */
    public void stop() {
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
        if (listeners != null) {
            listeners.restore();
        }
    }
}
