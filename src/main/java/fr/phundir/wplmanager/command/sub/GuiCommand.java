package fr.phundir.wplmanager.command.sub;

import fr.phundir.wplmanager.WorldPluginManager;
import fr.phundir.wplmanager.command.SubCommand;
import fr.phundir.wplmanager.gui.WorldsMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /wpm gui}: opens the menu to manage plugins per world.
 */
public final class GuiCommand extends SubCommand {

    public GuiCommand(WorldPluginManager plugin) {
        super(plugin, "gui", "worldpluginmanager.flags", "", 0, 0, "menu");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            messages.send(sender, "players_only");
            return;
        }
        Player player = (Player) sender;
        plugin.getMenus().open(player, new WorldsMenu(plugin.getMenus(), player));
    }
}
