package com.github.nthportal.uhc;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.List;

public class Config {
    public static final String EPISODE_TIME = "episodeTime";
    public static final String COUNTDOWN_FROM = "countdownFrom";

    public static void setup(UHCPlugin plugin) {
        FileConfiguration config = plugin.config;

        config.addDefault(EPISODE_TIME, 20);
        config.addDefault(COUNTDOWN_FROM, 5);

        List<String> emptyList = Collections.emptyList();
        config.addDefault(Events.ON_COUNTDOWN_START, emptyList);
        config.addDefault(Events.ON_COUNTDOWN_MARK, emptyList);
        config.addDefault(Events.ON_START, emptyList);
        config.addDefault(Events.ON_STOP, emptyList);
        config.addDefault(Events.ON_PAUSE, emptyList);
        config.addDefault(Events.ON_RESUME, emptyList);
        config.addDefault(Events.ON_EPISODE_START, emptyList);
        config.addDefault(Events.ON_EPISODE_END, emptyList);
        config.addDefault(Events.ON_DEATH, emptyList);

        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    public static class Events {
        static final String ON_COUNTDOWN_START = "onCountdownStart";
        static final String ON_COUNTDOWN_MARK = "onCountdownMark";
        static final String ON_START = "onStart";
        static final String ON_STOP = "onStop";
        static final String ON_PAUSE = "onPause";
        static final String ON_RESUME = "onResume";
        static final String ON_EPISODE_START = "onEpStart";
        static final String ON_EPISODE_END = "onEpEnd";
        static final String ON_DEATH = "onDeath";
    }
}
