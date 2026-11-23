package fr.phundir.wplmanager.protocol;

import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;

/**
 * ProtocolLib helpers working across ProtocolLib versions (3.x to 5.x).
 */
final class Packets {
    /** PacketEvent#isPlayerTemporary only exists since ProtocolLib 4. */
    private static volatile boolean temporaryCheckAvailable = true;

    private Packets() {
    }

    /**
     * {@code true} if the packet's player is not a real, fully joined player yet
     * (login phase), in which case most Player methods throw.
     */
    static boolean hasNoRealPlayer(PacketEvent event) {
        if (temporaryCheckAvailable) {
            try {
                return event.isPlayerTemporary();
            } catch (NoSuchMethodError e) {
                temporaryCheckAvailable = false;
            }
        }
        Player player = event.getPlayer();
        if (player == null) {
            return true;
        }
        try {
            player.getWorld();
            return false;
        } catch (RuntimeException e) {
            return true;
        }
    }
}
