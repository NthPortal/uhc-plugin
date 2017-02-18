package com.github.nthportal.uhc.commands;

import com.github.nthportal.uhc.core.Config;
import com.github.nthportal.uhc.core.UHCPlugin;
import com.google.common.base.Joiner;
import lombok.val;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.*;

public class ConfCommandExecutor implements CommandExecutor {
    public static final String NAME = "uhc-conf";
    public static final String PERMISSION = "uhc-plugin.uhc-conf";

    private static final int PAGE_ENTRIES = 8;

    private final UHCPlugin plugin;

    public ConfCommandExecutor(UHCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] strings) {
        if ((strings.length == 0) || !commandSender.hasPermission(PERMISSION)) {
            return false;
        }

        switch (strings[0].toLowerCase()) {
            case Opts.RELOAD:
                plugin.reloadConfig();
                commandSender.sendMessage("Reloaded configuration from file");
                break;
            case Opts.HELP:
                doHelp(commandSender, command);
                break;
            case Opts.ON:
                return onEventCommand(commandSender, Arrays.copyOfRange(strings, 1, strings.length));
            default:
                commandSender.sendMessage("Invalid sub-command: " + strings[0]);
                return false;
        }
        return true;
    }

    private void doHelp(CommandSender commandSender, Command command) {
        val name = command.getName();
        commandSender.sendMessage("-------- " + name + " help --------");
        commandSender.sendMessage("/" + name + " " + Opts.RELOAD + " - reloads configuration from file");
        commandSender.sendMessage("/" + name + " " + Opts.ON + " {EVENT} " + Opts.ADD
                + " <first|last|INDEX> <COMMAND> - adds a command to be run first/last or at"
                + " a specified index relative to other commands already set for an event");
        commandSender.sendMessage("/" + name + " " + Opts.ON + " {EVENT} " + Opts.REMOVE
                + " <INDEX> - removes the command set at the specified index for an event");
        commandSender.sendMessage("/" + name + " " + Opts.ON + " {EVENT} " + Opts.CLEAR
                + " - clears the commands set for an event");
        commandSender.sendMessage("/" + name + " " + Opts.ON + " {EVENT} " + Opts.LIST
                + " [PAGE] - lists the first or a specified page of commands set for an event"
                + " (Note: commands are run in the order listed)");
        commandSender.sendMessage("/" + name + " " + Opts.ON + " {EVENT} " + Opts.MOVE
                + " <TARGET INDEX> <DESTINATION INDEX> - moves the command at the target index to"
                + " the position before the destination index in the list of commands set for an event");
        commandSender.sendMessage("Valid events ({EVENT}) are: start, stop, pause, resume, countdownStart,"
                + " countdownMark, epStart, epEnd, death (\"ep\" is short for \"episode\")");
        commandSender.sendMessage("-------- " + name + " help --------");
    }

    private boolean onEventCommand(CommandSender commandSender, String[] args) {
        if (args.length < 2) {
            commandSender.sendMessage("Insufficient arguments for sub-command: " + Opts.ON);
            return false;
        }

        String event = args[0].toLowerCase();
        if (!Events.map.containsKey(event)) {
            commandSender.sendMessage("Invalid sub-command: " + args[0]);
            return false;
        }
        val onEvent = Events.map.get(event);

        switch (args[1].toLowerCase()) {
            case Opts.ADD:
                return doAdd(commandSender, onEvent, Arrays.copyOfRange(args, 2, args.length));
            case Opts.REMOVE:
                return doRemove(commandSender, onEvent, Arrays.copyOfRange(args, 2, args.length));
            case Opts.CLEAR:
                return doClear(commandSender, onEvent);
            case Opts.LIST:
                return doList(commandSender, onEvent, Arrays.copyOfRange(args, 2, args.length));
            case Opts.MOVE:
                return doMove(commandSender, onEvent, Arrays.copyOfRange(args, 2, args.length));
            default:
                commandSender.sendMessage("Invalid sub-command: " + args[1]);
                return false;
        }
    }

    private boolean doAdd(CommandSender commandSender, String event, String[] args) {
        if (args.length < 2) {
            commandSender.sendMessage("Insufficient arguments for sub-command: " + Opts.ADD);
            return false;
        }

        val location = args[0].toLowerCase();
        val commands = plugin.getConfig().getStringList(event);
        val newCommand = Joiner.on(" ").join(Arrays.copyOfRange(args, 1, args.length));

        switch (location) {
            case Opts.LAST:
                commands.add(newCommand);
                break;
            case Opts.FIRST:
                commands.add(0, newCommand);
                break;
            default:
                try {
                    val index = Integer.parseInt(location) - 1;
                    if ((index < 0) || (index > commands.size())) {
                        commandSender.sendMessage("Specified index out of range. Valid range: 1 - " + (commands.size() + 1));
                        return false;
                    }
                    commands.add(index, newCommand);
                } catch (NumberFormatException ignored) {
                    commandSender.sendMessage("Invalid argument for sub-command: " + args[0]);
                    return false;
                }
        }

        plugin.getConfig().set(event, commands);
        plugin.saveConfig();
        commandSender.sendMessage("Added command to run " + event);
        return true;
    }

    private boolean doRemove(CommandSender commandSender, String event, String[] args) {
        if (args.length < 1) {
            commandSender.sendMessage("Insufficient arguments for sub-command: " + Opts.REMOVE);
            return false;
        }

        List<String> commands = plugin.getConfig().getStringList(event);
        try {
            val index = Integer.parseInt(args[0]) - 1;
            if ((index < 0) || (index > commands.size())) {
                commandSender.sendMessage("Specified index out of range. Valid range: 1 - " + commands.size());
                return false;
            }
            commands.remove(index);
        } catch (NumberFormatException ignored) {
            commandSender.sendMessage("Invalid argument for sub-command: " + args[0]);
            return false;
        }

        plugin.getConfig().set(event, commands);
        plugin.saveConfig();
        commandSender.sendMessage("Removed command from running " + event);
        return true;
    }

    private boolean doClear(CommandSender commandSender, String event) {
        plugin.getConfig().set(event, Collections.<String>emptyList());
        plugin.saveConfig();
        commandSender.sendMessage("Cleared commands currently running " + event);
        return true;
    }

    private boolean doList(CommandSender commandSender, String event, String[] args) {
        val commands = plugin.getConfig().getStringList(event);

        val size = commands.size();
        if (size == 0) {
            commandSender.sendMessage("No commands set to run " + event);
            return true;
        }

        // pagination!
        int page = 0;
        int maxPage = (int) Math.ceil(size / (double) PAGE_ENTRIES);
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]) - 1;
                if ((page < 0) || (page >= maxPage)) {
                    commandSender.sendMessage("Specified page out of range. Valid range: 1 - " + maxPage);
                    return false;
                }
            } catch (NumberFormatException ignored) {
                commandSender.sendMessage("Invalid argument for sub-command: " + args[0]);
                return false;
            }
        }


        commandSender.sendMessage("---- " + event + " page: (" + (page + 1) + "/" + maxPage + ") ----");
        for (int i = (page * PAGE_ENTRIES); (i < Math.min((page + 1) * PAGE_ENTRIES, size)); i++) {
            commandSender.sendMessage((i + 1) + ")  " + commands.get(i));
        }
        commandSender.sendMessage("---- " + event + " total commands: " + size + " ----");
        return true;
    }

    private boolean doMove(CommandSender commandSender, String event, String[] args) {
        if (args.length < 2) {
            commandSender.sendMessage("Insufficient arguments for sub-command: " + Opts.MOVE);
            return false;
        }

        val commands = plugin.getConfig().getStringList(event);
        val size = commands.size();

        int target;
        int dest;

        try {
            target = Integer.parseInt(args[0]) - 1;
        } catch (NumberFormatException ignored) {
            commandSender.sendMessage("Invalid argument for sub-command: " + args[0]);
            return false;
        }
        try {
            dest = Integer.parseInt(args[1]) - 1;
        } catch (NumberFormatException ignored) {
            commandSender.sendMessage("Invalid argument for sub-command: " + args[1]);
            return false;
        }

        if ((target < 0) || (target > size) || (dest < 0) || (dest > size)) {
            commandSender.sendMessage("Index out of range: 1 - " + (size + 1));
            return false;
        }

        if (dest == target) {
            return true;
        }

        String tempCommand = commands.get(target);
        if (dest > target) {
            dest--;
        }
        commands.remove(target);
        commands.add(dest, tempCommand);

        plugin.getConfig().set(event, commands);
        plugin.saveConfig();
        commandSender.sendMessage("Moved command for running " + event);
        return true;
    }

    static class Opts {
        static final String RELOAD = "reload";
        static final String HELP = "help";
        static final String ON = "on";

        static final Set<String> main = new HashSet<>();

        static final String ADD = "add";
        static final String REMOVE = "remove";
        static final String CLEAR = "clear";
        static final String LIST = "list";
        static final String MOVE = "move";

        static final Set<String> secondary = new HashSet<>();

        static final String FIRST = "first";
        static final String LAST = "last";

        static final Set<String> locations = new HashSet<>();

        static {
            main.add(RELOAD);
            main.add(HELP);
            main.add(ON);

            secondary.add(ADD);
            secondary.add(REMOVE);
            secondary.add(CLEAR);
            secondary.add(LIST);
            secondary.add(MOVE);

            locations.add(FIRST);
            locations.add(LAST);
        }
    }

    static class Events {
        static final String COUNTDOWN_START = "countdownstart";
        static final String COUNTDOWN_MARK = "countdownmark";
        static final String START = "start";
        static final String STOP = "stop";
        static final String PAUSE = "pause";
        static final String RESUME = "resume";
        static final String EPISODE_START = "epstart";
        static final String EPISODE_END = "epend";
        static final String DEATH = "death";

        static final Map<String, String> map = new HashMap<>();

        static {
            map.put(COUNTDOWN_START, Config.Events.ON_COUNTDOWN_START);
            map.put(COUNTDOWN_MARK, Config.Events.ON_COUNTDOWN_MARK);
            map.put(START, Config.Events.ON_START);
            map.put(STOP, Config.Events.ON_STOP);
            map.put(PAUSE, Config.Events.ON_PAUSE);
            map.put(RESUME, Config.Events.ON_RESUME);
            map.put(EPISODE_START, Config.Events.ON_EPISODE_START);
            map.put(EPISODE_END, Config.Events.ON_EPISODE_END);
            map.put(DEATH, Config.Events.ON_DEATH);
        }
    }

}
