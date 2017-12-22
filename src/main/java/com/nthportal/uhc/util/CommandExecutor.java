package com.nthportal.uhc.util;

import com.nthportal.uhc.core.Context;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.val;
import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
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

    public void executeEventCommands(String event, Collection<Function<String, String>> replacements) {
        val commands = context.plugin().getConfig().getStringList(event);
        val replacer = combineReplacements(replacements);

        commands.stream()
                .map(CommandExecutor::cleanUpCommand)
                .map(replacer)
                .forEach(this::executeCommand);
    }

    public <T> void executeMappedCommandsMatching(String event, T toMatch, Function<String, Optional<T>> fromString) {
        executeMappedCommandsMatching(event, toMatch, fromString, Collections.emptyList());
    }

    public <T> void executeMappedCommandsMatching(String event,
                                                  T toMatch,
                                                  Function<String, Optional<T>> fromString,
                                                  Collection<Function<String, String>> replacements) {
        val replacer = combineReplacements(replacements);
        val mapList = context.plugin().getConfig().getMapList(event);
        for (val map : mapList) {
            for (val entry : map.entrySet()) {
                val key = entry.getKey().toString();
                val command = entry.getValue().toString();
                fromString.apply(key).ifPresent(value -> {
                    if (toMatch.equals(value)) {
                        executeCommandWithReplacements(command, replacer);
                    }
                });
            }
        }
    }

    public void executeMappedCommandsMatchingInt(String event, int toMatch) {
        executeMappedCommandsMatchingInt(event, toMatch, Collections.emptyList());
    }

    public void executeMappedCommandsMatchingInt(String event,
                                                 int toMatch,
                                                 Collection<Function<String, String>> replacements) {
        executeMappedCommandsMatching(event, toMatch, s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                context.logger().log(Level.WARNING, event + " entries must have integer keys");
                return Optional.empty();
            }
        }, replacements);
    }

    public void executeMappedCommandsMatchingString(String event, String toMatch) {
        executeMappedCommandsMatchingString(event, toMatch, Collections.emptyList());
    }

    public void executeMappedCommandsMatchingString(String event,
                                                    String toMatch,
                                                    Collection<Function<String, String>> replacements) {
        executeMappedCommandsMatching(event, toMatch, Optional::of, replacements);
    }

    private void executeCommandWithReplacements(String command, Function<String, String> replacer) {
        executeCommand(replacer.apply(cleanUpCommand(command)));
    }

    private void executeCommand(String command) {
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

    public static Function<String, String> replacement(String target, String replacement) {
        return input -> input.replace(target, replacement);
    }

    private static Function<String, String> combineReplacements(Collection<Function<String, String>> replacements) {
        return replacements.stream().reduce(Function.identity(), Function::andThen);
    }

    private static String cleanUpCommand(String command) {
        return command.startsWith("/") ? command.substring(1) : command;
    }

    public static class ReplaceTargets {
        public static final String MINUTES = "{{minutes}}";
        public static final String EPISODE = "{{episode}}";
        public static final String COUNTDOWN_MARK = "{{mark}}";
        public static final String PLAYER = "{{player}}";
        public static final String KILLER = "{{killer}}";
        public static final String CORPSE = "{{corpse}}";
    }
}
