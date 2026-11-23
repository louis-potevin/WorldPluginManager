package fr.phundir.wplmanager.command;

import fr.phundir.wplmanager.config.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Root {@code /wpm} command: dispatches to subcommands and handles their tab completion.
 */
public final class WpmCommand implements TabExecutor {
    public static final String LABEL = "wpm";
    private static final String HELP = "help";

    private final Messages messages;
    private final List<SubCommand> subCommands = new ArrayList<>();
    private final Map<String, SubCommand> byLabel = new HashMap<>();

    public WpmCommand(Messages messages) {
        this.messages = messages;
    }

    public void register(SubCommand subCommand) {
        subCommands.add(subCommand);
        byLabel.put(subCommand.getName(), subCommand);
        for (String alias : subCommand.getAliases()) {
            byLabel.put(alias, subCommand);
        }
    }

    /** Subcommands the sender has permission for, in registration order. */
    public List<SubCommand> usableBy(CommandSender sender) {
        return subCommands.stream().filter(sub -> sub.canUse(sender)).collect(Collectors.toList());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            dispatch(sender, HELP, args);
        } else {
            dispatch(sender, args[0], Arrays.copyOfRange(args, 1, args.length));
        }
        return true;
    }

    /** Runs a subcommand by name or alias; also used by the legacy commands. */
    public void dispatch(CommandSender sender, String subLabel, String[] args) {
        SubCommand sub = find(subLabel);
        if (sub == null) {
            messages.send(sender, "unknown_subcommand", "subcommand", subLabel);
            return;
        }
        if (!sub.canUse(sender)) {
            messages.send(sender, "no_permission");
            return;
        }
        if (!sub.acceptsArgCount(args.length)) {
            messages.send(sender, "usage", "usage", sub.usage());
            return;
        }
        sub.execute(sender, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length <= 1) {
            List<String> names = usableBy(sender).stream().map(SubCommand::getName).collect(Collectors.toList());
            return matching(args.length == 0 ? "" : args[0], names);
        }
        return complete(sender, args[0], Arrays.copyOfRange(args, 1, args.length));
    }

    /** Tab completion of a subcommand's arguments; also used by the legacy commands. */
    public List<String> complete(CommandSender sender, String subLabel, String[] args) {
        SubCommand sub = find(subLabel);
        if (sub == null || args.length == 0 || args.length > sub.getMaxArgs() || !sub.canUse(sender)) {
            return Collections.emptyList();
        }
        return matching(args[args.length - 1], sub.complete(sender, args));
    }

    private SubCommand find(String label) {
        return byLabel.get(label.toLowerCase(Locale.ENGLISH));
    }

    /** Candidates starting with {@code typed} (ignoring case), sorted alphabetically. */
    private static List<String> matching(String typed, List<String> candidates) {
        List<String> matches = StringUtil.copyPartialMatches(typed, candidates, new ArrayList<String>());
        matches.sort(String.CASE_INSENSITIVE_ORDER);
        return matches;
    }
}
