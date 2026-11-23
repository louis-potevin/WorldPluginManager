package fr.phundir.wplmanager.command;

import fr.phundir.wplmanager.WorldPluginManager;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * {@code /flags <world> [plugin]}: shows the status of one plugin, or of every configured plugin, in a world.
 */
public final class FlagsCommand extends PluginWorldCommand {

    public FlagsCommand(WorldPluginManager plugin) {
        super(plugin, "worldpluginmanager.flags", "/flags <world> [plugin]", 1, 2);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        String worldName = args[0];
        if (findWorld(sender, worldName) == null) {
            return;
        }

        if (args.length == 2) {
            showPluginStatus(sender, worldName, args[1]);
        } else {
            showWorldStatuses(sender, worldName);
        }
    }

    private void showPluginStatus(CommandSender sender, String worldName, String pluginName) {
        Plugin target = findPlugin(sender, pluginName);
        if (target == null) {
            return;
        }
        sendStatus(sender, target.getName(), worldName, statusStore.isEnabled(target.getName(), worldName));
    }

    private void showWorldStatuses(CommandSender sender, String worldName) {
        Map<String, Boolean> statuses = statusStore.getStatuses(worldName);
        if (statuses.isEmpty()) {
            messages.send(sender, "flags_empty", "world", worldName);
            return;
        }
        messages.send(sender, "flags_header", "world", worldName);
        for (Map.Entry<String, Boolean> entry : statuses.entrySet()) {
            sendStatus(sender, entry.getKey(), worldName, entry.getValue());
        }
    }

    @Override
    protected List<String> completions(int argIndex) {
        switch (argIndex) {
            case 0:
                return worldNames();
            case 1:
                return pluginNames();
            default:
                return Collections.emptyList();
        }
    }
}
