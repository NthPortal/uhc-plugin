package com.github.nthportal.uhc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public interface SubCommand {
    boolean execute(CommandSender commandSender, Command command, String label, String[] args);
}
