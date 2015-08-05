package com.github.nthportal.uhc.commands;

import com.github.nthportal.uhc.UHCPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MainCommandExecutor implements CommandExecutor {
    public static final String NAME = "uhc";
    public static final String PERMISSION = "uhc-plugin.uhc";

    private final UHCPlugin plugin;

    public MainCommandExecutor(final UHCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] strings) {
        if ((strings.length == 0) || (strings.length > 1) || !commandSender.hasPermission(PERMISSION)) {
            return false;
        }

        boolean success;
        switch (strings[0].toLowerCase()) {
            case Opts.START:
                commandSender.sendMessage("Starting UHC...");
                success = plugin.timer.start();
                commandSender.sendMessage(success ? "Started UHC" : "Unable to start UHC - UHC paused or already running");
                break;
            case Opts.STOP:
                success = plugin.timer.stop();
                commandSender.sendMessage(success ? "Stopped UHC" : "Unable to stop UHC - UHC already stopped");
                break;
            case Opts.PAUSE:
                success = plugin.timer.pause();
                commandSender.sendMessage(success ? "Paused UHC" : "Unable to pause UHC - UHC not running or already paused");
                break;
            case Opts.RESUME:
                success = plugin.timer.resume();
                commandSender.sendMessage(success ? "Resumed UHC" : "Unable to resume UHC - UHC not paused");
                break;
            default:
                commandSender.sendMessage("Invalid sub-command: " + strings[0]);
                return false;
        }
        return true;
    }

    private static class Opts {
        static final String START = "start";
        static final String STOP = "stop";
        static final String PAUSE = "pause";
        static final String RESUME = "resume";
    }
}
