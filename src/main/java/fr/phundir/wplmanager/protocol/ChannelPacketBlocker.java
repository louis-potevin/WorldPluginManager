package fr.phundir.wplmanager.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.MinecraftKey;
import fr.phundir.wplmanager.blocking.BlockingPolicy;
import fr.phundir.wplmanager.util.Plugins;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.regex.Pattern;

/**
 * Blocks plugin messaging channel packets ({@code namespace:channel}) when the
 * plugin owning the namespace is disabled in the player's world.
 * <p>
 * Requires ProtocolLib: only reference this class after checking that ProtocolLib is installed.
 */
public final class ChannelPacketBlocker extends PacketAdapter {
    private static final Pattern CHANNEL_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    private final BlockingPolicy policy;
    private volatile boolean failureLogged;

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
        if (Packets.hasNoRealPlayer(event)) {
            return;
        }
        try {
            String channel = channelOf(event.getPacket());
            if (channel != null && isBlocked(event.getPlayer(), channel)) {
                event.setCancelled(true);
            }
        } catch (RuntimeException | LinkageError e) {
            // Never break the connection over an unexpected packet layout or ProtocolLib version.
            if (!failureLogged) {
                failureLogged = true;
                plugin.getLogger().warning("Could not read a plugin channel packet (" + e
                        + "). Plugin channels may not be blocked on this version.");
            }
        }
    }

    /**
     * The channel of a custom payload packet, whose layout depends on the version:
     * a String before 1.13, a MinecraftKey from 1.13 to 1.20.1, a payload object since 1.20.2.
     */
    private static String channelOf(PacketContainer packet) {
        try {
            if (packet.getMinecraftKeys().size() > 0) {
                MinecraftKey key = packet.getMinecraftKeys().read(0);
                return key == null ? null : key.getFullKey();
            }
        } catch (LinkageError e) {
            // ProtocolLib 3 has no MinecraftKey: channels are plain strings there.
        }
        if (packet.getStrings().size() > 0) {
            return packet.getStrings().read(0);
        }
        return payloadChannel(packet.getHandle());
    }

    /** 1.20.2+: the packet holds a payload exposing its channel through id() or type().id(). */
    private static String payloadChannel(Object packet) {
        for (Class<?> type = packet.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    String channel = idOf(field.get(packet));
                    if (channel != null) {
                        return channel;
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Try the next field.
                }
            }
        }
        return null;
    }

    private static String idOf(Object payload) {
        if (payload == null) {
            return null;
        }
        Object id = invoke(invoke(payload, "type"), "id");
        if (id == null) {
            id = invoke(payload, "id");
        }
        return id != null && CHANNEL_ID.matcher(id.toString()).matches() ? id.toString() : null;
    }

    private static Object invoke(Object target, String method) {
        if (target == null) {
            return null;
        }
        try {
            Method getter = target.getClass().getMethod(method);
            getter.setAccessible(true);
            return getter.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private boolean isBlocked(Player player, String key) {
        int separator = key.indexOf(':');
        String namespace = separator >= 0 ? key.substring(0, separator) : key;

        // Channel namespaces are lowercase, plugin names usually are not.
        return policy.isBlocked(player, Plugins.findByName(namespace));
    }
}
