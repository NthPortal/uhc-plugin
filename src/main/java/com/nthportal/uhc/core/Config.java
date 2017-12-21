package com.nthportal.uhc.core;

import lombok.val;

import java.util.Collections;

public final class Config {
    private Config() {}

    public static final String EPISODE_LENGTH = "episode-length";
    public static final String COUNTDOWN_FROM = "countdown-from";

    public static final int DEFAULT_EPISODE_LENGTH = 20;
    public static final int DEFAULT_COUNTDOWN_FROM = 5;

    public static void setup(UHCPlugin plugin) {
        val config = plugin.getConfig();

        config.addDefault(EPISODE_LENGTH, DEFAULT_EPISODE_LENGTH);
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
        public static final String ON_COUNTDOWN_START = "on-countdown-start";
        public static final String ON_COUNTDOWN_MARK = "on-countdown-mark";
        public static final String ON_START = "on-start";
        public static final String ON_STOP = "on-stop";
        public static final String ON_PAUSE = "on-pause";
        public static final String ON_RESUME = "on-resume";
        public static final String ON_EPISODE_START = "on-ep-start";
        public static final String ON_EPISODE_END = "on-ep-end";
        public static final String ON_DEATH = "on-death";
        public static final String ON_START_EP_NUM = "on-start-ep-num";
        public static final String ON_MINUTE = "on-minute";
    }
}
