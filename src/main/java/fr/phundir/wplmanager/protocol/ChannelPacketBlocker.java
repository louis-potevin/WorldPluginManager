package fr.phundir.wplmanager.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.MinecraftKey;
import fr.phundir.wplmanager.blocking.BlockingPolicy;
import fr.phundir.wplmanager.util.Plugins;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Blocks plugin messaging channel packets ({@code namespace:channel}) when the
 * plugin owning the namespace is disabled in the player's world.
 * <p>
 * Requires ProtocolLib: only reference this class after checking that ProtocolLib is installed.
 */
public final class ChannelPacketBlocker extends PacketAdapter {
    private final BlockingPolicy policy;

    private ChannelPacketBlocker(Plugin plugin, BlockingPolicy policy) {
        super(plugin, ListenerPriority.NORMAL,
                PacketType.Play.Client.CUSTOM_PAYLOAD, PacketType.Play.Server.CUSTOM_PAYLOAD);
        this.policy = policy;
    }

    public static void register(Plugin plugin, BlockingPolicy policy) {
        ProtocolLibrary.getProtocolManager().addPacketListener(new ChannelPacketBlocker(plugin, policy));
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        blockIfDisabled(event);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        blockIfDisabled(event);
    }

    private void blockIfDisabled(PacketEvent event) {
        MinecraftKey channel = event.getPacket().getMinecraftKeys().read(0);
        if (channel != null && isBlocked(event.getPlayer(), channel)) {
            event.setCancelled(true);
        }
    }

    private boolean isBlocked(Player player, MinecraftKey channel) {
        String key = channel.getFullKey();
        int separator = key.indexOf(':');
        String namespace = separator >= 0 ? key.substring(0, separator) : key;

        // Channel namespaces are lowercase, plugin names usually are not.
        return policy.isBlocked(player, Plugins.findByName(namespace));
    }
}
