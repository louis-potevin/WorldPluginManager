package fr.phundir.wplmanager.command;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

/**
 * Sends a chat line that types a command when clicked.
 * <p>
 * Uses the BungeeCord chat API bundled with Spigot. It may be missing (CraftBukkit, future
 * Paper versions), so callers must catch {@link LinkageError} and fall back to plain text.
 */
public final class ClickableLine {

    private ClickableLine() {
    }

    @SuppressWarnings("deprecation") // HoverEvent(Action, BaseComponent[]): the only constructor on every version
    public static void send(Player player, String line, String suggestion, String hoverText) {
        BaseComponent[] components = TextComponent.fromLegacyText(line);
        ClickEvent click = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggestion);
        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hoverText));
        for (BaseComponent component : components) {
            component.setClickEvent(click);
            component.setHoverEvent(hover);
        }
        player.spigot().sendMessage(components);
    }
}
