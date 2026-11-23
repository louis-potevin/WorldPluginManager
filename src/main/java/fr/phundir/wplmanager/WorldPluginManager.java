package fr.phundir.wplmanager;

import fr.phundir.wplmanager.blocking.BlockingPolicy;
import fr.phundir.wplmanager.blocking.CommandOwnerResolver;
import fr.phundir.wplmanager.command.FlagsCommand;
import fr.phundir.wplmanager.command.FocusCommand;
import fr.phundir.wplmanager.command.PluginStatusCommand;
import fr.phundir.wplmanager.config.Messages;
import fr.phundir.wplmanager.config.WorldStatusStore;
import fr.phundir.wplmanager.listener.CommandBlockListener;
import fr.phundir.wplmanager.protocol.ChannelPacketBlocker;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point of WorldPluginManager: wires configuration, commands and listeners together.
 */
public final class WorldPluginManager extends JavaPlugin {
    private static final String PROTOCOL_LIB = "ProtocolLib";

    private Messages messages;
    private WorldStatusStore statusStore;
    private BlockingPolicy blockingPolicy;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        messages = new Messages(this);
        messages.load();

        statusStore = new WorldStatusStore(this);
        statusStore.load();

        blockingPolicy = new BlockingPolicy(this, statusStore);

        registerCommand("displ", new PluginStatusCommand(this, false));
        registerCommand("enpl", new PluginStatusCommand(this, true));
        registerCommand("flags", new FlagsCommand(this));
        registerCommand("focus", new FocusCommand(this));

        CommandOwnerResolver commandResolver = new CommandOwnerResolver(getLogger());
        getServer().getPluginManager().registerEvents(
                new CommandBlockListener(messages, commandResolver, blockingPolicy), this);
        registerPacketBlocker();

        getLogger().info("Plugin enabled!");
    }

    @Override
    public void onDisable() {
        if (statusStore != null) {
            statusStore.save();
        }
        getLogger().info("Plugin disabled!");
    }

    public Messages getMessages() {
        return messages;
    }

    public WorldStatusStore getStatusStore() {
        return statusStore;
    }

    private void registerCommand(String name, TabExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command /" + name + " is missing from plugin.yml.");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    /**
     * ProtocolLib classes are only touched inside {@link ChannelPacketBlocker},
     * so this class still loads when ProtocolLib is absent.
     */
    private void registerPacketBlocker() {
        if (getServer().getPluginManager().getPlugin(PROTOCOL_LIB) == null) {
            getLogger().warning(PROTOCOL_LIB + " not found. Packet listeners disabled.");
            return;
        }
        ChannelPacketBlocker.register(this, blockingPolicy);
    }
}
