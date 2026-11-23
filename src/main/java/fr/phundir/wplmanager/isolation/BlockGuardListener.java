package fr.phundir.wplmanager.isolation;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

/**
 * Cancels the world changes a disabled plugin makes through events: blocks broken or
 * placed on behalf of a player, explosions it creates, and entities it spawns
 * (creatures, falling blocks, primed TNT, dropped items...).
 * <p>
 * Direct block changes ({@code Block#setType}, {@code BlockState#update}) fire no event
 * and cannot be blocked.
 */
final class BlockGuardListener implements Listener {
    private final ActionGuard guard;

    BlockGuardListener(ActionGuard guard) {
        this.guard = guard;
    }

    /** E.g. {@code Player#breakBlock}, or a plugin simulating a player breaking a block. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        guard.cancelIfBlocked(event, event.getBlock().getWorld(), event.getPlayer(), "block break");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        guard.cancelIfBlocked(event, event.getBlock().getWorld(), event.getPlayer(), "block place");
    }

    /** E.g. {@code World#createExplosion} with an entity source. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        guard.cancelIfBlocked(event, event.getLocation().getWorld(), null, "explosion");
    }

    /** E.g. {@code World#createExplosion} without a source. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        guard.cancelIfBlocked(event, event.getBlock().getWorld(), null, "explosion");
    }

    /**
     * Entities spawned through the API: creatures ({@code World#spawnEntity}), falling blocks,
     * primed TNT, dropped items... Natural creature spawns are skipped to save stack walks.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event instanceof CreatureSpawnEvent
                && ((CreatureSpawnEvent) event).getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
            return;
        }
        World world = event.getLocation().getWorld();
        Plugin caller = guard.findBlockedCaller(world, null);
        if (caller != null) {
            event.setCancelled(true);
            guard.logBlocked(caller, "spawn of " + event.getEntityType().name().toLowerCase(Locale.ROOT), world, null);
        }
    }
}
