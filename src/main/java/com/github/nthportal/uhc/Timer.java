package com.github.nthportal.uhc;

import com.google.common.base.Function;
import org.bukkit.Server;
import org.bukkit.command.CommandException;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

public class Timer {
    private final UHCPlugin plugin;
    private final FileConfiguration config;
    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    private Future<?> scheduleFuture;
    private State state = State.STOPPED;
    private int interval;
    private int episode = 0;
    private long originalStartTime;
    private long effectiveStartTime;
    private long elapsedTime = 0;
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

    public Timer(UHCPlugin plugin) {
        this.plugin = plugin;
        config = plugin.config;
    }

    public boolean start() {
        lock.writeLock().lock();
        try {
            if (state != State.STOPPED) {
                return false;
            }

            interval = config.getInt(Config.EPISODE_TIME);
            countdown();
            originalStartTime = System.currentTimeMillis();
            scheduleFuture = service.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    doEpisodeMarker();
                }
            }, interval, interval, TimeUnit.MINUTES);
            effectiveStartTime = originalStartTime;
            episode = 1;

            onStart();
            onEpisodeStart(); // TODO maybe remove?
            state = State.RUNNING;
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public synchronized boolean stop() {
        lock.writeLock().lock();
        try {
            if (state == State.STOPPED) {
                return false;
            }

            scheduleFuture.cancel(true);
            episode = 0;
            originalStartTime = 0;
            effectiveStartTime = 0;
            elapsedTime = 0;

            onStop();
            state = State.STOPPED;
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public synchronized boolean pause() {
        lock.writeLock().lock();
        try {
            if (state != State.RUNNING) {
                return false;
            }

            long currentTime = System.currentTimeMillis();
            elapsedTime = currentTime - effectiveStartTime;
            scheduleFuture.cancel(true);

            onPause();
            state = State.PAUSED;
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public synchronized boolean resume() {
        lock.writeLock().lock();
        try {
            if (state != State.PAUSED) {
                return false;
            }

            long intervalInMillis = TimeUnit.MINUTES.toMillis(interval);
            long timeUntilNextEpisode = intervalInMillis - (elapsedTime % intervalInMillis);
            scheduleFuture = service.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    doEpisodeMarker();
                }
            }, timeUntilNextEpisode, intervalInMillis, TimeUnit.MILLISECONDS);
            effectiveStartTime = System.currentTimeMillis() - elapsedTime;
            elapsedTime = 0;

            onResume();
            state = State.RUNNING;
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public State getState() {
        lock.readLock().lock();
        try {
            return state;
        } finally {
            lock.readLock().unlock();
        }
    }

    public long getOriginalStartTime() {
        lock.readLock().lock();
        try {
            return originalStartTime;
        } finally {
            lock.readLock().unlock();
        }
    }

    public long getEffectiveStartTime() {
        lock.readLock().lock();
        try {
            return effectiveStartTime;
        } finally {
            lock.readLock().unlock();
        }
    }

    private void countdown() {
        onCountdownStart();
        int countdownFrom = config.getInt(Config.COUNTDOWN_FROM);
        for (int i = 0; i < countdownFrom; i++) {
            onCountdownMark(countdownFrom - i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                plugin.logger.log(Level.WARNING, "Sleep interruption in UHC countdown", e);
            }
        }
    }

    private void doEpisodeMarker() {
        lock.readLock().lock();
        try {
            onEpisodeEnd();
            episode++;
            onEpisodeStart();
        } finally {
            lock.readLock().unlock();
        }
    }

    // Event handling stuff

    private void onStart() {
        executeCommands(Config.Events.ON_START);
    }

    private void onStop() {
        executeCommands(Config.Events.ON_STOP);
    }

    private void onPause() {
        executeCommands(Config.Events.ON_PAUSE);
    }

    private void onResume() {
        executeCommands(Config.Events.ON_RESUME);
    }

    private void onEpisodeStart() {
        final int minutes = interval * (episode - 1);
        List<Function<String, String>> replacements = new ArrayList<>();
        replacements.add(new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace(Replacements.EPISODE, String.valueOf(episode));
            }
        });
        replacements.add(new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace(Replacements.MINUTES, String.valueOf(minutes));
            }
        });
        executeCommands(Config.Events.ON_EPISODE_START, replacements);
    }

    private void onEpisodeEnd() {
        final int minutes = interval * episode;
        List<Function<String, String>> replacements = new ArrayList<>();
        replacements.add(new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace(Replacements.EPISODE, String.valueOf(episode));
            }
        });
        replacements.add(new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace(Replacements.MINUTES, String.valueOf(minutes));
            }
        });
        executeCommands(Config.Events.ON_EPISODE_END, replacements);
    }

    private void onCountdownStart() {
        executeCommands(Config.Events.ON_COUNTDOWN_START);
    }

    private void onCountdownMark(final int mark) {
        List<Function<String, String>> replacements = new ArrayList<>();
        replacements.add(new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace(Replacements.COUNTDOWN_MARK, String.valueOf(mark));
            }
        });
        executeCommands(Config.Events.ON_COUNTDOWN_MARK, replacements);
    }

    private void executeCommands(String event) {
        executeCommands(event, Collections.<Function<String, String>>emptyList());
    }

    private void executeCommands(String event, List<Function<String, String>> replaceFunctions) {
        List<String> commands = config.getStringList(event);
        for (String command : commands) {
            for (Function<String, String> function : replaceFunctions) {
                command = function.apply(command);
            }
            // Execute command
            Server server = plugin.getServer();
            try {
                server.dispatchCommand(server.getConsoleSender(), command);
            }
            // Why is this a RuntimeException?
            // It's not in my control if someone else doesn't know how to code their command executor
            catch (CommandException e) {
                plugin.logger.log(Level.WARNING, "Exception running command: " + command, e);
            }
        }
    }

    public enum State {
        STOPPED, RUNNING, PAUSED
    }

    private static class Replacements {
        public static final String MINUTES = "{{minutes}}";
        public static final String EPISODE = "{{episode}}";
        public static final String COUNTDOWN_MARK = "{{mark}}";
    }
}
