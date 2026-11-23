package fr.phundir.wplmanager.command.sub;

import fr.phundir.wplmanager.WorldPluginManager;
import fr.phundir.wplmanager.command.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@code /wpm info <plugin>}: shows in which worlds a plugin is enabled or disabled.
 */
public final class InfoCommand extends SubCommand {

    public InfoCommand(WorldPluginManager plugin) {
        super(plugin, "info", "worldpluginmanager.flags", "<plugin>", 1, 1);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Plugin target = findPlugin(sender, args[0]);
        if (target == null) {
            return;
        }

        List<String> enabled = new ArrayList<>();
        List<String> disabled = new ArrayList<>();
        for (String worldName : worldNames()) {
            (statusStore.isEnabled(target.getName(), worldName) ? enabled : disabled).add(worldName);
        }

        messages.send(sender, "info_header", "plugin", target.getName());
        sendEnabledDisabled(sender, enabled, disabled);
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        return args.length == 1 ? pluginNames() : Collections.<String>emptyList();
    }
}
