package com.github.nthportal.uhc.util;

import com.github.nthportal.uhc.core.Context;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.val;
import org.bukkit.Bukkit;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
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

    public static void executeEventCommands(Context context, String event) {
        executeEventCommands(context, event, Collections.emptyList());
    }

    public static void executeEventCommands(Context context, String event, List<Function<String, String>> replaceFunctions) {
        val commands = context.plugin().getConfig().getStringList(event);
        val replacer = replaceFunctions.stream().reduce(Function.identity(), Function::andThen);

        commands.stream()
                .map(command -> command.startsWith("/") ? command.substring(1) : command)
                .map(replacer)
                .forEach(command -> executeCommand(context, command));
    }

    public static void executeMappedCommandsMatching(Context context, String event, int toMatch) {
        val mapList = context.plugin().getConfig().getMapList(event);
        for (val map : mapList) {
            for (val entry : map.entrySet()) {
                try {
                    val key = entry.getKey().toString();
                    val command = entry.getValue().toString();
                    val num = Integer.parseInt(key);
                    if (num == toMatch) {
                        executeCommand(context, command);
                    }
                } catch (NumberFormatException e) {
                    context.logger().log(Level.WARNING, event + " entries must have integer keys");
                }
            }
        }
    }

    public static void executeCommand(final Context context, final String command) {
        context.logger().log(Level.INFO, "Executing command: " + command);
        val future = Bukkit.getScheduler().callSyncMethod(context.plugin(), () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            return null;
        });

        // Have something else log Exceptions, to help keep things going
        SERVICE.submit(() -> {
            try {
                future.get();
            } catch (ExecutionException | InterruptedException e) {
                context.logger().log(Level.WARNING, "Exception running command: " + command, e);
            }
        });
    }

    public static Function<String, String> replacementFunction(final String target, final String replacement) {
        return input -> input.replace(target, replacement);
    }

    public static class ReplaceTargets {
        public static final String MINUTES = "{{minutes}}";
        public static final String EPISODE = "{{episode}}";
        public static final String COUNTDOWN_MARK = "{{mark}}";
        public static final String PLAYER = "{{player}}";
    }
}
