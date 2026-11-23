package fr.phundir.wplmanager;

import fr.phundir.wplmanager.blocking.BlockingPolicy;
import fr.phundir.wplmanager.blocking.CommandOwnerResolver;
import fr.phundir.wplmanager.command.LegacyCommand;
import fr.phundir.wplmanager.command.WpmCommand;
import fr.phundir.wplmanager.command.sub.FocusCommand;
import fr.phundir.wplmanager.command.sub.HelpCommand;
import fr.phundir.wplmanager.command.sub.SetStatusCommand;
import fr.phundir.wplmanager.command.sub.StatusCommand;
import fr.phundir.wplmanager.config.Messages;
import fr.phundir.wplmanager.config.WorldStatusStore;
import fr.phundir.wplmanager.listener.CommandBlockListener;
import fr.phundir.wplmanager.listener.CommandVisibilityListener;
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
    /** {@code null} on servers older than 1.13. */
    private CommandVisibilityListener commandVisibility;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        messages = new Messages(this);
        messages.load();

        statusStore = new WorldStatusStore(this);
        statusStore.load();

        blockingPolicy = new BlockingPolicy(this, statusStore);

        registerCommands();

        CommandOwnerResolver commandResolver = new CommandOwnerResolver(getLogger());
        getServer().getPluginManager().registerEvents(
                new CommandBlockListener(messages, commandResolver, blockingPolicy), this);
        registerCommandVisibility(commandResolver);
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

    /**
     * Saves the statuses and applies them to online players.
     * Call after any change made through {@link #getStatusStore()}.
     */
    public void onStatusesChanged() {
        statusStore.save();
        if (commandVisibility != null) {
            commandVisibility.refreshAll();
        }
    }

    private void registerCommands() {
        WpmCommand wpm = new WpmCommand(messages);
        wpm.register(new SetStatusCommand(this, true));
        wpm.register(new SetStatusCommand(this, false));
        wpm.register(new FocusCommand(this));
        wpm.register(new StatusCommand(this));
        wpm.register(new HelpCommand(this, wpm));
        registerCommand(WpmCommand.LABEL, wpm);

        // Pre-/wpm commands, kept for existing servers.
        registerCommand("enpl", new LegacyCommand(wpm, "enable"));
        registerCommand("displ", new LegacyCommand(wpm, "disable"));
        registerCommand("focus", new LegacyCommand(wpm, "focus"));
        registerCommand("flags", new LegacyCommand(wpm, "status"));
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

    private void registerCommandVisibility(CommandOwnerResolver commandResolver) {
        if (!CommandVisibilityListener.isSupported()) {
            getLogger().info("Server older than 1.13: blocked commands stay visible in tab completion.");
            return;
        }
        commandVisibility = new CommandVisibilityListener(commandResolver, blockingPolicy);
        getServer().getPluginManager().registerEvents(commandVisibility, this);
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
