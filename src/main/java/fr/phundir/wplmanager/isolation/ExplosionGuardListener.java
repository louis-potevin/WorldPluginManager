package fr.phundir.wplmanager.isolation;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;

/**
 * Cancels sourceless explosions ({@code World#createExplosion}) created by a disabled plugin.
 * <p>
 * BlockExplodeEvent only exists since 1.8.3: only register this listener when
 * {@link #isSupported()} is {@code true}, so the other block guards still work on older servers.
 */
final class ExplosionGuardListener implements Listener {
    private final ActionGuard guard;

    ExplosionGuardListener(ActionGuard guard) {
        this.guard = guard;
    }

    static boolean isSupported() {
        try {
            Class.forName("org.bukkit.event.block.BlockExplodeEvent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        guard.cancelIfBlocked(event, event.getBlock().getWorld(), null, "explosion");
    }
}
