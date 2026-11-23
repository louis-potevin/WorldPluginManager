package fr.phundir.wplmanager.command.sub;

import fr.phundir.wplmanager.WorldPluginManager;
import fr.phundir.wplmanager.command.SubCommand;
import fr.phundir.wplmanager.command.WpmCommand;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@code /wpm help}: lists the subcommands the sender may use.
 * For players, clicking a line types the command in the chat box.
 */
public final class HelpCommand extends SubCommand {
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
            if (sender instanceof Player) {
                sendClickable((Player) sender, line, "/" + WpmCommand.LABEL + " " + sub.getName() + " ");
            } else {
                sender.sendMessage(line);
            }
        }
        messages.send(sender, "help_footer");
    }

    @SuppressWarnings("deprecation") // HoverEvent(Action, BaseComponent[]) is the only constructor available on every version
    private void sendClickable(Player player, String line, String suggestion) {
        BaseComponent[] components = TextComponent.fromLegacyText(line);
        ClickEvent click = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggestion);
        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(messages.get("help_hover")));
        for (BaseComponent component : components) {
            component.setClickEvent(click);
            component.setHoverEvent(hover);
        }
        player.spigot().sendMessage(components);
    }
}
