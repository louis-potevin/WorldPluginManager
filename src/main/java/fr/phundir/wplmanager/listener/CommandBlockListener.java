package fr.phundir.wplmanager.listener;

import fr.phundir.wplmanager.blocking.BlockingPolicy;
import fr.phundir.wplmanager.blocking.CommandOwnerResolver;
import fr.phundir.wplmanager.config.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

/**
 * Cancels commands whose owning plugin is disabled in the player's world.
 * Runs at the lowest priority so other plugins never see a blocked command.
 */
public final class CommandBlockListener implements Listener {
    private final Messages messages;
    private final CommandOwnerResolver resolver;
    private final BlockingPolicy policy;

    public CommandBlockListener(Messages messages, CommandOwnerResolver resolver, BlockingPolicy policy) {
        this.messages = messages;
        this.resolver = resolver;
        this.policy = policy;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        Plugin owner = resolver.resolve(event.getMessage().substring(1));
        if (policy.isBlocked(player, owner)) {
            event.setCancelled(true);
            messages.send(player, "plugin_disabled",
                    "plugin", owner.getName(), "world", player.getWorld().getName());
        }
    }
}
