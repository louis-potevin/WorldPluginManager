package fr.phundir.wplmanager.isolation;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;

/**
 * Cancels potion effects added or removed by a plugin disabled in the entity's world.
 * <p>
 * Requires Minecraft 1.13+: only register it when {@link #isSupported()} is {@code true}.
 */
final class PotionGuardListener implements Listener {
    private final ActionGuard guard;

    PotionGuardListener(ActionGuard guard) {
        this.guard = guard;
    }

    static boolean isSupported() {
        try {
            Class.forName("org.bukkit.event.entity.EntityPotionEffectEvent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (event.getCause() == EntityPotionEffectEvent.Cause.PLUGIN) {
            Entity entity = event.getEntity();
            guard.cancelIfBlocked(event, entity.getWorld(), entity instanceof Player ? (Player) entity : null);
        }
    }
}
