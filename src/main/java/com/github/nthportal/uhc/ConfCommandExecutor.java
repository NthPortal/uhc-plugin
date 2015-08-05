package com.github.nthportal.uhc;

import com.google.common.base.Joiner;
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
                commandSender.sendMessage("Help message placeholder"); // TODO replace with actual help message
                break;
            case Opts.ON:
                return onEventCommand(commandSender, Arrays.copyOfRange(strings, 1, strings.length));
            default:
                commandSender.sendMessage("Invalid sub-command: " + strings[0]);
                return false;
        }
        return true;
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
        String onEvent = Events.map.get(event);

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
                commandSender.sendMessage("Invalid sub-command: " + args[0]);
                return false;
        }
    }

    private boolean doAdd(CommandSender commandSender, String event, String[] args) {
        if (args.length < 2) {
            commandSender.sendMessage("Insufficient arguments for sub-command: " + Opts.ADD);
            return false;
        }

        String location = args[0].toLowerCase();
        List<String> commands = plugin.getConfig().getStringList(event);
        String newCommand = Joiner.on(" ").join(Arrays.copyOfRange(args, 1, args.length));

        switch (location) {
            case Opts.LAST:
                commands.add(newCommand);
                break;
            case Opts.FIRST:
                commands.add(0, newCommand);
                break;
            default:
                try {
                    int index = Integer.parseInt(location) - 1;
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
            int index = Integer.parseInt(args[0]) - 1;
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
        List<String> commands = plugin.getConfig().getStringList(event);

        int size = commands.size();
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

        List<String> commands = plugin.getConfig().getStringList(event);
        int size = commands.size();

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

    private static class Opts {
        static final String RELOAD = "reload";
        static final String HELP = "help";
        static final String ON = "on";

        static final String ADD = "add";
        static final String REMOVE = "remove";
        static final String CLEAR = "clear";
        static final String LIST = "list";
        static final String MOVE = "move";

        static final String FIRST = "first";
        static final String LAST = "last";
    }

    private static class Events {
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
