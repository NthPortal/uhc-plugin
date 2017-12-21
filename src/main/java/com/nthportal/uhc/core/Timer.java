package com.nthportal.uhc.core;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.nthportal.uhc.events.*;
import lombok.Builder;
import lombok.Synchronized;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.val;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class Timer {
    private final Context context;
    private final ScheduledExecutorService service;
    private AtomicReference<FullState> fullState;

    Timer(Context context) {
        this.context = context;

        service = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat("uhc-manager:timer-scheduler")
                        .build()
        );

        fullState = new AtomicReference<>(FullState.stopped(context));
    }

    @Synchronized
    public boolean start() {
        if (fullState.get().state() != State.STOPPED) {
            return false;
        }

        context.plugin().reloadConfig();
        val configInfo = ConfigInfo.fromConfig(context);
        val countdownFrom = configInfo.countdownFrom();

        fullState.set(FullState.newBuilder()
                .configInfo(configInfo)
                .runningState(RunningState.partiallyInitialized())
                .state(State.STARTING)
                .result());

        onCountdownStart(countdownFrom);

        // Schedule events
        val countdownFuture = service.scheduleAtFixedRate(new CountdownTask(countdownFrom), 0, 1, TimeUnit.SECONDS);
        val episodeFuture = service.scheduleAtFixedRate(episodeTask(), countdownFrom, TimeUnit.MINUTES.toSeconds(configInfo.episodeLength()), TimeUnit.SECONDS);
        val minuteFuture = service.scheduleAtFixedRate(minuteTask(), countdownFrom, TimeUnit.MINUTES.toSeconds(1), TimeUnit.SECONDS);

        fullState.updateAndGet(state -> state
                .toBuilder()
                .runningState(state.runningState()
                        .toBuilder()
                        .countdownFuture(countdownFuture)
                        .episodeFuture(episodeFuture)
                        .minuteFuture(minuteFuture)
                        .result())
                .result()
        );

        context.logger().info("Started UHC");
        return true;
    }

    @Synchronized
    public boolean stop() {
        val state = fullState.get();

        if (state.state() == State.STOPPED) {
            return false;
        }

        state.runningState().cancelFutures();
        onStop();
        fullState.set(FullState.stopped(context));

        context.logger().info("Stopped UHC");
        return true;
    }

    @Synchronized
    public boolean pause() {
        val state = fullState.get();

        if (state.state() != State.RUNNING) {
            return false;
        }

        val runningState = state.runningState();
        runningState.cancelFutures();

        val elapsedTime = System.currentTimeMillis() - runningState.effectiveStartTime();
        onPause(elapsedTime);

        fullState.updateAndGet(currentState ->
                currentState.toBuilder()
                        .state(State.PAUSED)
                        .runningState(currentState.runningState()
                                .toBuilder()
                                .elapsedTime(elapsedTime)
                                .result())
                        .result());

        context.logger().info("Paused UHC");
        return true;
    }

    @Synchronized
    public boolean resume() {
        val state = fullState.get();

        if (state.state() != State.PAUSED) {
            return false;
        }

        val configInfo = state.configInfo();
        val runningState = state.runningState();
        val elapsedTime = runningState.elapsedTime();
        onResume(elapsedTime);

        val currentTime = System.currentTimeMillis();

        val episodeOffset = Math.max(TimeUnit.MINUTES.toMillis(runningState.currentEpisode() * configInfo.episodeLength()) - elapsedTime, 0);
        val minuteOffset = Math.max(TimeUnit.MINUTES.toMillis(runningState.currentMinute()) - elapsedTime, 0);

        // Schedule events
        val episodeFuture = service.scheduleAtFixedRate(episodeTask(), episodeOffset, TimeUnit.MINUTES.toMillis(configInfo.episodeLength()), TimeUnit.MILLISECONDS);
        val minuteFuture = service.scheduleAtFixedRate(minuteTask(), minuteOffset, TimeUnit.MINUTES.toMillis(1), TimeUnit.MILLISECONDS);

        fullState.updateAndGet(currentState ->
                currentState.toBuilder()
                        .state(State.RUNNING)
                        .runningState(currentState.runningState()
                                .toBuilder()
                                .effectiveStartTime(currentTime - elapsedTime)
                                .episodeFuture(episodeFuture)
                                .minuteFuture(minuteFuture)
                                .result())
                        .result());

        context.logger().info("Resumed UHC");
        return true;
    }

    private Runnable minuteTask() {
        return () -> {
            val state = fullState.getAndUpdate(currentState ->
                    currentState.toBuilder()
                            .runningState(currentState.runningState().withNextMinute())
                            .result());

            val minute = state.runningState().currentMinute();
            if (minute > 0) {
                onMinute(minute);
            }
        };
    }

    private Runnable episodeTask() {
        return () -> {
            val state = fullState.getAndUpdate(currentState ->
                    currentState.toBuilder()
                            .runningState(currentState.runningState().withNextEpisode())
                            .result());

            val endingEpisode = state.runningState().currentEpisode();
            if (endingEpisode > 0) {
                onEpisodeEnd(endingEpisode);
            }
            onEpisodeStart(endingEpisode + 1);
        };
    }

    public State state() {
        return fullState.get().state();
    }

    private void onStart() {
        context.eventBus().post(new StartEvent());
        context.logger().info("Posted event for UHC start");
    }

    private void onStop() {
        context.eventBus().post(new StopEvent());
        context.logger().info("Posted event for UHC stop");
    }

    private void onPause(long timeElapsed) {
        context.eventBus().post(new PauseEvent(timeElapsed));
        context.logger().info("Posted event for UHC pause");
    }

    private void onResume(long timeElapsed) {
        context.eventBus().post(new ResumeEvent(timeElapsed));
        context.logger().info("Posted event for UHC resume");
    }

    private void onEpisodeStart(int episodeNumber) {
        context.eventBus().post(new EpisodeStartEvent(episodeNumber, fullState.get().configInfo().episodeLength()));
        context.logger().info("Posted event for start of episode " + episodeNumber);
    }

    private void onEpisodeEnd(int episodeNumber) {
        context.eventBus().post(new EpisodeEndEvent(episodeNumber, fullState.get().configInfo().episodeLength()));
        context.logger().info("Posted event for end of episode " + episodeNumber);
    }

    private void onCountdownStart(int countingFrom) {
        context.eventBus().post(new CountdownStartEvent(countingFrom));
        context.logger().info("Posted event for countdown start");
    }

    private void onCountdownMark(int mark) {
        context.eventBus().post(new CountdownMarkEvent(mark));
        context.logger().info("Posted event for countdown mark " + mark);
    }

    private void onMinute(int minute) {
        context.eventBus().post(new MinuteEvent(minute));
        context.logger().info("Posted event for minute " + minute);
    }

    public enum State {
        STOPPED, STARTING, RUNNING, PAUSED
    }

    @Value
    @Accessors(fluent = true)
    private static class ConfigInfo {
        int episodeLength;
        int countdownFrom;

        static ConfigInfo fromConfig(Context context) {
            return new ConfigInfo(Config.getValidatedEpisodeLength(context), Config.getCountdownFrom(context));
        }
    }

    @Value
    @Accessors(fluent = true)
    @Builder(builderClassName = "Builder", builderMethodName = "newBuilder", buildMethodName = "result", toBuilder = true)
    private static class RunningState {
        int currentMinute;
        int currentEpisode;
        long originalStartTime;
        long effectiveStartTime;
        long elapsedTime;
        Future<?> countdownFuture;
        Future<?> episodeFuture;
        Future<?> minuteFuture;

        static RunningState partiallyInitialized() {
            return newBuilder()
                    .currentMinute(0)
                    .currentEpisode(0)
                    .countdownFuture(null)
                    .episodeFuture(null)
                    .minuteFuture(null)
                    .originalStartTime(-1)
                    .effectiveStartTime(-1)
                    .elapsedTime(-1)
                    .result();
        }

        RunningState startingNow() {
            val time = System.currentTimeMillis();

            return toBuilder()
                    .originalStartTime(time)
                    .effectiveStartTime(time)
                    .elapsedTime(0)
                    .result();
        }

        RunningState withNextEpisode() {
            return toBuilder()
                    .currentEpisode(currentEpisode + 1)
                    .result();
        }

        RunningState withNextMinute() {
            return toBuilder()
                    .currentMinute(currentMinute + 1)
                    .result();
        }

        void cancelFutures() {
            countdownFuture().cancel(true);
            episodeFuture().cancel(true);
            minuteFuture().cancel(true);
        }
    }

    @Value
    @Accessors(fluent = true)
    @Builder(builderClassName = "Builder", builderMethodName = "newBuilder", buildMethodName = "result", toBuilder = true)
    private static class FullState {
        State state;
        RunningState runningState;
        ConfigInfo configInfo;

        static FullState stopped(Context context) {
            return new FullState(State.STOPPED, null, ConfigInfo.fromConfig(context));
        }
    }

    private class CountdownTask implements Runnable {
        private final AtomicInteger mark;

        CountdownTask(int countdownFrom) {
            mark = new AtomicInteger(countdownFrom);
        }

        @Override
        public void run() {
            val currentMark = mark.getAndDecrement();

            if (currentMark > 0) {
                onCountdownMark(currentMark);
            } else if (currentMark == 0) {
                // Switch from STARTING to RUNNING
                fullState.updateAndGet(state -> {
                    val currentState = state.state();
                    return state.toBuilder()
                            .runningState(state.runningState()
                                    .startingNow())
                            .state(currentState == State.STARTING ? State.RUNNING : currentState)
                            .result();
                });

                onStart();

                // Cancel this future by throwing an exception
                context.logger().fine("Cancelling countdown future");
                throw new RuntimeException("Cancelling countdown future by throwing this exception");
            } else {
                context.logger().severe("Negative countdown mark: " + currentMark);
                throw new IllegalStateException("Negative countdown mark: " + currentMark);
            }
        }
    }
}
