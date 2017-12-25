package com.nthportal.uhc.commands;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.nthportal.uhc.core.Config;
import com.nthportal.uhc.core.Context;
import com.nthportal.uhc.core.Timer;
import com.nthportal.uhc.events.*;
import com.nthportal.uhc.util.Util;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.experimental.Accessors;
import lombok.val;
import org.apache.commons.lang.math.IntRange;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.nthportal.uhc.util.MessageUtil.*;

@RequiredArgsConstructor
public class TestCommandExecutor implements CommandExecutor {
    public static final String NAME = "uhc-test";

    private final Context context;
    private final Timer timer;
    private final ExecutorService service = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder()
                    .setNameFormat("uhc-manager:config-tester")
                    .build());
    private volatile boolean busy = false;

    private final Map<String, SubCommand> subCommands = Stream.of(
            new NoArgsSubCommand(Events.COUNTDOWN_START) {
                @Override
                void execute(CommandSender sender) {
                    doCountdownStart();
                }
            },
            new NoArgsSubCommand(Events.FULL_COUNTDOWN) {
                @Override
                void execute(CommandSender sender) {
                    doFullCountdown(sender);
                }
            },
            new NoArgsSubCommand(Events.START) {
                @Override
                void execute(CommandSender sender) {
                    doStart();
                }
            },
            new NoArgsSubCommand(Events.FULL_START) {
                @Override
                void execute(CommandSender sender) {
                    doFullStart(sender);
                }
            },
            new NoArgsSubCommand(Events.STOP) {
                @Override
                void execute(CommandSender sender) {
                    context.eventBus().post(new StopEvent());
                }
            },
            new NoArgsSubCommand(Events.PAUSE) {
                @Override
                void execute(CommandSender sender) {
                    context.eventBus().post(new PauseEvent(0));
                }
            },
            new NoArgsSubCommand(Events.RESUME) {
                @Override
                void execute(CommandSender sender) {
                    context.eventBus().post(new ResumeEvent(0));
                }
            },
            new SubCommand(Events.COUNTDOWN_MARK, new IntRange(1)) {
                @Override
                boolean execute(CommandSender sender, String[] args) {
                    OptionalInt opt = getNumericArg(sender, args[0]);
                    opt.ifPresent(num -> context.eventBus().post(new CountdownMarkEvent(num)));
                    return opt.isPresent();
                }
            },
            new SubCommand(Events.EPISODE_CHANGE, new IntRange(1)) {
                @Override
                boolean execute(CommandSender sender, String[] args) {
                    OptionalInt opt = getNumericArg(sender, args[0]);
                    opt.ifPresent(num -> doEpisodeChange(num));
                    return opt.isPresent();
                }
            },
            new SubCommand(Events.MINUTE, new IntRange(1)) {
                @Override
                boolean execute(CommandSender sender, String[] args) {
                    OptionalInt opt = getNumericArg(sender, args[0]);
                    opt.ifPresent(num -> context.eventBus().post(new MinuteEvent(num)));
                    return opt.isPresent();
                }
            },
            new SubCommand(Events.DEATH, new IntRange(0, 1)) {
                @Override
                boolean execute(CommandSender sender, String[] args) {
                    Optional<Player> opt =  (args.length == 1) ? getPlayerArg(sender, args[0]) : commandSenderAsPlayer(sender);
                    opt.ifPresent(player -> doPlayerDeath(player));
                    return opt.isPresent();
                }
            },
            new SubCommand(Events.PVP_KILL, new IntRange(1, 2)) {
                @Override
                boolean execute(CommandSender sender, String[] args) {
                    Optional<Player> killerOpt = (args.length == 2) ? getPlayerArg(sender, args[0]) : commandSenderAsPlayer(sender);
                    Optional<Player> valid = killerOpt.flatMap((Player killer) -> {
                        Optional<Player> corpseOpt = getPlayerArg(sender, args[args.length - 1])
                                .filter((Player corpse) -> {
                                    boolean same = killer.equals(corpse);
                                    if (same) {
                                        sendError(sender, "Killer and corpse cannot be the same player");
                                    }
                                    return !same;
                                });
                        corpseOpt.ifPresent(corpse -> doPlayerKill(killer, corpse));
                        return corpseOpt;
                    });
                    return valid.isPresent();
                }
            }
    ).collect(Collectors.toMap(sc -> sc.event, Function.identity()));

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
        if (missingPermission(sender, Permissions.CONFIGURE)) {
            return true;
        } else if (args.length == 0) {
            return false;
        }

        // Check permission to test UHC while in progress
        if (sender.hasPermission(Permissions.UHC) || timer.state() == Timer.State.STOPPED) {
            val event = args[0].toLowerCase();
            val subCommand = subCommands.get(event);
            if (subCommand == null) {
                sendError(sender, "Invalid event: " + args[0]);
            } else {
                // Check arg count
                val count = args.length - 1;
                if (count < subCommand.argCount.getMinimumInteger()) {
                    sendError(sender, "Insufficient arguments for event: " + event);
                } else if (count > subCommand.argCount.getMaximumInteger()) {
                    if (subCommand.argCount.getMaximumInteger() == 0) {
                        sendError(sender, "Event does not take arguments: " + event);
                    } else {
                        sendError(sender, "Too many arguments for event: " + event);
                    }
                } else {
                    return subCommand.execute(sender, Util.arrayTail(args));
                }
            }
            return false;
        } else {
            sendError(sender, "You don't have permission to run this command while a UHC is in progress");
            return true;
        }
    }

    private void doCountdownStart() {
        context.eventBus().post(new CountdownStartEvent(Config.getCountdownFrom(context)));
    }

    private void doFullCountdown(CommandSender sender) {
        val countdownFrom = Config.getCountdownFrom(context);
        if (countdownFrom == 0) {
            doCountdownStart();
        } else {
            val success = submitTask(countdownTask(countdownFrom));
            if (!success) {
                alreadyTestingEvent(sender);
            }
        }
    }

    private Runnable countdownTask(int countdownFrom) {
        val eventBus = context.eventBus();
        return () -> {
            eventBus.post(new CountdownStartEvent(countdownFrom));
            int mark = countdownFrom;
            while (mark > 0) {
                eventBus.post(new CountdownMarkEvent(mark));
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
        sendError(sender, "Already testing another extended event");
    }

    private void doStart() {
        val eventBus = context.eventBus();
        val length = Config.getValidatedEpisodeLength(context);
        eventBus.post(new StartEvent());
        eventBus.post(new EpisodeStartEvent(1, length));
    }

    private void doFullStart(CommandSender sender) {
        val countdownFrom = Config.getCountdownFrom(context);
        if (countdownFrom == 0) {
            doCountdownStart();
            doStart();
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
        }
    }

    private void doEpisodeChange(int episodeNumber) {
        val eventBus = context.eventBus();
        val length = Config.getValidatedEpisodeLength(context);
        val minute = length * episodeNumber;
        eventBus.post(new EpisodeEndEvent(episodeNumber, length));
        eventBus.post(new EpisodeStartEvent(episodeNumber + 1, length));
        eventBus.post(new MinuteEvent(minute));
    }

    private void doPlayerDeath(Player player) {
        context.eventBus().post(new PlayerDeathEvent(player));
    }

    private void doPlayerKill(Player killer, Player corpse) {
        context.eventBus().post(new PlayerDeathEvent(corpse));
        context.eventBus().post(new PlayerKillEvent(killer, corpse));
    }

    @RequiredArgsConstructor
    @Accessors(fluent = true)
    private abstract class SubCommand {
        private final String event;
        private final IntRange argCount;

        abstract boolean execute(CommandSender sender, String[] args);

        OptionalInt getNumericArg(CommandSender sender, String arg) {
            try {
                val number = Integer.parseInt(arg);
                if (number <= 0) {
                    sendError(sender, "Number argument must be positive");
                } else if (number > Short.MAX_VALUE) { // Sanity check
                    sendError(sender, "Number argument too large");
                } else {
                    return OptionalInt.of(number);
                }
            } catch (NumberFormatException ignored) {
                sendError(sender, "Event '" + event + "' requires numeric argument");
            }
            return OptionalInt.empty();
        }

        Optional<Player> getPlayerArg(CommandSender sender, String arg) {
            val player = Bukkit.getPlayerExact(arg);
            if (player == null) {
                sendError(sender, "Player not found: " + arg);
                return Optional.empty();
            } else {
                return Optional.of(player);
            }
        }

        Optional<Player> commandSenderAsPlayer(CommandSender sender) {
            if (sender instanceof Player) {
                return Optional.of((Player) sender);
            } else {
                return Optional.empty();
            }
        }
    }

    private abstract class NoArgsSubCommand extends SubCommand {
        NoArgsSubCommand(String event) {
            super(event, new IntRange(0));
        }

        @Override
        final boolean execute(CommandSender sender, String[] args) {
            execute(sender);
            return true;
        }

        abstract void execute(CommandSender sender);
    }

    static class Events {
        static final String COUNTDOWN_START = "countdown-start";
        static final String COUNTDOWN_MARK = "countdown-mark";
        static final String FULL_COUNTDOWN = "full-countdown";
        static final String START = "start";
        static final String FULL_START = "full-start";
        static final String STOP = "stop";
        static final String PAUSE = "pause";
        static final String RESUME = "resume";
        static final String EPISODE_CHANGE = "episode-change";
        static final String MINUTE = "minute";
        static final String DEATH = "death";
        static final String PVP_KILL = "pvp-kill";

        static final Set<String> all = Util.unmodifiableSet(
                COUNTDOWN_START,
                COUNTDOWN_MARK,
                FULL_COUNTDOWN,
                START,
                FULL_START,
                STOP,
                PAUSE,
                RESUME,
                EPISODE_CHANGE,
                MINUTE,
                DEATH,
                PVP_KILL
        );

        static final Set<String> acceptingPlayerArg = Util.unmodifiableSet(
                DEATH,
                PVP_KILL
        );
    }
}
