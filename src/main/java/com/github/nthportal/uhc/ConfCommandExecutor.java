package com.github.nthportal.uhc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ConfCommandExecutor implements CommandExecutor {
    public static final String NAME = "uhc-conf";
    public static final String PERMISSION = "uhcplugin.uhc-conf";

    private final UHCPlugin plugin; // In case it's needed later

    public ConfCommandExecutor(UHCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        return false;
    }

    private static class Opts {
        static final String ON_START = "onStart";
        static final String ON_STOP = "onStop";
        static final String ON_PAUSE = "onPause";
        static final String ON_RESUME = "onResume";
    }
}
