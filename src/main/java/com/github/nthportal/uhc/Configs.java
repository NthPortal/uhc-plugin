package com.github.nthportal.uhc;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.List;

public class Configs {
    public static final String EPISODE_TIME = "episodeTime";
    public static final String COUNTDOWN_FROM = "countdownFrom";

    public static void setup(UHCPlugin plugin) {
        FileConfiguration config = plugin.config;

        config.addDefault(EPISODE_TIME, 20);
        config.addDefault(COUNTDOWN_FROM, 5);

        List<String> emptyList = Collections.emptyList();
        config.addDefault(ConfCommandExecutor.Opts.ON_COUNTDOWN_MARK, emptyList);
        config.addDefault(ConfCommandExecutor.Opts.ON_START, emptyList);
        config.addDefault(ConfCommandExecutor.Opts.ON_STOP, emptyList);
        config.addDefault(ConfCommandExecutor.Opts.ON_PAUSE, emptyList);
        config.addDefault(ConfCommandExecutor.Opts.ON_RESUME, emptyList);
        config.addDefault(ConfCommandExecutor.Opts.ON_EPISODE_START, emptyList);
        config.addDefault(ConfCommandExecutor.Opts.ON_EPISODE_END, emptyList);
        config.addDefault(ConfCommandExecutor.Opts.ON_DEATH, emptyList);

        config.options().copyDefaults(true);
        plugin.saveConfig();
    }
}
