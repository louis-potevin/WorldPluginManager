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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Stops players from receiving packets of a given kind (messages, sounds) sent by a plugin
 * that is disabled in their world. The sending plugin is found on the call stack, which
 * works when the packet is sent from the plugin's code (task, command, listener, broadcast).
 * <p>
 * Requires ProtocolLib: only reference this class after checking that ProtocolLib is installed.
 */
public final class OutgoingPacketBlocker extends PacketAdapter {
    /**
     * Server packets carrying text shown to the player, by ProtocolLib name.
     * Packets are looked up by name because the available ones depend on the Minecraft version.
     */
    public static final Set<String> MESSAGE_PACKETS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "CHAT", "SYSTEM_CHAT", "TITLE", "SET_TITLE_TEXT", "SET_SUBTITLE_TEXT", "SET_ACTION_BAR_TEXT")));

    /** Server packets playing a sound (including the hurt sound of 1.8 damage being undone). */
    public static final Set<String> SOUND_PACKETS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "NAMED_SOUND_EFFECT", "CUSTOM_SOUND_EFFECT", "ENTITY_SOUND")));

    private final ActionGuard guard;
    private final String action;

    private OutgoingPacketBlocker(Plugin plugin, ActionGuard guard, List<PacketType> types, String action) {
        super(plugin, ListenerPriority.LOWEST, types);
        this.guard = guard;
        this.action = action;
    }

    /**
     * Registers a blocker for the given packets.
     *
     * @param packetNames ProtocolLib names of the server packets to block, e.g. {@link #MESSAGE_PACKETS}
     * @param action      what is blocked, for the log, e.g. {@code "message"}
     * @return an action that unregisters it
     */
    public static Runnable register(Plugin plugin, ActionGuard guard, Set<String> packetNames, String action) {
        List<PacketType> types = new ArrayList<>();
        for (PacketType type : PacketType.values()) {
            if (type.getProtocol() == PacketType.Protocol.PLAY && type.isServer()
                    && packetNames.contains(type.name()) && type.isSupported()) {
                types.add(type);
            }
        }
        if (types.isEmpty()) {
            plugin.getLogger().warning("No " + action + " packet found in ProtocolLib. Blocking of " + action + "s disabled.");
            return () -> { };
        }
        OutgoingPacketBlocker blocker = new OutgoingPacketBlocker(plugin, guard, types, action);
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
            guard.logBlocked(sender, action, world, player);
        }
    }
}
