package fr.phundir.wplmanager.command.sub;

import fr.phundir.wplmanager.WorldPluginManager;
import fr.phundir.wplmanager.command.SubCommand;
import fr.phundir.wplmanager.util.Plugins;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@code /wpm reset <plugin> [world|*]}: removes a plugin's restriction in a world
 * (the sender's current world by default) or in every world, so it is enabled again.
 */
public final class ResetCommand extends SubCommand {

    public ResetCommand(WorldPluginManager plugin) {
        super(plugin, "reset", "worldpluginmanager.reset", "<plugin> [world|*]", 1, 2);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Plugin target = findPlugin(sender, args[0]);
        if (target == null) {
            return;
        }
        String pluginName = target.getName();

        if (args.length > 1 && ALL_WORLDS.equals(args[1])) {
            if (statusStore.resetEverywhere(pluginName)) {
                plugin.onStatusesChanged();
                messages.send(sender, "reset_everywhere", "plugin", pluginName);
            } else {
                messages.send(sender, "reset_nothing", "plugin", pluginName);
            }
            return;
        }

        World world = targetWorld(sender, args, 1);
        if (world == null) {
            return;
        }
        if (statusStore.reset(pluginName, world.getName())) {
            plugin.onStatusesChanged();
            messages.send(sender, "reset_world", "plugin", pluginName, "world", world.getName());
        } else {
            messages.send(sender, "reset_nothing", "plugin", pluginName);
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        switch (args.length) {
            case 1:
                // Only plugins that actually have restrictions to remove.
                List<String> configured = pluginNames();
                configured.retainAll(statusStore.getConfiguredPlugins());
                return configured;
            case 2:
                Plugin target = Plugins.findByName(args[0]);
                List<String> worlds = target == null
                        ? new ArrayList<String>()
                        : new ArrayList<>(statusStore.getConfiguredWorlds(target.getName()));
                worlds.add(ALL_WORLDS);
                return worlds;
            default:
                return Collections.emptyList();
        }
    }
}
