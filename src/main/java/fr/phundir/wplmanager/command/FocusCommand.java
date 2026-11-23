package fr.phundir.wplmanager.command;

import fr.phundir.wplmanager.WorldPluginManager;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;

/**
 * {@code /focus <plugin> <world>}: enables a plugin in one world and disables it everywhere else.
 */
public final class FocusCommand extends PluginWorldCommand {

    public FocusCommand(WorldPluginManager plugin) {
        super(plugin, "worldpluginmanager.focus", "/focus <plugin> <world>", 2, 2);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        World focusedWorld = findWorld(sender, args[1]);
        if (focusedWorld == null) {
            return;
        }
        Plugin target = findPlugin(sender, args[0]);
        if (target == null) {
            return;
        }

        for (String worldName : worldNames()) {
            applyStatus(sender, target, worldName, worldName.equals(focusedWorld.getName()));
        }
        statusStore.save();
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
