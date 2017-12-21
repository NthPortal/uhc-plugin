package com.nthportal.uhc.commands;

import com.nthportal.uhc.core.Config;
import com.nthportal.uhc.core.UHCPlugin;
import lombok.val;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.*;

public class ConfCommandExecutor implements CommandExecutor {
    public static final String NAME = "uhc-conf";
    public static final String PERMISSION = "uhc-manager.uhc-conf";

    private final UHCPlugin plugin;

    public ConfCommandExecutor(UHCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] strings) {
        if ((strings.length == 0) || !commandSender.hasPermission(PERMISSION)) {
            return false;
        }

        switch (strings[0].toLowerCase()) {
            case Opts.RELOAD:
                plugin.reloadConfig();
                commandSender.sendMessage("Reloaded configuration from file");
                break;
            case Opts.EPISODE_LENGTH:
                if (strings.length == 1) {
                    printEpisodeLength(commandSender);
                } else {
                    Integer length = validateNumericArg(
                            commandSender,
                            Arrays.copyOfRange(strings, 1, strings.length),
                            Opts.EPISODE_LENGTH);
                    return length != null && updateEpisodeLength(commandSender, length);
                }
            case Opts.COUNTDOWN_FROM:
                if (strings.length == 1) {
                    printCountdownFrom(commandSender);
                } else {
                    Integer countdownFrom = validateNumericArg(
                            commandSender,
                            Arrays.copyOfRange(strings, 1, strings.length),
                            Opts.COUNTDOWN_FROM);
                    return countdownFrom != null && updateCountdownFrom(commandSender, countdownFrom);
                }
            case Opts.HELP:
                doHelp(commandSender, command);
                break;
            default:
                commandSender.sendMessage("Invalid sub-command: " + strings[0]);
                return false;
        }
        return true;
    }

    private void printEpisodeLength(CommandSender commandSender) {
        val episodeLength = plugin.getConfig().getInt(Opts.EPISODE_LENGTH);
        commandSender.sendMessage("UHC episode length is " + episodeLength + " minute(s)");
    }

    private void printCountdownFrom(CommandSender commandSender) {
        val countdownFrom = plugin.getConfig().getInt(Opts.COUNTDOWN_FROM);
        commandSender.sendMessage("UHCs count down from " + countdownFrom);
    }

    private Integer validateNumericArg(CommandSender commandSender, String[] args, String subCommand) {
        if (args.length > 1) {
            commandSender.sendMessage("Too many arguments for sub-command: " + subCommand);
        } else {
            try {
                return Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
                commandSender.sendMessage(args[0] + " is not a number");
            }
        }

        return null;
    }

    private boolean updateEpisodeLength(CommandSender commandSender, int lengthInMinutes) {
        if (lengthInMinutes <= 0) {
            commandSender.sendMessage("Episode length must be a positive number");
            return false;
        }

        plugin.getConfig().set(Config.EPISODE_LENGTH, lengthInMinutes);
        plugin.saveConfig();

        commandSender.sendMessage(new String[] {
                "Set UHC episode length to " + lengthInMinutes + " minute(s)",
                "New episode length will not be applied to a running UHC",
        });
        return true;
    }

    private boolean updateCountdownFrom(CommandSender commandSender, int countdownFrom) {
        if (countdownFrom < 0) {
            commandSender.sendMessage("Countdown cannot be from a negative number ('0' disables countdown)");
            return false;
        }

        plugin.getConfig().set(Config.COUNTDOWN_FROM, countdownFrom);
        plugin.saveConfig();

        commandSender.sendMessage("The next UHC will count down from " + countdownFrom);
        return true;
    }

    private void doHelp(CommandSender commandSender, Command command) {
        val name = command.getName();
        commandSender.sendMessage(new String[] {
                "-------- " + name + " help --------",
                "/" + name + " " + Opts.RELOAD + " - reloads configuration from file",
                "/" + name + " " + Opts.EPISODE_LENGTH + " <LENGTH IN MINUTES> - sets the episode length",
                "/" + name + " " + Opts.COUNTDOWN_FROM + " <NUMBER> - sets the number from which the UHC countdown starts",
                "-------- " + name + " help --------"
                });
    }

    static class Opts {
        static final String RELOAD = "reload";
        static final String HELP = "help";
        static final String EPISODE_LENGTH = Config.EPISODE_LENGTH;
        static final String COUNTDOWN_FROM = Config.COUNTDOWN_FROM;

        static final Set<String> set = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                RELOAD,
                HELP,
                EPISODE_LENGTH,
                COUNTDOWN_FROM
        )));
    }
}
