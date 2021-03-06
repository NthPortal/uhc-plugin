package com.github.nthportal.uhc.core;

import com.github.nthportal.uhc.UHCPlugin;
import com.github.nthportal.uhc.events.*;
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

            interval = getValidatedEpisodeLength();
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

    public boolean stop() {
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

    public boolean pause() {
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

            onPause(elapsedTime);
            state = State.PAUSED;
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean resume() {
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

            onResume(elapsedTime);

            elapsedTime = 0;

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

    private int getValidatedEpisodeLength() {
        int length = plugin.getConfig().getInt(Config.EPISODE_TIME);
        if (length <= 0) {
            length = Config.DEFAULT_EPISODE_TIME;
            plugin.getConfig().set(Config.EPISODE_TIME, length);
            plugin.saveConfig();
        }
        return length;
    }

    private void countdown() {
        int countdownFrom = plugin.getConfig().getInt(Config.COUNTDOWN_FROM);
        onCountdownStart(countdownFrom);
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
        plugin.eventBus.post(new UHCStartEvent());
    }

    private void onStop() {
        plugin.eventBus.post(new UHCStopEvent());
    }

    private void onPause(long timeElapsed) {
        plugin.eventBus.post(new UHCPauseEvent(timeElapsed));
    }

    private void onResume(long timeElapsed) {
        plugin.eventBus.post(new UHCResumeEvent(timeElapsed));
    }

    private void onEpisodeStart() {
        plugin.eventBus.post(new UHCEpisodeStartEvent(episode, interval));
    }

    private void onEpisodeEnd() {
        plugin.eventBus.post(new UHCEpisodeEndEvent(episode, interval));
    }

    private void onCountdownStart(int countingFrom) {
        plugin.eventBus.post(new UHCCountdownStartEvent(countingFrom));
    }

    private void onCountdownMark(int mark) {
        plugin.eventBus.post(new UHCCountdownMarkEvent(mark));
    }

    public enum State {
        STOPPED, RUNNING, PAUSED
    }
}
