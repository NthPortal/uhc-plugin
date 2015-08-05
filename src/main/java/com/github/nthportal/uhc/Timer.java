package com.github.nthportal.uhc;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

public class Timer {
    private final UHCPlugin plugin;
    private ScheduledExecutorService service;
    private State state = State.STOPPED;
    private int interval;
    private int episode = 0;
    private long startTime;
    private long elapsedTime = 0;
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

    public Timer(UHCPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean start() {
        lock.writeLock().lock();
        try {
            if (state != State.STOPPED) {
                return false;
            }

            service = Executors.newSingleThreadScheduledExecutor();
            interval = plugin.config.getInt(Configs.EPISODE_TIME);
            countdown();
            startTime = System.currentTimeMillis();
            service.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    doEpisodeMarker();
                }
            }, interval, interval, TimeUnit.MINUTES);
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

            service.shutdownNow();
            service = null;
            episode = 0;
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
            elapsedTime += currentTime - startTime;
            // TODO shutdown ScheduledExecutorService

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

            // TODO implement

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

    private void countdown() {
        int countdownFrom = plugin.config.getInt(Configs.COUNTDOWN_FROM);
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
        // TODO send various start commands
    }

    private void onStop() {
        // TODO send various stop commands
    }

    private void onPause() {
        // TODO implement
    }

    private void onResume() {
        // TODO implement
    }

    private void onEpisodeStart() {
        int minutes = interval * (episode - 1);
        // TODO run episode start commands
    }

    private void onEpisodeEnd() {
        int minutes = interval * episode;
        // TODO run episode end commands
    }

    private void onCountdownMark(int mark) {
        // TODO run countdown commands
    }

    public enum State {
        STOPPED, RUNNING, PAUSED
    }
}
