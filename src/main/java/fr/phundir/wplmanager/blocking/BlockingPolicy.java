package fr.phundir.wplmanager.blocking;

import fr.phundir.wplmanager.config.WorldStatusStore;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Decides whether a plugin may be used in a world.
 */
public final class BlockingPolicy {
    private final Plugin self;
    private final WorldStatusStore statusStore;

    public BlockingPolicy(Plugin self, WorldStatusStore statusStore) {
        this.self = self;
        this.statusStore = statusStore;
    }

    /**
     * @param owner the plugin the player is trying to use, may be {@code null}
     * @return {@code true} if {@code owner} is disabled in the player's world
     */
    public boolean isBlocked(Player player, Plugin owner) {
        return isBlocked(owner, player.getWorld());
    }

    /**
     * @param owner the plugin to check, may be {@code null}
     * @param world where the action happens, may be {@code null} (never blocked)
     */
    public boolean isBlocked(Plugin owner, World world) {
        if (owner == null || owner == self || world == null) {
            return false;
        }
        return !statusStore.isEnabled(owner.getName(), world.getName());
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
