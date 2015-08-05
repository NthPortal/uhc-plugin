package com.github.nthportal.uhc.commands;

import com.github.nthportal.uhc.UHCPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public class ConfCommandTabCompleter implements TabCompleter {
    private final UHCPlugin plugin;

    public ConfCommandTabCompleter(UHCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null; // TODO implement
    }
}
