package fr.phundir.wplmanager.command.sub;

import fr.phundir.wplmanager.WorldPluginManager;
import fr.phundir.wplmanager.command.SubCommand;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;

/**
 * {@code /wpm focus <plugin> [world]}: enables a plugin only in one world
 * (the sender's current world by default) and disables it everywhere else.
 */
public final class FocusCommand extends SubCommand {

    public FocusCommand(WorldPluginManager plugin) {
        super(plugin, "focus", "worldpluginmanager.focus", "<plugin> [world]", 1, 2, "only");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Plugin target = findPlugin(sender, args[0]);
        if (target == null) {
            return;
        }
        if (target == plugin) {
            messages.send(sender, "cannot_disable_main_plugin", "plugin", target.getName());
            return;
        }
        World focusedWorld = targetWorld(sender, args, 1);
        if (focusedWorld == null) {
            return;
        }

        for (String worldName : worldNames()) {
            statusStore.setEnabled(target.getName(), worldName, worldName.equals(focusedWorld.getName()));
        }
        plugin.onStatusesChanged();
        messages.send(sender, "plugin_focused", "plugin", target.getName(), "world", focusedWorld.getName());
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        switch (args.length) {
            case 1:
                return otherPluginNames();
            case 2:
                return worldNames();
            default:
                return Collections.emptyList();
        }
    }
}
