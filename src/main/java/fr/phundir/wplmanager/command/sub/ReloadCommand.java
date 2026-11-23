package fr.phundir.wplmanager.command.sub;

import fr.phundir.wplmanager.WorldPluginManager;
import fr.phundir.wplmanager.command.SubCommand;
import org.bukkit.command.CommandSender;

/**
 * {@code /wpm reload}: reloads config.yml and messages.yml from disk.
 */
public final class ReloadCommand extends SubCommand {

    public ReloadCommand(WorldPluginManager plugin) {
        super(plugin, "reload", "worldpluginmanager.reload", "", 0, 0);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.reload();
        messages.send(sender, "reloaded");
    }
}
