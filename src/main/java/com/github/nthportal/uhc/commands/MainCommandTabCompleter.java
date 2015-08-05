package com.github.nthportal.uhc.commands;

import com.github.nthportal.uhc.UHCPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainCommandTabCompleter implements TabCompleter {
    private final UHCPlugin plugin;

    public MainCommandTabCompleter(UHCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }

        List<String> list = new ArrayList<>();
        for (String s : MainCommandExecutor.Opts.set) {
            if (s.startsWith(args[0])) {
                list.add(s);
            }
        }
        return list;
    }
}
