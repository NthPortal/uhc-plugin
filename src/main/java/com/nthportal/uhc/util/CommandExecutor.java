package com.nthportal.uhc.util;

import com.nthportal.uhc.core.Context;
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

public class CommandExecutor {
    private final ExecutorService service;
    private final Context context;

    public CommandExecutor(Context context) {
        service = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat("uhc-manager:cmd-executor")
                        .build()
        );
        this.context = context;
    }

    public void executeEventCommands(String event) {
        executeEventCommands(event, Collections.emptyList());
    }

    public void executeEventCommands(String event, List<Function<String, String>> replacements) {
        val commands = context.plugin().getConfig().getStringList(event);
        val replacer = replacements.stream().reduce(Function.identity(), Function::andThen);

        commands.stream()
                .map(command -> command.startsWith("/") ? command.substring(1) : command)
                .map(replacer)
                .forEach(this::executeCommand);
    }

    public void executeMappedCommandsMatching(String event, int toMatch) {
        val mapList = context.plugin().getConfig().getMapList(event);
        for (val map : mapList) {
            for (val entry : map.entrySet()) {
                try {
                    val key = entry.getKey().toString();
                    val command = entry.getValue().toString();
                    val num = Integer.parseInt(key);
                    if (num == toMatch) {
                        executeCommand(command);
                    }
                } catch (NumberFormatException e) {
                    context.logger().log(Level.WARNING, event + " entries must have integer keys");
                }
            }
        }
    }

    public void executeCommand(final String command) {
        context.logger().log(Level.INFO, "Executing command: " + command);
        val future = Bukkit.getScheduler().callSyncMethod(context.plugin(), () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            return null;
        });

        // Have something else log Exceptions, to help keep things going
        service.submit(() -> {
            try {
                future.get();
            } catch (ExecutionException | InterruptedException e) {
                context.logger().log(Level.WARNING, "Exception running command: " + command, e);
            }
        });
    }

    public static Function<String, String> replacement(final String target, final String replacement) {
        return input -> input.replace(target, replacement);
    }

    public static class ReplaceTargets {
        public static final String MINUTES = "{{minutes}}";
        public static final String EPISODE = "{{episode}}";
        public static final String COUNTDOWN_MARK = "{{mark}}";
        public static final String PLAYER = "{{player}}";
    }
}
