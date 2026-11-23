package fr.phundir.wplmanager.command.sub;

import fr.phundir.wplmanager.WorldPluginManager;
import fr.phundir.wplmanager.command.ClickableLine;
import fr.phundir.wplmanager.command.SubCommand;
import fr.phundir.wplmanager.command.WpmCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@code /wpm help}: lists the subcommands the sender may use.
 * For players, clicking a line types the command in the chat box.
 */
public final class HelpCommand extends SubCommand {
    /** Cleared the first time the server lacks the chat components API. */
    private static volatile boolean clickableSupported = true;

    private final WpmCommand root;

    public HelpCommand(WorldPluginManager plugin, WpmCommand root) {
        super(plugin, "help", null, "", 0, 0, "?");
        this.root = root;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        List<SubCommand> usable = root.usableBy(sender);
        usable.remove(this);
        if (usable.isEmpty()) {
            messages.send(sender, "no_permission");
            return;
        }

        messages.send(sender, "help_header");
        for (SubCommand sub : usable) {
            String line = messages.get("help_entry", "usage", sub.usage(), "description", sub.description());
            if (!(sender instanceof Player) || !sendClickable((Player) sender, line, sub)) {
                sender.sendMessage(line);
            }
        }
        messages.send(sender, "help_footer");
    }

    /** @return {@code false} if clickable messages are not supported by this server */
    private boolean sendClickable(Player player, String line, SubCommand sub) {
        if (!clickableSupported) {
            return false;
        }
        try {
            ClickableLine.send(player, line, "/" + WpmCommand.LABEL + " " + sub.getName() + " ", messages.get("help_hover"));
            return true;
        } catch (LinkageError e) {
            clickableSupported = false;
            return false;
        }
    }
}
