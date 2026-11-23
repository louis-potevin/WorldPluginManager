package fr.phundir.wplmanager.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import fr.phundir.wplmanager.isolation.ActionGuard;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Stops players from receiving the messages (chat, action bar, titles) sent by a plugin
 * that is disabled in their world. The sending plugin is found on the call stack, which
 * works when the packet is sent from the plugin's code (task, command, listener, broadcast).
 * <p>
 * Requires ProtocolLib: only reference this class after checking that ProtocolLib is installed.
 */
public final class MessagePacketBlocker extends PacketAdapter {
    /**
     * Server packets carrying text shown to the player, by ProtocolLib name.
     * Looked up by name because the available packets depend on the Minecraft version.
     */
    private static final Set<String> MESSAGE_PACKETS = new HashSet<>(Arrays.asList(
            "CHAT", "SYSTEM_CHAT", "TITLE", "SET_TITLE_TEXT", "SET_SUBTITLE_TEXT", "SET_ACTION_BAR_TEXT"));

    private final ActionGuard guard;

    private MessagePacketBlocker(Plugin plugin, ActionGuard guard, List<PacketType> types) {
        super(plugin, ListenerPriority.LOWEST, types);
        this.guard = guard;
    }

    /**
     * Registers the blocker.
     *
     * @return an action that unregisters it
     */
    public static Runnable register(Plugin plugin, ActionGuard guard) {
        List<PacketType> types = new ArrayList<>();
        for (PacketType type : PacketType.values()) {
            if (type.getProtocol() == PacketType.Protocol.PLAY && type.isServer()
                    && MESSAGE_PACKETS.contains(type.name()) && type.isSupported()) {
                types.add(type);
            }
        }
        if (types.isEmpty()) {
            plugin.getLogger().warning("No message packet found in ProtocolLib. Message blocking disabled.");
            return () -> { };
        }
        MessagePacketBlocker blocker = new MessagePacketBlocker(plugin, guard, types);
        ProtocolLibrary.getProtocolManager().addPacketListener(blocker);
        return () -> ProtocolLibrary.getProtocolManager().removePacketListener(blocker);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.isPlayerTemporary()) {
            return;
        }
        Player player = event.getPlayer();
        World world = player.getWorld();
        Plugin sender = guard.findBlockedCaller(world, player);
        if (sender != null) {
            event.setCancelled(true);
            guard.logBlocked(sender, "message", world, player);
        }
    }
}
