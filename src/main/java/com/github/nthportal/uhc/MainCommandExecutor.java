package com.github.nthportal.uhc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

public class MainCommandExecutor implements CommandExecutor {
    public static final String NAME = "uhc";
    public static final String PERMISSION = "uhc-plugin.uhc";

    private static final Map<String, SubCommand> subcommands = new HashMap<>();

    private final UHCPlugin plugin; // In case it's needed later

    public MainCommandExecutor(UHCPlugin plugin) {
        this.plugin = plugin;
    }

    static {
        subcommands.put(Opts.START, new SubCommand() {
            @Override
            public boolean execute(CommandSender commandSender, Command command, String label, String[] args) {
                return false; // TODO implement
            }
        });

        subcommands.put(Opts.STOP, new SubCommand() {
            @Override
            public boolean execute(CommandSender commandSender, Command command, String label, String[] args) {
                return false; // TODO implement
            }
        });

        //subcommands.add(PAUSE);
        //subcommands.add(RESUME);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] strings) {
        if ((strings.length == 0) || !commandSender.hasPermission(PERMISSION)) {
            return false;
        }

        if (subcommands.containsKey(strings[0].toLowerCase())) {
            commandSender.sendMessage("Pretend that did something!");
            return subcommands.get(strings[0]).execute(commandSender, command, label, strings);
        } else {
            commandSender.sendMessage("Invalid sub-command: " + strings[0]); // TODO probably remove
            return false;
        }
    }

    private static class Opts {
        static final String START = "start";
        static final String STOP = "stop";
        static final String PAUSE = "pause";
        static final String RESUME = "resume";
    }
}
