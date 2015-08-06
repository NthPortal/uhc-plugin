package com.github.nthportal.uhc.commands;

import com.github.nthportal.uhc.util.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfCommandTabCompleter implements TabCompleter {
    private static final Set<String> events = new HashSet<>();

    static {
        events.add("countdownStart");
        events.add("countdownMark");
        events.add(ConfCommandExecutor.Events.START);
        events.add(ConfCommandExecutor.Events.STOP);
        events.add(ConfCommandExecutor.Events.PAUSE);
        events.add(ConfCommandExecutor.Events.RESUME);
        events.add("epStart");
        events.add("epEnd");
        events.add(ConfCommandExecutor.Events.DEATH);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        switch (args.length) {
            case 1:
                return Util.filterAndCollect(ConfCommandExecutor.Opts.main, args[0]);
            case 2:
                if (condition2(args)) {
                    return Util.filterAndCollect(events, args[1]);
                }
                break;
            case 3:
                if (condition2(args) && condition3(args)) {
                    return Util.filterAndCollect(ConfCommandExecutor.Opts.secondary, args[2]);
                }
                break;
            case 4:
                if (condition2(args) && condition3(args) && condition4(args)) {
                    return Util.filterAndCollect(ConfCommandExecutor.Opts.locations, args[3]);
                }
        }
        return Collections.emptyList();
    }

    private static boolean condition2(String[] args) {
        return args[0].equalsIgnoreCase(ConfCommandExecutor.Opts.ON);
    }

    private static boolean condition3(String[] args) {
        return ConfCommandExecutor.Events.map.containsKey(args[1].toLowerCase());
    }

    private static boolean condition4(String[] args) {
        return args[2].equalsIgnoreCase(ConfCommandExecutor.Opts.ADD);
    }
}
