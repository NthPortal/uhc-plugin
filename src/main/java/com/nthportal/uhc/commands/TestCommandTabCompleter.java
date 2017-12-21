package com.nthportal.uhc.commands;

import com.nthportal.uhc.util.Util;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TestCommandTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        switch (args.length) {
            case 1:
                return Util.filterAndSort(TestCommandExecutor.Events.all, args[0]);
            case 2:
                if (args[0].equalsIgnoreCase(TestCommandExecutor.Events.DEATH)) {
                    val nameStart = args[1].toLowerCase();
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(nameStart))
                            .sorted()
                            .collect(Collectors.toList());
                }
        }
        return Collections.emptyList();
    }
}
