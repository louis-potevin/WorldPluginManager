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
 * {@code /wpm status [world] [plugin]}: shows which plugins are enabled in a world
 * (the sender's current world by default), or the status of a single plugin.
 */
public final class StatusCommand extends SubCommand {

    public StatusCommand(WorldPluginManager plugin) {
        super(plugin, "status", "worldpluginmanager.flags", "[world] [plugin]", 0, 2, "flags");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        World world = targetWorld(sender, args, 0);
        if (world == null) {
            return;
        }
        String worldName = world.getName();

        if (args.length == 2) {
            Plugin target = findPlugin(sender, args[1]);
            if (target != null) {
                sendStatus(sender, target.getName(), worldName, statusStore.isEnabled(target.getName(), worldName));
            }
            return;
        }

        List<String> enabled = new ArrayList<>();
        List<String> disabled = new ArrayList<>();
        List<String> names = pluginNames();
        names.sort(String.CASE_INSENSITIVE_ORDER);
        for (String pluginName : names) {
            (statusStore.isEnabled(pluginName, worldName) ? enabled : disabled).add(pluginName);
        }

        messages.send(sender, "status_header", "world", worldName);
        sendEnabledDisabled(sender, enabled, disabled);
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        switch (args.length) {
            case 1:
                return worldNames();
            case 2:
                return pluginNames();
            default:
                return Collections.emptyList();
        }
    }
}
