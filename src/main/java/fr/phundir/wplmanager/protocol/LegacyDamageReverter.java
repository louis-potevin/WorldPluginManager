package fr.phundir.wplmanager.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import fr.phundir.wplmanager.isolation.ActionGuard;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Minecraft 1.7 to 1.9 only: {@code LivingEntity#damage(double)} without a source never fires
 * {@code EntityDamageEvent} (the server builds the event but does not call it), so the damage
 * cannot be cancelled. It is undone instead: right after applying it, the server sends the
 * "hurt" entity status packet from within the plugin's call. If a plugin disabled in that world
 * is on the stack, the animation is dropped and the health restored.
 * <p>
 * Restored health = current health + the damage just dealt, capped by the health the entity
 * had at the start of the tick (players) and by its max health, so it never over-heals.
 * <p>
 * Requires ProtocolLib: only reference this class after checking that ProtocolLib is installed.
 */
@SuppressWarnings("deprecation") // getMaxHealth: the only max health API before 1.9
public final class LegacyDamageReverter extends PacketAdapter {
    private static final byte HURT_STATUS = 2;

    private final ActionGuard guard;
    private final Map<UUID, Double> healthAtTickStart = new HashMap<>();

    private LegacyDamageReverter(Plugin plugin, ActionGuard guard) {
        super(plugin, ListenerPriority.LOWEST, PacketType.Play.Server.ENTITY_STATUS);
        this.guard = guard;
    }

    /**
     * Registers the reverter and its per-tick health snapshot.
     *
     * @return an action that unregisters both
     */
    public static Runnable register(Plugin plugin, ActionGuard guard) {
        LegacyDamageReverter reverter = new LegacyDamageReverter(plugin, guard);
        ProtocolLibrary.getProtocolManager().addPacketListener(reverter);
        BukkitTask snapshots = Bukkit.getScheduler().runTaskTimer(plugin, reverter::snapshotHealth, 0L, 1L);
        return () -> {
            ProtocolLibrary.getProtocolManager().removePacketListener(reverter);
            snapshots.cancel();
        };
    }

    private void snapshotHealth() {
        healthAtTickStart.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            healthAtTickStart.put(player.getUniqueId(), player.getHealth());
        }
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (Packets.hasNoRealPlayer(event) || event.getPacket().getBytes().read(0) != HURT_STATUS) {
            return;
        }
        Entity entity = event.getPacket().getEntityModifier(event).read(0);
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        Plugin caller = guard.findBlockedCaller(entity.getWorld());
        if (caller == null) {
            return;
        }
        // Sent once per viewer: the animation is dropped for each, the health restored once.
        event.setCancelled(true);
        restore((LivingEntity) entity, caller);
    }

    private void restore(LivingEntity entity, Plugin caller) {
        double current = entity.getHealth();
        double target = Math.min(entity.getMaxHealth(), current + entity.getLastDamage());
        Double before = healthAtTickStart.get(entity.getUniqueId());
        if (before != null) {
            target = Math.min(target, before);
        }
        if (target > current) {
            entity.setHealth(target);
            entity.setLastDamage(0);
            entity.setNoDamageTicks(0);
            guard.logBlocked(caller, "damage", entity.getWorld(), entity instanceof Player ? (Player) entity : null);
        }
    }
}
