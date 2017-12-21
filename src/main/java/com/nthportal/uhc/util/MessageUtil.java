package com.nthportal.uhc.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class MessageUtil {
    private MessageUtil() {}

    public static void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.GREEN + message);
    }

    public static void sendWarning(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.YELLOW + message);
    }

    public static void sendError(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.RED + message);
    }

    public static boolean missingPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        } else {
            sendError(sender, "You don't have permission to run this command");
            return false;
        }
    }
}
