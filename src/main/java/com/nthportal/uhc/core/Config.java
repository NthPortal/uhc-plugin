package com.nthportal.uhc.core;

import lombok.val;

import java.util.Collections;

public class Config {
    public static final String EPISODE_TIME = "episodeTime";
    public static final String COUNTDOWN_FROM = "countdownFrom";

    public static final int DEFAULT_EPISODE_TIME = 20;
    public static final int DEFAULT_COUNTDOWN_FROM = 5;

    public static void setup(UHCPlugin plugin) {
        val config = plugin.getConfig();

        config.addDefault(EPISODE_TIME, DEFAULT_EPISODE_TIME);
        config.addDefault(COUNTDOWN_FROM, DEFAULT_COUNTDOWN_FROM);

        val emptyList = Collections.emptyList();
        val emptyMap = Collections.emptyMap();
        config.addDefault(Events.ON_COUNTDOWN_START, emptyList);
        config.addDefault(Events.ON_COUNTDOWN_MARK, emptyList);
        config.addDefault(Events.ON_START, emptyList);
        config.addDefault(Events.ON_STOP, emptyList);
        config.addDefault(Events.ON_PAUSE, emptyList);
        config.addDefault(Events.ON_RESUME, emptyList);
        config.addDefault(Events.ON_EPISODE_START, emptyList);
        config.addDefault(Events.ON_EPISODE_END, emptyList);
        config.addDefault(Events.ON_DEATH, emptyList);
        config.addDefault(Events.ON_START_EP_NUM, emptyMap);
        config.addDefault(Events.ON_MINUTE, emptyMap);

        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    public static class Events {
        public static final String ON_COUNTDOWN_START = "onCountdownStart";
        public static final String ON_COUNTDOWN_MARK = "onCountdownMark";
        public static final String ON_START = "onStart";
        public static final String ON_STOP = "onStop";
        public static final String ON_PAUSE = "onPause";
        public static final String ON_RESUME = "onResume";
        public static final String ON_EPISODE_START = "onEpStart";
        public static final String ON_EPISODE_END = "onEpEnd";
        public static final String ON_DEATH = "onDeath";
        public static final String ON_START_EP_NUM = "onStartEpNum";
        public static final String ON_MINUTE = "onMinute";
    }
}
