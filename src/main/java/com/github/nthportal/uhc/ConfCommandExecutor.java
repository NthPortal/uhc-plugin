package com.github.nthportal.uhc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ConfCommandExecutor implements CommandExecutor {
    public static final String NAME = "uhc-conf";
    public static final String PERMISSION = "uhc-plugin.uhc-conf";

    private final UHCPlugin plugin; // In case it's needed later

    public ConfCommandExecutor(UHCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        return false;
    }
}
