package com.github.nthportal.uhc.events;

import com.github.nthportal.uhc.core.Config;
import com.github.nthportal.uhc.util.CommandExecutor;
import com.google.common.eventbus.Subscribe;
import lombok.AllArgsConstructor;
import lombok.val;

import java.util.ArrayList;
import java.util.function.Function;

import static com.github.nthportal.uhc.util.CommandExecutor.*;

@AllArgsConstructor
public class MainListener {
    private final CommandExecutor executor;

    @Subscribe
    public void onPlayerDeath(UHCPlayerDeathEvent event) {
        val replacements = new ArrayList<Function<String, String>>();
        replacements.add(replacement(ReplaceTargets.PLAYER, event.player().getName()));
        executor.executeEventCommands(Config.Events.ON_DEATH, replacements);
    }

    @Subscribe
    public void onCountdownStart(UHCCountdownStartEvent event) {
        executor.executeEventCommands(Config.Events.ON_COUNTDOWN_START);
    }

    @Subscribe
    public void onCountdownMark(UHCCountdownMarkEvent event) {
        val replacements = new ArrayList<Function<String, String>>();
        replacements.add(replacement(ReplaceTargets.COUNTDOWN_MARK, String.valueOf(event.countdownMark())));
        executor.executeEventCommands(Config.Events.ON_COUNTDOWN_MARK, replacements);
    }

    @Subscribe
    public void onStart(UHCStartEvent event) {
        executor.executeEventCommands(Config.Events.ON_START);
    }

    @Subscribe
    public void onStop(UHCStopEvent event) {
        executor.executeEventCommands(Config.Events.ON_STOP);
    }

    @Subscribe
    public void onPause(UHCPauseEvent event) {
        executor.executeEventCommands(Config.Events.ON_PAUSE);
    }

    @Subscribe
    public void onResume(UHCResumeEvent event) {
        executor.executeEventCommands(Config.Events.ON_RESUME);
    }

    @Subscribe
    public void onEpisodeStart(UHCEpisodeStartEvent event) {
        val replacements = new ArrayList<Function<String, String>>();
        replacements.add(replacement(ReplaceTargets.EPISODE, String.valueOf(event.episodeNumber())));
        replacements.add(replacement(ReplaceTargets.MINUTES, String.valueOf(event.minutesElapsed())));
        executor.executeEventCommands(Config.Events.ON_EPISODE_START, replacements);

        // Run episode-specific commands
        executor.executeMappedCommandsMatching(Config.Events.ON_START_EP_NUM, event.episodeNumber());
    }

    @Subscribe
    public void onEpisdeEnd(UHCEpisodeEndEvent event) {
        val replacements = new ArrayList<Function<String, String>>();
        replacements.add(replacement(ReplaceTargets.EPISODE, String.valueOf(event.episodeNumber())));
        replacements.add(replacement(ReplaceTargets.MINUTES, String.valueOf(event.minutesElapsed())));
        executor.executeEventCommands(Config.Events.ON_EPISODE_END, replacements);
    }
}
