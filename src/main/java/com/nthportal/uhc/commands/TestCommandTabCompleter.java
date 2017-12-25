package com.nthportal.uhc.commands;

import com.nthportal.uhc.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TestCommandTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        switch (args.length) {
            case 1:
                return Util.filterAndSort(TestCommandExecutor.Events.all, args[0]);
            case 2:
                if (TestCommandExecutor.Events.acceptingPlayerArg.contains(args[0].toLowerCase())) {
                    return getPlayerNames(args[1].toLowerCase());
                }
                break;
            case 3:
                if (args[0].equalsIgnoreCase(TestCommandExecutor.Events.PVP_KILL)) {
                    return getPlayerNames(args[2].toLowerCase(),
                            name -> !name.equalsIgnoreCase(args[1]));
                }
                break;
        }
        return Collections.emptyList();
    }

    private static List<String> getPlayerNames(String nameStart) {
        return getPlayerNames(nameStart, name -> true);
    }

    private static List<String> getPlayerNames(String nameStart, Predicate<String> nameFilter) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(nameStart))
                .filter(nameFilter)
                .sorted()
                .collect(Collectors.toList());
    }
}
