package com.nthportal.uhc.commands;

import com.nthportal.uhc.core.Timer;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
public class MainCommandExecutor implements CommandExecutor {
    public static final String NAME = "uhc";
    public static final String PERMISSION = "uhc-manager.uhc";

    private final ExecutorService service = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder()
                    .setNameFormat("uhc-plugin-uhc-starter")
                    .build());

    private final Timer timer;

    @Override
    public boolean onCommand(final CommandSender commandSender, Command command, String label, String[] strings) {
        if ((strings.length == 0) || (strings.length > 1) || !commandSender.hasPermission(PERMISSION)) {
            return false;
        }

        switch (strings[0].toLowerCase()) {
            case Opts.START:
                service.submit(() -> commandSender.sendMessage(timer.start() ? "Started UHC" : "Unable to start UHC - UHC paused, in the middle of starting, or already running"));
                break;
            case Opts.STOP:
                service.submit(() -> commandSender.sendMessage(timer.stop() ? "Stopped UHC" : "Unable to stop UHC - UHC already stopped"));
                break;
            case Opts.PAUSE:
                service.submit(() -> commandSender.sendMessage(timer.pause() ? "Paused UHC" : "Unable to pause UHC - UHC not running, in the middle of starting, or already paused"));
                break;
            case Opts.RESUME:
                service.submit(() -> commandSender.sendMessage(timer.resume() ? "Resumed UHC" : "Unable to resume UHC - UHC not paused"));
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
