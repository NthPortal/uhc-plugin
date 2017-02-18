package com.github.nthportal.uhc.core;

import com.github.nthportal.uhc.events.*;
import com.github.nthportal.uhc.util.CommandUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.val;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Timer {
    private final Context context;
    private final ScheduledExecutorService service;
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final List<Future<?>> minuteFutures = new ArrayList<>();
    private Future<?> episodeFuture;
    private List<Map<?, ?>> minuteCommands;
    private State state = State.STOPPED;
    private int interval;
    private int episode = 0;
    private long originalStartTime;
    private long effectiveStartTime;
    private long elapsedTime = 0;

    Timer(Context context) {
        this.context = context;

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
            episodeFuture = service.scheduleAtFixedRate(this::doEpisodeMarker, interval, interval, TimeUnit.MINUTES);

            // Handle onMinute events
            minuteCommands = context.plugin().getConfig().getMapList(Config.Events.ON_MINUTE);
            minuteCommands = minuteCommands.stream()
                    .map(map -> map.entrySet().stream()
                            .filter(entry -> {
                                try {
                                    val key = entry.getKey().toString();
                                    val command = entry.getValue().toString();
                                    val min = Integer.parseInt(key);
                                    if (min <= 0) {
                                        context.logger().log(Level.WARNING, Config.Events.ON_MINUTE + " entries must have positive integer keys");
                                        return false;
                                    }
                                    val future = service.schedule(() -> CommandUtil.executeCommand(context, command), min, TimeUnit.MINUTES);
                                    minuteFutures.add(future);
                                } catch (NumberFormatException e) {
                                    context.logger().log(Level.WARNING, Config.Events.ON_MINUTE + " entries must have positive integer keys");
                                    return false;
                                }
                                return true;
                            })
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                    )
                    .filter(map -> !map.isEmpty())
                    .collect(Collectors.toList());

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
            for (val future : minuteFutures) {
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

            val currentTime = System.currentTimeMillis();
            elapsedTime = currentTime - effectiveStartTime;
            episodeFuture.cancel(true);
            for (val future : minuteFutures) {
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

            val intervalInMillis = TimeUnit.MINUTES.toMillis(interval);
            val timeUntilNextEpisode = intervalInMillis - (elapsedTime % intervalInMillis);

            effectiveStartTime = System.currentTimeMillis() - elapsedTime;
            episodeFuture = service.scheduleAtFixedRate(this::doEpisodeMarker, timeUntilNextEpisode, intervalInMillis, TimeUnit.MILLISECONDS);

            // Handle onMinute events
            minuteCommands = minuteCommands.stream()
                    .map(map -> map.entrySet().stream()
                            .filter(entry -> {
                                val key = entry.getKey().toString();
                                val command = entry.getValue().toString();
                                val min = Integer.parseInt(key);
                                val minutesInMillis = TimeUnit.MINUTES.toMillis(min);
                                if (minutesInMillis < elapsedTime) {
                                    return false;
                                }
                                val timeUntilMinute = minutesInMillis - elapsedTime;
                                val future = service.schedule(() -> CommandUtil.executeCommand(context, command), timeUntilMinute, TimeUnit.MILLISECONDS);
                                minuteFutures.add(future);
                                return true;
                            })
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                    )
                    .filter(map -> !map.isEmpty())
                    .collect(Collectors.toList());

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
        val plugin = context.plugin();

        int length = plugin.getConfig().getInt(Config.EPISODE_TIME);
        if (length <= 0) {
            length = Config.DEFAULT_EPISODE_TIME;
            plugin.getConfig().set(Config.EPISODE_TIME, length);
            plugin.saveConfig();
        }
        return length;
    }

    private void countdown() {
        val plugin = context.plugin();

        val countdownFrom = plugin.getConfig().getInt(Config.COUNTDOWN_FROM);
        onCountdownStart(countdownFrom);
        for (int mark = countdownFrom; mark > 0; mark--) {
            onCountdownMark(mark);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                context.logger().log(Level.WARNING, "Sleep interruption in UHC countdown", e);
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
        context.eventBus().post(new UHCStartEvent());
    }

    private void onStop() {
        context.eventBus().post(new UHCStopEvent());
    }

    private void onPause(long timeElapsed) {
        context.eventBus().post(new UHCPauseEvent(timeElapsed));
    }

    private void onResume(long timeElapsed) {
        context.eventBus().post(new UHCResumeEvent(timeElapsed));
    }

    private void onEpisodeStart() {
        context.eventBus().post(new UHCEpisodeStartEvent(episode, interval));
    }

    private void onEpisodeEnd() {
        context.eventBus().post(new UHCEpisodeEndEvent(episode, interval));
    }

    private void onCountdownStart(int countingFrom) {
        context.eventBus().post(new UHCCountdownStartEvent(countingFrom));
    }

    private void onCountdownMark(int mark) {
        context.eventBus().post(new UHCCountdownMarkEvent(mark));
    }

    public enum State {
        STOPPED, RUNNING, PAUSED
    }
}
