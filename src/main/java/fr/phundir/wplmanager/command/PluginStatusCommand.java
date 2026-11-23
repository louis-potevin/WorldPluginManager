package fr.phundir.wplmanager.command;

import fr.phundir.wplmanager.WorldPluginManager;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;

/**
 * {@code /enpl <plugin> <world>} and {@code /displ <plugin> <world>}:
 * enables or disables a plugin in one world.
 */
public final class PluginStatusCommand extends PluginWorldCommand {
    private final boolean enable;

    public PluginStatusCommand(WorldPluginManager plugin, boolean enable) {
        super(plugin,
                enable ? "worldpluginmanager.enpl" : "worldpluginmanager.displ",
                enable ? "/enpl <plugin> <world>" : "/displ <plugin> <world>",
                2, 2);
        this.enable = enable;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        World world = findWorld(sender, args[1]);
        if (world == null) {
            return;
        }
        Plugin target = findPlugin(sender, args[0]);
        if (target == null) {
            return;
        }

        applyStatus(sender, target, world.getName(), enable);
        plugin.onStatusesChanged();
    }

    @Override
    protected List<String> completions(int argIndex) {
        switch (argIndex) {
            case 0:
                return pluginNames();
            case 1:
                return worldNames();
            default:
                return Collections.emptyList();
        }
    }
}
