package fr.phundir.wplmanager.command;

import fr.phundir.wplmanager.WorldPluginManager;
import fr.phundir.wplmanager.config.Messages;
import fr.phundir.wplmanager.config.WorldStatusStore;
import fr.phundir.wplmanager.util.Plugins;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A {@code /wpm <name> ...} subcommand. {@link WpmCommand} checks the permission and
 * the argument count before calling {@link #execute}.
 */
public abstract class SubCommand {
    /** World argument meaning "every loaded world". */
    protected static final String ALL_WORLDS = "*";

    protected final WorldPluginManager plugin;
    protected final Messages messages;
    protected final WorldStatusStore statusStore;

    private final String name;
    private final List<String> aliases;
    private final String permission;
    private final String arguments;
    private final int minArgs;
    private final int maxArgs;

    /**
     * @param permission required permission, or {@code null} for none
     * @param arguments  argument syntax shown in help and usage, e.g. {@code "<plugin> [world]"}
     */
    protected SubCommand(WorldPluginManager plugin, String name, String permission, String arguments,
                         int minArgs, int maxArgs, String... aliases) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
        this.statusStore = plugin.getStatusStore();
        this.name = name;
        this.aliases = Arrays.asList(aliases);
        this.permission = permission;
        this.arguments = arguments;
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
    }

    /** Runs the subcommand; {@code args} excludes the subcommand name. */
    public abstract void execute(CommandSender sender, String[] args);

    /**
     * Suggestions for the last element of {@code args} (the one being typed).
     * Filtering by what has already been typed is done by {@link WpmCommand}.
     */
    public List<String> complete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    public String getName() {
        return name;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public int getMaxArgs() {
        return maxArgs;
    }

    public boolean acceptsArgCount(int count) {
        return count >= minArgs && count <= maxArgs;
    }

    public boolean canUse(CommandSender sender) {
        return permission == null || sender.hasPermission(permission);
    }

    public String usage() {
        return "/" + WpmCommand.LABEL + " " + name + (arguments.isEmpty() ? "" : " " + arguments);
    }

    public String description() {
        return messages.get("help_" + name);
    }

    /**
     * Returns the world, or {@code null} after notifying the sender.
     * Always use {@link World#getName()} afterwards: the lookup ignores case, the stored statuses do not.
     */
    protected World findWorld(CommandSender sender, String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            messages.send(sender, "world_not_found", "world", worldName);
        }
        return world;
    }

    /**
     * World given at {@code args[index]}, or the sender's current world when omitted.
     * Returns {@code null} after notifying the sender if there is none.
     */
    protected World targetWorld(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            return findWorld(sender, args[index]);
        }
        if (sender instanceof Player) {
            return ((Player) sender).getWorld();
        }
        messages.send(sender, "world_required", "usage", usage());
        return null;
    }

    /** Returns the plugin (name matched ignoring case), or {@code null} after notifying the sender. */
    protected Plugin findPlugin(CommandSender sender, String pluginName) {
        Plugin target = Plugins.findByName(pluginName);
        if (target == null) {
            messages.send(sender, "plugin_not_found", "plugin", pluginName);
        }
        return target;
    }

    protected void sendStatus(CommandSender sender, String pluginName, String worldName, boolean enabled) {
        messages.send(sender, enabled ? "plugin_enabled" : "plugin_disabled",
                "plugin", pluginName, "world", worldName);
    }

    /** Sends two compact lines: what is enabled and what is disabled. */
    protected void sendEnabledDisabled(CommandSender sender, List<String> enabled, List<String> disabled) {
        messages.send(sender, "list_enabled", "count", String.valueOf(enabled.size()), "entries", join(enabled));
        messages.send(sender, "list_disabled", "count", String.valueOf(disabled.size()), "entries", join(disabled));
    }

    private String join(List<String> entries) {
        return entries.isEmpty() ? messages.get("list_none") : String.join(", ", entries);
    }

    protected static List<String> pluginNames() {
        return Arrays.stream(Bukkit.getPluginManager().getPlugins())
                .map(Plugin::getName)
                .collect(Collectors.toList());
    }

    protected static List<String> worldNames() {
        return Bukkit.getWorlds().stream()
                .map(World::getName)
                .collect(Collectors.toList());
    }
}
