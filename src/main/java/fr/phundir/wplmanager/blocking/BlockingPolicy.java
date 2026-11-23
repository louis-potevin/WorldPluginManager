package fr.phundir.wplmanager.blocking;

import fr.phundir.wplmanager.config.WorldStatusStore;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Decides whether a player may use a plugin in the world they are in.
 */
public final class BlockingPolicy {
    public static final String BYPASS_PERMISSION = "worldpluginmanager.bypass";

    private final Plugin self;
    private final WorldStatusStore statusStore;

    public BlockingPolicy(Plugin self, WorldStatusStore statusStore) {
        this.self = self;
        this.statusStore = statusStore;
    }

    /**
     * @param owner the plugin the player is trying to use, may be {@code null}
     * @return {@code true} if {@code owner} is disabled in the player's world and the player cannot bypass it
     */
    public boolean isBlocked(Player player, Plugin owner) {
        if (owner == null || owner == self) {
            return false;
        }
        return !statusStore.isEnabled(owner.getName(), player.getWorld().getName())
                && !player.hasPermission(BYPASS_PERMISSION);
    }
}
