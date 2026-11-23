package fr.phundir.wplmanager.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.List;

/**
 * Keeps the pre-{@code /wpm} commands ({@code /enpl}, {@code /displ}, {@code /focus}, {@code /flags})
 * working by forwarding them to the equivalent {@code /wpm} subcommand.
 */
public final class LegacyCommand implements TabExecutor {
    private final WpmCommand root;
    private final String subCommand;

    public LegacyCommand(WpmCommand root, String subCommand) {
        this.root = root;
        this.subCommand = subCommand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        root.dispatch(sender, subCommand, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return root.complete(sender, subCommand, args);
    }
}
