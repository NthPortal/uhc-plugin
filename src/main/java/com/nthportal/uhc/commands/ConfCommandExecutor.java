package com.nthportal.uhc.commands;

import com.nthportal.uhc.core.Config;
import com.nthportal.uhc.core.UHCPlugin;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.*;

@RequiredArgsConstructor
public class ConfCommandExecutor implements CommandExecutor {
    public static final String NAME = "uhc-conf";

    private final UHCPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ((args.length == 0) || !sender.hasPermission(Permissions.CONFIGURE)) {
            return false;
        }

        switch (args[0].toLowerCase()) {
            case Opts.RELOAD:
                plugin.reloadConfig();
                sender.sendMessage("Reloaded configuration from file");
                break;
            case Opts.EPISODE_LENGTH:
                if (args.length == 1) {
                    printEpisodeLength(sender);
                } else {
                    val length = validateNumericArg(
                            sender,
                            Arrays.copyOfRange(args, 1, args.length),
                            Opts.EPISODE_LENGTH);
                    return length.isPresent() && updateEpisodeLength(sender, length.getAsInt());
                }
            case Opts.COUNTDOWN_FROM:
                if (args.length == 1) {
                    printCountdownFrom(sender);
                } else {
                    val countdownFrom = validateNumericArg(
                            sender,
                            Arrays.copyOfRange(args, 1, args.length),
                            Opts.COUNTDOWN_FROM);
                    return countdownFrom.isPresent() && updateCountdownFrom(sender, countdownFrom.getAsInt());
                }
            case Opts.HELP:
                doHelp(sender, command);
                break;
            default:
                sender.sendMessage("Invalid sub-command: " + args[0]);
                return false;
        }
        return true;
    }

    private void printEpisodeLength(CommandSender sender) {
        val episodeLength = plugin.getConfig().getInt(Opts.EPISODE_LENGTH);
        sender.sendMessage("UHC episode length is " + episodeLength + " minute(s)");
    }

    private void printCountdownFrom(CommandSender sender) {
        val countdownFrom = plugin.getConfig().getInt(Opts.COUNTDOWN_FROM);
        sender.sendMessage("UHCs count down from " + countdownFrom);
    }

    private OptionalInt validateNumericArg(CommandSender sender, String[] args, String subCommand) {
        if (args.length > 1) {
            sender.sendMessage("Too many arguments for sub-command: " + subCommand);
        } else {
            try {
                return OptionalInt.of(Integer.parseInt(args[0]));
            } catch (NumberFormatException ignored) {
                sender.sendMessage(args[0] + " is not a number");
            }
        }

        return OptionalInt.empty();
    }

    private boolean updateEpisodeLength(CommandSender sender, int lengthInMinutes) {
        if (lengthInMinutes <= 0) {
            sender.sendMessage("Episode length must be a positive number");
            return false;
        }

        plugin.getConfig().set(Config.EPISODE_LENGTH, lengthInMinutes);
        plugin.saveConfig();

        sender.sendMessage(new String[]{
                "Set UHC episode length to " + lengthInMinutes + " minute(s)",
                "New episode length will not be applied to a running UHC",
        });
        return true;
    }

    private boolean updateCountdownFrom(CommandSender sender, int countdownFrom) {
        if (countdownFrom < 0) {
            sender.sendMessage("Countdown cannot be from a negative number ('0' disables countdown)");
            return false;
        }

        plugin.getConfig().set(Config.COUNTDOWN_FROM, countdownFrom);
        plugin.saveConfig();

        sender.sendMessage("The next UHC will count down from " + countdownFrom);
        return true;
    }

    private void doHelp(CommandSender sender, Command command) {
        val name = command.getName();
        sender.sendMessage(new String[]{
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
