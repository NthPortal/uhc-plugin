package com.nthportal.uhc.commands;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.nthportal.uhc.core.Config;
import com.nthportal.uhc.core.Context;
import com.nthportal.uhc.events.*;
import com.nthportal.uhc.util.Util;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@RequiredArgsConstructor
public class TestCommandExecutor implements CommandExecutor {
    public static final String NAME = "uhc-test";

    private final Context context;
    private final ExecutorService service = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder()
                    .setNameFormat("uhc-manager:config-tester")
                    .build());
    private volatile boolean busy = false;

    @Synchronized
    private boolean submitTask(Runnable task) {
        if (busy) {
            return false;
        }

        busy = true;
        service.submit(() -> {
            try {
                task.run();
            } finally {
                busy = false;
            }
        });
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ((args.length == 0) || !sender.hasPermission(Permissions.CONFIGURE)) {
            return false;
        }

        val event = args[0].toLowerCase();
        if (Events.all.contains(event)) {
            context.plugin().reloadConfig();
            return doTest(sender, event, Util.arrayTail(args));
        } else {
            sender.sendMessage("Invalid event: " + args[0]);
            return false;
        }
    }

    private boolean doTest(CommandSender sender, String event, String[] args) {
        if (args.length > 1) {
            sender.sendMessage("Too many arguments");
            return false;
        }

        if (args.length == 1) {
            return doTestHasArg(sender, event, args[0]);
        } else {
            return doTestNoArgs(sender, event);
        }
    }

    private boolean doTestNoArgs(CommandSender sender, String event) {
        switch (event) {
            case Events.COUNTDOWN_START:
                doCountdownStart();
                break;
            case Events.FULL_COUNTDOWN:
                return doFullCountdown(sender);
            case Events.START:
                doStart();
                break;
            case Events.FULL_START:
                return doFullStart(sender);
            default:
                sender.sendMessage("Event '" + event + "' requires argument");
        }
        return true;
    }

    private boolean doTestHasArg(CommandSender sender, String event, String arg) {
        if (Events.acceptingNumericArg.contains(event)) {
            try {
                val number = Integer.parseInt(arg);
                return doTestNumericArg(sender, event, number);
            } catch (NumberFormatException ignored) {
                sender.sendMessage("Event '" + event + "' requires numeric argument");
                return false;
            }
        } else if (Events.acceptingPlayerArg.contains((event))) {
            val player = Bukkit.getPlayerExact(arg);
            if (player == null) {
                sender.sendMessage("Player not found: " + arg);
                return false;
            } else {
                return doTestPlayerArg(sender, event, player);
            }
        } else {
            return doTestStringArg(sender, event, arg);
        }
    }

    private boolean doTestNumericArg(CommandSender sender, String event, int number) {
        if (number <= 0) {
            sender.sendMessage("Number argument must be positive");
            return false;
        } else if (number > Short.MAX_VALUE) { // Sanity check
            sender.sendMessage("Number argument too large");
            return false;
        }

        switch (event) {
            case Events.COUNTDOWN_MARK:
                context.eventBus().post(new UHCCountdownMarkEvent(number));
                break;
            case Events.EPISODE_CHANGE:
                doEpisodeChange(number);
                break;
            case Events.MINUTE:
                context.eventBus().post(new UHCMinuteEvent(number));
                break;
            default:
                throw Util.impossible();
        }
        return true;
    }

    private boolean doTestPlayerArg(CommandSender sender, String event, Player player) {
        switch (event) {
            case Events.DEATH:
                context.eventBus().post(new UHCPlayerDeathEvent(player));
                break;
            default:
                throw Util.impossible();
        }
        return true;
    }

    private boolean doTestStringArg(CommandSender sender, String event, String arg) {
        sender.sendMessage("Event '" + event + "'does not take args");
        return false;
    }

    private void doCountdownStart() {
        context.eventBus().post(new UHCCountdownStartEvent(Config.getCountdownFrom(context)));
    }

    private boolean doFullCountdown(CommandSender sender) {
        val countdownFrom = Config.getCountdownFrom(context);
        if (countdownFrom == 0) {
            doCountdownStart();
            return true;
        } else {
            val success = submitTask(countdownTask(countdownFrom));
            if (!success) {
                alreadyTestingEvent(sender);
            }
            return success;
        }
    }

    private Runnable countdownTask(int countdownFrom) {
        val eventBus = context.eventBus();
        return () -> {
            eventBus.post(new UHCCountdownStartEvent(countdownFrom));
            int mark = countdownFrom;
            while (mark > 0) {
                eventBus.post(new UHCCountdownMarkEvent(mark));
                mark--;
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(1));
                } catch (InterruptedException e) {
                    context.logger().log(Level.WARNING, "interrupted countdown test", e);
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        };
    }

    private void alreadyTestingEvent(CommandSender sender) {
        sender.sendMessage("Already testing another event");
    }

    private void doStart() {
        val eventBus = context.eventBus();
        val length = context.plugin().getConfig().getInt(Config.EPISODE_LENGTH);
        eventBus.post(new UHCStartEvent());
        eventBus.post(new UHCEpisodeStartEvent(1, length));
    }

    private boolean doFullStart(CommandSender sender) {
        val countdownFrom = Config.getCountdownFrom(context);
        if (countdownFrom == 0) {
            doCountdownStart();
            doStart();
            return true;
        } else {
            val success = submitTask(() -> {
                countdownTask(countdownFrom).run();
                if (!Thread.currentThread().isInterrupted()) {
                    doStart();
                }
            });
            if (!success) {
                alreadyTestingEvent(sender);
            }
            return success;
        }
    }

    private void doEpisodeChange(int episodeNumber) {
        val eventBus = context.eventBus();
        val length = context.plugin().getConfig().getInt(Config.EPISODE_LENGTH);
        val minute = length * episodeNumber;
        eventBus.post(new UHCEpisodeEndEvent(episodeNumber, length));
        eventBus.post(new UHCEpisodeStartEvent(episodeNumber + 1, length));
        eventBus.post(new UHCMinuteEvent(minute));
    }

    static class Events {
        static final String COUNTDOWN_START = "countdown-start";
        static final String COUNTDOWN_MARK = "countdown-mark";
        static final String FULL_COUNTDOWN = "full-countdown";
        static final String START = "start";
        static final String FULL_START = "full-start";
        static final String EPISODE_CHANGE = "episode-change";
        static final String MINUTE = "minute";
        static final String DEATH = "death";

        static final Set<String> all = Util.unmodifiableSet(
                COUNTDOWN_START,
                COUNTDOWN_MARK,
                FULL_COUNTDOWN,
                START,
                FULL_START,
                EPISODE_CHANGE,
                MINUTE,
                DEATH
        );

        static final Set<String> acceptingNumericArg = Util.unmodifiableSet(
                COUNTDOWN_MARK,
                EPISODE_CHANGE,
                MINUTE
        );

        static final Set<String> acceptingPlayerArg = Util.unmodifiableSet(DEATH);
    }
}
