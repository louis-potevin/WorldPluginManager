package fr.phundir.wplmanager.isolation;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Cancels the actions a disabled plugin performs in a world through the Bukkit API,
 * from anywhere in its code (scheduled tasks included): teleporting, kicking, damaging,
 * changing a game mode, spawning creatures, opening inventories, making a player chat.
 * <p>
 * Runs at the lowest priority so other plugins never see the cancelled action.
 */
final class ActionGuardListener implements Listener {
    private final ActionGuard guard;

    ActionGuardListener(ActionGuard guard) {
        this.guard = guard;
    }

    /** A plugin acting on a player is judged by the world the player is leaving. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        guard.cancelIfBlocked(event, event.getFrom().getWorld(), event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onKick(PlayerKickEvent event) {
        guard.cancelIfBlocked(event, event.getPlayer().getWorld(), event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        guard.cancelIfBlocked(event, entity.getWorld(), entity instanceof Player ? (Player) entity : null);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        guard.cancelIfBlocked(event, event.getPlayer().getWorld(), event.getPlayer());
    }

    /** Only creatures spawned through the API (World#spawnEntity...), not natural spawns. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            guard.cancelIfBlocked(event, event.getLocation().getWorld(), null);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            guard.cancelIfBlocked(event, player.getWorld(), player);
        }
    }

    /** Chat typed by a player is asynchronous; synchronous chat comes from Player#chat in a plugin. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onForcedChat(AsyncPlayerChatEvent event) {
        if (!event.isAsynchronous()) {
            guard.cancelIfBlocked(event, event.getPlayer().getWorld(), event.getPlayer());
        }
    }
}
