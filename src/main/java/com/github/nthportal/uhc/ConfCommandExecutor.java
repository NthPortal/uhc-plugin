package com.github.nthportal.uhc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ConfCommandExecutor implements CommandExecutor {
    public static final String NAME = "uhc-conf";
    public static final String PERMISSION = "uhc-plugin.uhc-conf";

    private final UHCPlugin plugin;

    public ConfCommandExecutor(UHCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if ((strings.length == 0) || !commandSender.hasPermission(PERMISSION)) {
            return false;
        }

        switch (strings[0].toLowerCase()) {
            case Opts.RELOAD:
                plugin.reloadConfig();
                commandSender.sendMessage("Reloaded configuration from file");
                return true;
            case Opts.HELP:
                commandSender.sendMessage("Help message"); // TODO replace with actual help message
                return true;
            default:
                // TODO handle events or something
                commandSender.sendMessage("Invalid sub-command: " + strings[0]);
                return false;
        }
        //return true;
    }

    private class Opts {
        static final String RELOAD = "reload";
        static final String HELP = "help";
    }

}
