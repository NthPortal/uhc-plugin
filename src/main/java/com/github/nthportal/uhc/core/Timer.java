package com.github.nthportal.uhc.core;

import com.github.nthportal.uhc.UHCPlugin;
import com.github.nthportal.uhc.util.CommandUtil;
import com.google.common.base.Function;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

public class Timer {
    private final UHCPlugin plugin;
    private final ScheduledExecutorService service;
    private Future<?> episodeFuture;
    private final List<Future<?>> minuteFutures = new ArrayList<>();
    private List<Map<?, ?>> minuteCommands;
    private State state = State.STOPPED;
    private int interval;
    private int episode = 0;
    private long originalStartTime;
    private long effectiveStartTime;
    private long elapsedTime = 0;
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

    public Timer(UHCPlugin plugin) {
        this.plugin = plugin;

        service = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat("uhc-scheduler")
                        .build()
        );
    }

    public boolean start() {
        lock.writeLock().lock();
        try {
            if (state != State.STOPPED) {
                return false;
            }

            interval = plugin.getConfig().getInt(Config.EPISODE_TIME);
            countdown();
            originalStartTime = System.currentTimeMillis();
            episodeFuture = service.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    doEpisodeMarker();
                }
            }, interval, interval, TimeUnit.MINUTES);

            // Handle onMinute events
            minuteCommands = plugin.getConfig().getMapList(Config.Events.ON_MINUTE);
            for (Iterator<Map<?, ?>> i = minuteCommands.iterator(); i.hasNext(); ) {
                Map<?, ?> map = i.next();
                for (Iterator<? extends Map.Entry<?, ?>> j = map.entrySet().iterator(); j.hasNext(); ) {
                    Map.Entry<?, ?> entry = j.next();
                    try {
                        String key = entry.getKey().toString();
                        final String command = entry.getValue().toString();
                        int min = Integer.parseInt(key);
                        if (min <= 0) {
                            plugin.logger.log(Level.WARNING, Config.Events.ON_MINUTE + " entries must have positive integer keys");
                            j.remove();
                            break;
                        }
                        Future<?> future = service.schedule(new Runnable() {
                            @Override
                            public void run() {
                                CommandUtil.executeCommand(plugin, command);
                            }
                        }, min, TimeUnit.MINUTES);
                        minuteFutures.add(future);
                    } catch (NumberFormatException e) {
                        plugin.logger.log(Level.WARNING, Config.Events.ON_MINUTE + " entries must have positive integer keys");
                        j.remove();
                    }
                }
                if (map.isEmpty()) {
                    i.remove();
                }
            }

            effectiveStartTime = originalStartTime;
            episode = 1;

            onStart();
            onEpisodeStart();
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

            episodeFuture.cancel(true);
            for (Future<?> future : minuteFutures) {
                future.cancel(true);
            }
            minuteFutures.clear();
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
            episodeFuture.cancel(true);
            for (Future<?> future : minuteFutures) {
                future.cancel(true);
            }
            minuteFutures.clear();

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

            effectiveStartTime = System.currentTimeMillis() - elapsedTime;
            episodeFuture = service.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    doEpisodeMarker();
                }
            }, timeUntilNextEpisode, intervalInMillis, TimeUnit.MILLISECONDS);

            // Handle onMinute events
            for (Iterator<Map<?, ?>> i = minuteCommands.iterator(); i.hasNext(); ) {
                Map<?, ?> map = i.next();
                for (Iterator<? extends Map.Entry<?, ?>> j = map.entrySet().iterator(); j.hasNext(); ) {
                    Map.Entry<?, ?> entry = j.next();

                    String key = entry.getKey().toString();
                    final String command = entry.getValue().toString();
                    int min = Integer.parseInt(key);
                    long minutesInMillis = TimeUnit.MINUTES.toMillis(min);
                    if (minutesInMillis < elapsedTime) {
                        j.remove();
                        break;
                    }
                    long timeUntilMinute = minutesInMillis - elapsedTime;
                    Future<?> future = service.schedule(new Runnable() {
                        @Override
                        public void run() {
                            CommandUtil.executeCommand(plugin, command);
                        }
                    }, timeUntilMinute, TimeUnit.MILLISECONDS);
                    minuteFutures.add(future);
                }
                if (map.isEmpty()) {
                    i.remove();
                }
            }

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
        int countdownFrom = plugin.getConfig().getInt(Config.COUNTDOWN_FROM);
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
        CommandUtil.executeEventCommands(plugin, Config.Events.ON_START);
    }

    private void onStop() {
        CommandUtil.executeEventCommands(plugin, Config.Events.ON_STOP);
    }

    private void onPause() {
        CommandUtil.executeEventCommands(plugin, Config.Events.ON_PAUSE);
    }

    private void onResume() {
        CommandUtil.executeEventCommands(plugin, Config.Events.ON_RESUME);
    }

    private void onEpisodeStart() {
        final int minutes = interval * (episode - 1);
        List<Function<String, String>> replacements = new ArrayList<>();
        replacements.add(CommandUtil.replacementFunction(CommandUtil.ReplaceTargets.EPISODE, String.valueOf(episode)));
        replacements.add(CommandUtil.replacementFunction(CommandUtil.ReplaceTargets.MINUTES, String.valueOf(minutes)));
        CommandUtil.executeEventCommands(plugin, Config.Events.ON_EPISODE_START, replacements);

        // Run episode-specific commands
        CommandUtil.executeMappedCommandsMatching(plugin, Config.Events.ON_START_EP_NUM, episode);
    }

    private void onEpisodeEnd() {
        final int minutes = interval * episode;
        List<Function<String, String>> replacements = new ArrayList<>();
        replacements.add(CommandUtil.replacementFunction(CommandUtil.ReplaceTargets.EPISODE, String.valueOf(episode)));
        replacements.add(CommandUtil.replacementFunction(CommandUtil.ReplaceTargets.MINUTES, String.valueOf(minutes)));
        CommandUtil.executeEventCommands(plugin, Config.Events.ON_EPISODE_END, replacements);
    }

    private void onCountdownStart() {
        CommandUtil.executeEventCommands(plugin, Config.Events.ON_COUNTDOWN_START);
    }

    private void onCountdownMark(final int mark) {
        List<Function<String, String>> replacements = new ArrayList<>();
        replacements.add(CommandUtil.replacementFunction(CommandUtil.ReplaceTargets.COUNTDOWN_MARK, String.valueOf(mark)));
        CommandUtil.executeEventCommands(plugin, Config.Events.ON_COUNTDOWN_MARK, replacements);
    }

    public enum State {
        STOPPED, RUNNING, PAUSED
    }

}
