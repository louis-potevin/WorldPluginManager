package fr.phundir.wplmanager.isolation;

import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Bukkit fires no event before a ban, but banning an online player always kicks them.
 * When such a kick is cancelled because a disabled plugin caused it, the ban that
 * was just recorded for that player (name and IP) is removed as well.
 */
final class BanReverter {
    /** A ban created this recently is considered part of the cancelled kick. */
    private static final long RECENT_MILLIS = 5_000L;

    private final Logger logger;

    BanReverter(Logger logger) {
        this.logger = logger;
    }

    /** Removes the name and IP bans created for {@code player} in the last few seconds. */
    @SuppressWarnings("deprecation") // String-based BanList methods: the only ones available on every version
    void revertRecentBan(Player player, String causedBy) {
        try {
            boolean reverted = revert(Bukkit.getBanList(BanList.Type.NAME), player.getName());
            InetSocketAddress address = player.getAddress();
            if (address != null && address.getAddress() != null) {
                reverted |= revert(Bukkit.getBanList(BanList.Type.IP), address.getAddress().getHostAddress());
            }
            if (reverted) {
                logger.info("Removed the ban of " + player.getName() + " issued by " + causedBy
                        + ", which is disabled in the world " + player.getWorld().getName() + ".");
            }
        } catch (RuntimeException | LinkageError e) {
            logger.warning("Could not remove the ban of " + player.getName() + ": " + e);
        }
    }

    @SuppressWarnings({"deprecation", "rawtypes"})
    private static boolean revert(BanList banList, String target) {
        BanEntry entry = banList.getBanEntry(target);
        if (entry == null) {
            return false;
        }
        Date created = entry.getCreated();
        if (created == null || System.currentTimeMillis() - created.getTime() > RECENT_MILLIS) {
            return false;
        }
        banList.pardon(target);
        return true;
    }
}
