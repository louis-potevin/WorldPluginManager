package fr.phundir.wplmanager.command;

import fr.phundir.wplmanager.WorldPluginManager;
import fr.phundir.wplmanager.config.Messages;
import fr.phundir.wplmanager.config.WorldStatusStore;
import fr.phundir.wplmanager.util.Plugins;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Shared behavior of the plugin's commands: permission and argument checks,
 * world/plugin lookup, status changes and tab completion.
 */
public abstract class PluginWorldCommand implements TabExecutor {
    protected final WorldPluginManager plugin;
    protected final Messages messages;
    protected final WorldStatusStore statusStore;

    private final String permission;
    private final String usage;
    private final int minArgs;
    private final int maxArgs;

    protected PluginWorldCommand(WorldPluginManager plugin, String permission, String usage, int minArgs, int maxArgs) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
        this.statusStore = plugin.getStatusStore();
        this.permission = permission;
        this.usage = usage;
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
    }

    /** Runs the command once permission and argument count have been checked. */
    protected abstract void execute(CommandSender sender, String[] args);

    /** Suggestions for the argument at {@code argIndex} (0-based). */
    protected abstract List<String> completions(int argIndex);

    @Override
    public final boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(permission)) {
            messages.send(sender, "no_permission");
            return true;
        }
        if (args.length < minArgs || args.length > maxArgs) {
            messages.send(sender, "usage", "usage", usage);
            return true;
        }
        execute(sender, args);
        return true;
    }

    @Override
    public final List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0 || !sender.hasPermission(permission)) {
            return new ArrayList<>();
        }
        String typed = args[args.length - 1];
        return StringUtil.copyPartialMatches(typed, completions(args.length - 1), new ArrayList<String>());
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

    /** Returns the plugin (name matched ignoring case), or {@code null} after notifying the sender. */
    protected Plugin findPlugin(CommandSender sender, String pluginName) {
        Plugin target = Plugins.findByName(pluginName);
        if (target == null) {
            messages.send(sender, "plugin_not_found", "plugin", pluginName);
        }
        return target;
    }

    /**
     * Enables or disables {@code target} in a world and notifies the sender.
     * Refuses to disable WorldPluginManager itself. Does not save.
     */
    protected void applyStatus(CommandSender sender, Plugin target, String worldName, boolean enabled) {
        if (!enabled && target == plugin) {
            messages.send(sender, "cannot_disable_main_plugin", "plugin", target.getName());
            return;
        }
        statusStore.setEnabled(target.getName(), worldName, enabled);
        sendStatus(sender, target.getName(), worldName, enabled);
    }

    protected void sendStatus(CommandSender sender, String pluginName, String worldName, boolean enabled) {
        messages.send(sender, enabled ? "plugin_enabled" : "plugin_disabled",
                "plugin", pluginName, "world", worldName);
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
