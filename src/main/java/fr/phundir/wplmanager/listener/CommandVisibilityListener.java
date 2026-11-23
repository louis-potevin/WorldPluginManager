package fr.phundir.wplmanager.listener;

import fr.phundir.wplmanager.blocking.BlockingPolicy;
import fr.phundir.wplmanager.blocking.CommandOwnerResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.server.TabCompleteEvent;

/**
 * Hides blocked commands from players: removes them from the command list sent
 * to the client (suggestions, red/white highlighting) and from tab completion.
 * <p>
 * Requires Minecraft 1.13+: only register it when {@link #isSupported()} is {@code true}.
 */
public final class CommandVisibilityListener implements Listener {
    private final CommandOwnerResolver resolver;
    private final BlockingPolicy policy;

    public CommandVisibilityListener(CommandOwnerResolver resolver, BlockingPolicy policy) {
        this.resolver = resolver;
        this.policy = policy;
    }

    public static boolean isSupported() {
        try {
            Class.forName("org.bukkit.event.player.PlayerCommandSendEvent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /** Resends the command list to every online player, e.g. after a status change. */
    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.updateCommands();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();
        event.getCommands().removeIf(label -> policy.isBlocked(player, resolver.resolve(label)));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTabComplete(TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player)) {
            return;
        }
        String buffer = event.getBuffer();
        if (!buffer.startsWith("/")) {
            return;
        }
        if (policy.isBlocked((Player) event.getSender(), resolver.resolve(buffer.substring(1)))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        event.getPlayer().updateCommands();
    }
}
