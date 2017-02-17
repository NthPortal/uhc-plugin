package com.github.nthportal.uhc.util;

import com.github.nthportal.uhc.UHCPlugin;
import com.google.common.base.Function;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.bukkit.Bukkit;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;

public class CommandUtil {
    private static final ExecutorService SERVICE;

    static {
        SERVICE = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat("uhc-plugin-cmd-executor")
                        .build()
        );
    }

    public static void executeEventCommands(UHCPlugin plugin, String event) {
        executeEventCommands(plugin, event, Collections.<Function<String, String>>emptyList());
    }

    public static void executeEventCommands(UHCPlugin plugin, String event, List<Function<String, String>> replaceFunctions) {
        List<String> commands = plugin.getConfig().getStringList(event);
        for (String command : commands) {
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            for (Function<String, String> function : replaceFunctions) {
                command = function.apply(command);
            }

            // Execute command
            executeCommand(plugin, command);
        }
    }

    public static void executeMappedCommandsMatching(UHCPlugin plugin, String event, int toMatch) {
        List<Map<?, ?>> mapList = plugin.getConfig().getMapList(event);
        for (Map<?, ?> map : mapList) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                try {
                    String key = entry.getKey().toString();
                    String command = entry.getValue().toString();
                    int num = Integer.parseInt(key);
                    if (num == toMatch) {
                        executeCommand(plugin, command);
                    }
                } catch (NumberFormatException e) {
                    plugin.logger.log(Level.WARNING, event + " entries must have integer keys");
                }
            }
        }
    }

    public static void executeCommand(final UHCPlugin plugin, final String command) {
        plugin.logger.log(Level.INFO, "Executing command: " + command);
        final Future<Void> future = Bukkit.getScheduler().callSyncMethod(plugin, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                return null;
            }
        });

        // Have something else log Exceptions, to help keep things going
        SERVICE.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    future.get();
                }
                catch (ExecutionException | InterruptedException e) {
                    plugin.logger.log(Level.WARNING, "Exception running command: " + command, e);
                }
            }
        });
    }

    public static Function<String, String> replacementFunction(final String target, final String replacement) {
        return new Function<String, String>() {
            @Override
            public String apply(String input) {
                return input.replace(target, replacement);
            }
        };
    }

    public static class ReplaceTargets {
        public static final String MINUTES = "{{minutes}}";
        public static final String EPISODE = "{{episode}}";
        public static final String COUNTDOWN_MARK = "{{mark}}";
        public static final String PLAYER = "{{player}}";
    }
}
