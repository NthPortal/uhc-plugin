package com.github.nthportal.uhc.commands;

import com.github.nthportal.uhc.util.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public class MainCommandTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }

        return Util.filterAndCollect(MainCommandExecutor.Opts.set, args[0]);
    }
}
