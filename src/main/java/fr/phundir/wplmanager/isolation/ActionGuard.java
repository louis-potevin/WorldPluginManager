package fr.phundir.wplmanager.isolation;

import fr.phundir.wplmanager.blocking.BlockingPolicy;
import fr.phundir.wplmanager.config.WorldStatusStore;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/**
 * Decides whether an action (teleport, kick, damage, message...) was caused by a plugin
 * that is disabled in the world where it happens, by looking at the plugins on the call stack.
 */
public final class ActionGuard {
    private final BlockingPolicy policy;
    private final WorldStatusStore statusStore;
    private final CallerPlugins callers;
    private final Logger logger;
    private final boolean logBlocked;

    ActionGuard(Plugin self, BlockingPolicy policy, WorldStatusStore statusStore, boolean logBlocked) {
        this.policy = policy;
        this.statusStore = statusStore;
        this.callers = new CallerPlugins(self);
        this.logger = self.getLogger();
        this.logBlocked = logBlocked;
    }

    /**
     * @param player the player affected, if any (the bypass permission lets the action through)
     * @return the plugin disabled in {@code world} that caused the current action, or {@code null}
     */
    public Plugin findBlockedCaller(World world, Player player) {
        // Walking the stack has a cost: skip worlds where nothing is disabled.
        if (world == null || !statusStore.hasDisabledPlugins(world.getName())) {
            return null;
        }
        return callers.findOnStack(caller -> policy.isBlocked(caller, world, player));
    }

    /**
     * Cancels {@code event} if a plugin disabled in {@code world} caused it.
     *
     * @param action short description for the log, e.g. {@code "teleport"}
     * @return the plugin responsible if the event was cancelled, {@code null} otherwise
     */
    Plugin cancelIfBlocked(Cancellable event, World world, Player player, String action) {
        Plugin caller = findBlockedCaller(world, player);
        if (caller != null) {
            event.setCancelled(true);
            logBlocked(caller, action, world, player);
        }
        return caller;
    }

    /** Logs a blocked action when {@code isolation.log-blocked} is enabled. */
    public void logBlocked(Plugin caller, String action, World world, Player player) {
        if (logBlocked) {
            logger.info("Blocked " + action + " by " + caller.getName() + " in world " + world.getName()
                    + (player != null ? " (player " + player.getName() + ")" : ""));
        }
    }
}
