package fr.phundir.wplmanager.isolation;

import fr.phundir.wplmanager.blocking.BlockingPolicy;
import fr.phundir.wplmanager.config.WorldStatusStore;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.plugin.Plugin;

/**
 * Decides whether an action (teleport, kick, damage...) was caused by a plugin that is
 * disabled in the world where it happens, by looking at the plugins on the call stack.
 */
final class ActionGuard {
    private final BlockingPolicy policy;
    private final WorldStatusStore statusStore;
    private final CallerPlugins callers;

    ActionGuard(Plugin self, BlockingPolicy policy, WorldStatusStore statusStore) {
        this.policy = policy;
        this.statusStore = statusStore;
        this.callers = new CallerPlugins(self);
    }

    /**
     * Cancels {@code event} if a plugin disabled in {@code world} caused it.
     *
     * @param player the player affected, if any (the bypass permission lets the action through)
     */
    void cancelIfBlocked(Cancellable event, World world, Player player) {
        // Walking the stack has a cost: skip worlds where nothing is disabled.
        if (world == null || !statusStore.hasDisabledPlugins(world.getName())) {
            return;
        }
        if (callers.anyOnStack(caller -> policy.isBlocked(caller, world, player))) {
            event.setCancelled(true);
        }
    }
}
