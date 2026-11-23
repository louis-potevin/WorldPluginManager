package fr.phundir.wplmanager.command.sub;

import fr.phundir.wplmanager.WorldPluginManager;
import fr.phundir.wplmanager.command.SubCommand;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@code /wpm enable|disable <plugin> [world|*]}: enables or disables a plugin in a world
 * (the sender's current world by default) or in every world.
 */
public final class SetStatusCommand extends SubCommand {
    private final boolean enable;

    public SetStatusCommand(WorldPluginManager plugin, boolean enable) {
        super(plugin,
                enable ? "enable" : "disable",
                enable ? "worldpluginmanager.enpl" : "worldpluginmanager.displ",
                "<plugin> [world|*]", 1, 2,
                enable ? "on" : "off");
        this.enable = enable;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Plugin target = findPlugin(sender, args[0]);
        if (target == null) {
            return;
        }
        if (!enable && target == plugin) {
            messages.send(sender, "cannot_disable_main_plugin", "plugin", target.getName());
            return;
        }

        if (args.length > 1 && ALL_WORLDS.equals(args[1])) {
            for (String worldName : worldNames()) {
                statusStore.setEnabled(target.getName(), worldName, enable);
            }
            plugin.onStatusesChanged();
            messages.send(sender, enable ? "plugin_enabled_everywhere" : "plugin_disabled_everywhere",
                    "plugin", target.getName());
            return;
        }

        World world = targetWorld(sender, args, 1);
        if (world == null) {
            return;
        }
        statusStore.setEnabled(target.getName(), world.getName(), enable);
        plugin.onStatusesChanged();
        sendStatus(sender, target.getName(), world.getName(), enable);
    }

    /** Plugins disabled somewhere (and still installed), or every plugin if none is. */
    private List<String> disabledPluginNames() {
        List<String> installed = pluginNames();
        List<String> disabled = new ArrayList<>(installed);
        disabled.retainAll(statusStore.getDisabledPlugins());
        return disabled.isEmpty() ? installed : disabled;
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        switch (args.length) {
            case 1:
                return enable ? disabledPluginNames() : otherPluginNames();
            case 2:
                // Only suggest worlds where the command would change something.
                List<String> worlds = new ArrayList<>(worldsWhere(args[0], !enable));
                worlds.add(ALL_WORLDS);
                return worlds;
            default:
                return Collections.emptyList();
        }
    }
}
