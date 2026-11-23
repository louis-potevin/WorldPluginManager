package fr.phundir.wplmanager.isolation;

import fr.phundir.wplmanager.blocking.BlockingPolicy;
import fr.phundir.wplmanager.config.WorldStatusStore;
import fr.phundir.wplmanager.protocol.LegacyDamageReverter;
import fr.phundir.wplmanager.protocol.OutgoingPacketBlocker;
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
 * through the Bukkit API in those worlds (cancelled), including the messages and sounds
 * it sends to players there (with ProtocolLib).
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
    private final List<Runnable> packetBlockers = new ArrayList<>();
    private Runnable unregisterLegacyDamageReverter;
    private BukkitTask scanTask;

    public PluginIsolation(Plugin self, WorldStatusStore statusStore, BlockingPolicy policy, IsolationSettings settings) {
        this.self = self;
        this.statusStore = statusStore;
        this.settings = settings;
        this.listeners = settings.isolateEvents()
                ? new ListenerIsolator(self, policy, settings.getAlwaysAllowedEvents())
                : null;
        this.tasks = settings.isolateTasks() ? new TaskIsolator(self, policy) : null;
        this.actionGuard = settings.guardActions() || settings.blockMessages() || settings.blockSounds()
                ? new ActionGuard(self, policy, statusStore, settings.logBlocked())
                : null;
    }

    /** Starts the periodic scan; the first one runs on the next tick, once every plugin is enabled. */
    public void start() {
        if (settings.guardActions()) {
            registerActionGuard();
        }
        if (settings.blockMessages() || settings.blockSounds()) {
            registerPacketBlockers();
        }
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
        guardListeners.add(new ActionGuardListener(actionGuard, new BanReverter(self.getLogger())));
        guardListeners.add(new BlockGuardListener(actionGuard));
        if (PotionGuardListener.isSupported()) {
            guardListeners.add(new PotionGuardListener(actionGuard));
        }
        for (Listener listener : guardListeners) {
            Bukkit.getPluginManager().registerEvents(listener, self);
        }
        registerLegacyDamageReverter();
    }

    /**
     * On 1.8, damage dealt without a source fires no event and has to be undone through
     * ProtocolLib instead. The version is checked here: LegacyDamageReverter cannot even be
     * loaded without ProtocolLib.
     */
    private void registerLegacyDamageReverter() {
        if (!Bukkit.getBukkitVersion().startsWith("1.8")) {
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) {
            self.getLogger().info("ProtocolLib not found: on 1.8, damage dealt by disabled plugins cannot be undone.");
            return;
        }
        unregisterLegacyDamageReverter = LegacyDamageReverter.register(self, actionGuard);
    }

    /** ProtocolLib classes are only touched inside {@link OutgoingPacketBlocker}. */
    private void registerPacketBlockers() {
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) {
            self.getLogger().info("ProtocolLib not found: messages and sounds from disabled plugins will not be blocked.");
            return;
        }
        if (settings.blockMessages()) {
            packetBlockers.add(OutgoingPacketBlocker.register(
                    self, actionGuard, OutgoingPacketBlocker.MESSAGE_PACKETS, "message"));
        }
        if (settings.blockSounds()) {
            packetBlockers.add(OutgoingPacketBlocker.register(
                    self, actionGuard, OutgoingPacketBlocker.SOUND_PACKETS, "sound"));
        }
    }

    /** Stops scanning, unregisters the action and packet guards and restores every listener and task. */
    public void stop() {
        for (Runnable unregister : packetBlockers) {
            unregister.run();
        }
        packetBlockers.clear();
        if (unregisterLegacyDamageReverter != null) {
            unregisterLegacyDamageReverter.run();
            unregisterLegacyDamageReverter = null;
        }
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
