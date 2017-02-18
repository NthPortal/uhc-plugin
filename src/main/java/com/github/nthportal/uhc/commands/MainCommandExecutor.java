package com.github.nthportal.uhc.commands;

import com.github.nthportal.uhc.core.Timer;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.AllArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@AllArgsConstructor
public class MainCommandExecutor implements CommandExecutor {
    public static final String NAME = "uhc";
    public static final String PERMISSION = "uhc-plugin.uhc";

    private static final ExecutorService SERVICE;

    static {
        SERVICE = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat("uhc-plugin-uhc-starter")
                        .build()
        );
    }

    private final Timer timer;

    @Override
    public boolean onCommand(final CommandSender commandSender, Command command, String label, String[] strings) {
        if ((strings.length == 0) || (strings.length > 1) || !commandSender.hasPermission(PERMISSION)) {
            return false;
        }

        boolean success;
        switch (strings[0].toLowerCase()) {
            case Opts.START:
                commandSender.sendMessage("Starting UHC...");
                SERVICE.submit(new Runnable() {
                    @Override
                    public void run() {
                        commandSender.sendMessage(timer.start() ? "Started UHC" : "Unable to start UHC - UHC paused or already running");
                    }
                });
                break;
            case Opts.STOP:
                success = timer.stop();
                commandSender.sendMessage(success ? "Stopped UHC" : "Unable to stop UHC - UHC already stopped");
                break;
            case Opts.PAUSE:
                success = timer.pause();
                commandSender.sendMessage(success ? "Paused UHC" : "Unable to pause UHC - UHC not running or already paused");
                break;
            case Opts.RESUME:
                success = timer.resume();
                commandSender.sendMessage(success ? "Resumed UHC" : "Unable to resume UHC - UHC not paused");
                break;
            default:
                commandSender.sendMessage("Invalid sub-command: " + strings[0]);
                return false;
        }
        return true;
    }

    static class Opts {
        static final String START = "start";
        static final String STOP = "stop";
        static final String PAUSE = "pause";
        static final String RESUME = "resume";

        static final Set<String> set = new HashSet<>();

        static {
            set.add(START);
            set.add(STOP);
            set.add(PAUSE);
            set.add(RESUME);
        }
    }
}
