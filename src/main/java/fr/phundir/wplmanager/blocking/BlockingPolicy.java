package fr.phundir.wplmanager.blocking;

import fr.phundir.wplmanager.config.WorldStatusStore;
import org.bukkit.Bukkit;
import org.bukkit.World;
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
        return isBlocked(owner, player.getWorld(), player);
    }

    /**
     * @param owner  the plugin to check, may be {@code null}
     * @param world  where the action happens, may be {@code null} (never blocked)
     * @param player the player involved, if any; a player with the bypass permission is never blocked
     */
    public boolean isBlocked(Plugin owner, World world, Player player) {
        if (owner == null || owner == self || world == null) {
            return false;
        }
        return !statusStore.isEnabled(owner.getName(), world.getName())
                && (player == null || !player.hasPermission(BYPASS_PERMISSION));
    }

    /** {@code true} if {@code owner} is disabled in every loaded world. */
    public boolean isDisabledEverywhere(Plugin owner) {
        if (owner == self) {
            return false;
        }
        for (World world : Bukkit.getWorlds()) {
            if (statusStore.isEnabled(owner.getName(), world.getName())) {
                return false;
            }
        }
        return true;
    }
}
