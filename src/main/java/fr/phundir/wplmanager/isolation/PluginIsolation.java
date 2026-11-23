package fr.phundir.wplmanager.isolation;

import fr.phundir.wplmanager.blocking.BlockingPolicy;
import fr.phundir.wplmanager.config.WorldStatusStore;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Isolates plugins from the worlds where they are disabled, beyond commands:
 * their event listeners (filtered per world) and their scheduled tasks
 * (paused while the plugin is disabled in every world), and the actions it performs
 * through the Bukkit API in those worlds (cancelled).
 * <p>
 * Plugins can register listeners and schedule tasks at any time, so the server is rescanned
 * periodically and after every status change.
 */
public final class PluginIsolation {
    private final Plugin self;
    private final WorldStatusStore statusStore;
    private final IsolationSettings settings;
    private final ListenerIsolator listeners;
    private final TaskIsolator tasks;
    private final ActionGuard actionGuard;
    private final List<Listener> guardListeners = new ArrayList<>();
    private BukkitTask scanTask;

    public PluginIsolation(Plugin self, WorldStatusStore statusStore, BlockingPolicy policy, IsolationSettings settings) {
        this.self = self;
        this.statusStore = statusStore;
        this.settings = settings;
        this.listeners = settings.isolateEvents()
                ? new ListenerIsolator(self, policy, settings.getAlwaysAllowedEvents())
                : null;
        this.tasks = settings.isolateTasks() ? new TaskIsolator(self, policy) : null;
        this.actionGuard = settings.guardActions() ? new ActionGuard(self, policy, statusStore) : null;
    }

    /** Starts the periodic scan; the first one runs on the next tick, once every plugin is enabled. */
    public void start() {
        registerActionGuard();
        if (listeners == null && tasks == null) {
            return;
        }
        scanTask = Bukkit.getScheduler().runTaskTimer(self, this::refresh, 1L, settings.getScanInterval());
    }

    /** Applies the current statuses to listeners and tasks registered since the last scan. */
    public void refresh() {
        Set<String> restricted = statusStore.getDisabledPlugins();
        if (restricted.isEmpty()) {
            return;
        }
        if (listeners != null) {
            listeners.wrap(restricted);
        }
        if (tasks != null) {
            tasks.wrap(restricted);
        }
    }

    private void registerActionGuard() {
        if (actionGuard == null) {
            return;
        }
        guardListeners.add(new ActionGuardListener(actionGuard));
        if (PotionGuardListener.isSupported()) {
            guardListeners.add(new PotionGuardListener(actionGuard));
        }
        for (Listener listener : guardListeners) {
            Bukkit.getPluginManager().registerEvents(listener, self);
        }
    }

    /** Stops scanning, unregisters the action guard and restores every listener and task. */
    public void stop() {
        for (Listener listener : guardListeners) {
            HandlerList.unregisterAll(listener);
        }
        guardListeners.clear();
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
        if (listeners != null) {
            listeners.restore();
        }
        if (tasks != null) {
            tasks.restore();
        }
    }
}
