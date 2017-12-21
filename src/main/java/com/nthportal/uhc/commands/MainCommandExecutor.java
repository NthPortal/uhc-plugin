package com.nthportal.uhc.commands;

import com.nthportal.uhc.core.Timer;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.nthportal.uhc.util.MessageUtil.*;

@RequiredArgsConstructor
public class MainCommandExecutor implements CommandExecutor {
    public static final String NAME = "uhc";

    private final ExecutorService service = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder()
                    .setNameFormat("uhc-manager:timer-manager")
                    .build());

    private final Timer timer;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ((args.length == 0) || (args.length > 1) || !sender.hasPermission(Permissions.UHC)) {
            return false;
        }

        switch (args[0].toLowerCase()) {
            case Opts.START:
                service.submit(() -> sendStatus(sender, timer.start(),
                        "Started UHC",
                        "Unable to start UHC - UHC paused, in the middle of starting, or already running"));
                break;
            case Opts.STOP:
                service.submit(() -> sendStatus(sender, timer.stop(),
                        "Stopped UHC",
                        "Unable to stop UHC - UHC already stopped"));
                break;
            case Opts.PAUSE:
                service.submit(() -> sendStatus(sender, timer.pause(), "Paused UHC", "Unable to pause UHC - UHC not running, in the middle of starting, or already paused"));
                break;
            case Opts.RESUME:
                service.submit(() -> sendStatus(sender, timer.resume(), "Resumed UHC", "Unable to resume UHC - UHC not paused"));
                break;
            default:
                sendError(sender, "Invalid sub-command: " + args[0]);
                return false;
        }
        return true;
    }

    private static void sendStatus(CommandSender sender, boolean condition, String onSuccess, String onFailure) {
        if (condition) {
            sendSuccess(sender, onSuccess);
        } else {
            sendError(sender, onFailure);
        }
    }

    static class Opts {
        static final String START = "start";
        static final String STOP = "stop";
        static final String PAUSE = "pause";
        static final String RESUME = "resume";

        static final Set<String> all = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                START,
                STOP,
                PAUSE,
                RESUME
        )));
    }
}
