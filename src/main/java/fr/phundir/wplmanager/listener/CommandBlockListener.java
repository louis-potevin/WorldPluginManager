package fr.phundir.wplmanager.listener;

import fr.phundir.wplmanager.WorldPluginManager;
import fr.phundir.wplmanager.config.Messages;
import fr.phundir.wplmanager.config.WorldStatusStore;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Cancels commands whose owning plugin is disabled in the player's world.
 */
public final class CommandBlockListener implements Listener {
    private static final String WORLD_EDIT = "WorldEdit";

    /** WorldEdit commands typed with a double slash ({@code //set}, {@code //copy}...). */
    private static final Set<String> WORLD_EDIT_COMMANDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "help", "tool", "sel", "desel", "pos1", "pos2", "hpos1", "hpos2", "chunk",
            "expand", "contract", "outset", "inset", "size", "count", "distr", "set",
            "replace", "overlay", "naturalize", "walls", "outline", "forest", "cyl",
            "hcyl", "sphere", "hsphere", "pyramid", "hpyramid", "line", "curve",
            "generate", "drain", "fill", "fillr", "floodfill", "fixlava", "fixwater",
            "removeabove", "removebelow", "removenear", "remove", "stack", "move",
            "copy", "cut", "paste", "rotate", "flip", "schematic", "load", "save",
            "list", "clear", "cancel", "undo", "redo", "snapshot", "restore", "info",
            "history", "wand", "drawsel", "replacenear", "smooth", "deform", "forestgen",
            "brush", "mat", "mask", "farwand", "snap", "thru", "unstuck", "toggleplace",
            "tree", "craftscripts", "cs"
    )));

    private final Messages messages;
    private final WorldStatusStore statusStore;

    public CommandBlockListener(WorldPluginManager plugin) {
        this.messages = plugin.getMessages();
        this.statusStore = plugin.getStatusStore();
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        Plugin owner = message.startsWith("//")
                ? findWorldEditOwner(message.substring(2))
                : findCommandOwner(message.substring(1));
        if (owner == null) {
            return;
        }

        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        if (!statusStore.isEnabled(owner.getName(), worldName)) {
            messages.send(player, "plugin_disabled", "plugin", owner.getName(), "world", worldName);
            event.setCancelled(true);
        }
    }

    private static Plugin findCommandOwner(String commandLine) {
        PluginCommand command = Bukkit.getPluginCommand(firstWord(commandLine));
        return command == null ? null : command.getPlugin();
    }

    private static Plugin findWorldEditOwner(String commandLine) {
        if (!WORLD_EDIT_COMMANDS.contains(firstWord(commandLine))) {
            return null;
        }
        return Bukkit.getPluginManager().getPlugin(WORLD_EDIT);
    }

    private static String firstWord(String commandLine) {
        return commandLine.split(" ")[0];
    }
}
