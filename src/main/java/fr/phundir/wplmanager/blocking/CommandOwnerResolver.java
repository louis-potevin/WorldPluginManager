package fr.phundir.wplmanager.blocking;

import fr.phundir.wplmanager.util.Plugins;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Finds the plugin that owns a command, using the server's command map.
 * <p>
 * Unlike {@code Bukkit.getPluginCommand}, this also detects commands registered
 * at runtime (WorldEdit's {@code //set}, {@code /wand}..., or any plugin using
 * a dynamic command framework), aliases, and namespaced labels ({@code worldedit:wand}).
 */
public final class CommandOwnerResolver {
    private final CommandMap commandMap;

    public CommandOwnerResolver(Logger logger) {
        this.commandMap = lookupCommandMap(logger);
    }

    /**
     * @param commandLine a command line or label without the leading slash
     *                    ({@code "/set stone"} for {@code //set stone}, {@code "home bed"}, {@code "worldedit:wand"})
     * @return the owning plugin, or {@code null} for vanilla, unknown or unidentifiable commands
     */
    public Plugin resolve(String commandLine) {
        String label = firstWord(commandLine).toLowerCase(Locale.ENGLISH);
        if (label.isEmpty()) {
            return null;
        }

        Command command = commandMap != null ? commandMap.getCommand(label) : Bukkit.getPluginCommand(label);
        if (command instanceof PluginIdentifiableCommand) {
            return ((PluginIdentifiableCommand) command).getPlugin();
        }

        // Unknown or unidentifiable command: fall back on the "plugin:" prefix, if any.
        int separator = label.indexOf(':');
        return separator > 0 ? Plugins.findByName(label.substring(0, separator)) : null;
    }

    private static String firstWord(String commandLine) {
        String trimmed = commandLine.trim();
        int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }

    /** CraftServer exposes getCommandMap() but the Bukkit API does not, hence the reflection. */
    private static CommandMap lookupCommandMap(Logger logger) {
        try {
            Method getter = Bukkit.getServer().getClass().getMethod("getCommandMap");
            return (CommandMap) getter.invoke(Bukkit.getServer());
        } catch (ReflectiveOperationException | ClassCastException e) {
            logger.warning("Could not access the server command map (" + e + "). "
                    + "Only commands declared in plugin.yml files will be detected.");
            return null;
        }
    }
}
