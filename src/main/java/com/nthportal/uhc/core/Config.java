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
        config.addDefault(Events.ON_PVP_KILL, emptyList);
        config.addDefault(Events.ON_DEATH_OF, emptyMap);
        config.addDefault(Events.ON_PVP_DEATH_OF, emptyMap);
        config.addDefault(Events.ON_PVP_KILL_BY, emptyMap);
        config.addDefault(Events.ON_START_EP_NUM, emptyMap);
        config.addDefault(Events.ON_MINUTE, emptyMap);

        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    public static int getValidatedEpisodeLength(Context context) {
        return getValidatedEpisodeLength(context.plugin());
    }

    public static int getValidatedEpisodeLength(UHCPlugin plugin) {
        int length = plugin.getConfig().getInt(EPISODE_LENGTH);
        if (length <= 0) {
            length = DEFAULT_EPISODE_LENGTH;
            plugin.getConfig().set(EPISODE_LENGTH, length);
            plugin.saveConfig();
        }
        return length;
    }

    public static int getCountdownFrom(Context context) {
        return getCountdownFrom(context.plugin());
    }

    public static int getCountdownFrom(UHCPlugin plugin) {
        return Math.max(plugin.getConfig().getInt(COUNTDOWN_FROM), 0);
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
        public static final String ON_PVP_KILL = "on-pvp-kill";
        public static final String ON_DEATH_OF = "on-death-of";
        public static final String ON_PVP_DEATH_OF = "on-pvp-death-of";
        public static final String ON_PVP_KILL_BY = "on-pvp-kill-by";
        public static final String ON_START_EP_NUM = "on-start-ep-num";
        public static final String ON_MINUTE = "on-minute";
    }
}
