package com.nthportal.uhc.events;

import com.nthportal.uhc.core.Config;
import com.nthportal.uhc.util.CommandExecutor;
import com.google.common.eventbus.Subscribe;
import lombok.AllArgsConstructor;
import lombok.val;

import java.util.ArrayList;
import java.util.function.Function;

import static com.nthportal.uhc.util.CommandExecutor.*;

@AllArgsConstructor
public class MainListener {
    private final CommandExecutor executor;

    @Subscribe
    public void onPlayerDeath(PlayerDeathEvent event) {
        val replacements = new ArrayList<Function<String, String>>();
        replacements.add(replacement(ReplaceTargets.PLAYER, event.player().getName()));
        executor.executeEventCommands(Config.Events.ON_DEATH, replacements);
    }

    @Subscribe
    public void onCountdownStart(CountdownStartEvent event) {
        executor.executeEventCommands(Config.Events.ON_COUNTDOWN_START);
    }

    @Subscribe
    public void onCountdownMark(CountdownMarkEvent event) {
        val replacements = new ArrayList<Function<String, String>>();
        replacements.add(replacement(ReplaceTargets.COUNTDOWN_MARK, String.valueOf(event.countdownMark())));
        executor.executeEventCommands(Config.Events.ON_COUNTDOWN_MARK, replacements);
    }

    @Subscribe
    public void onStart(StartEvent event) {
        executor.executeEventCommands(Config.Events.ON_START);
    }

    @Subscribe
    public void onStop(StopEvent event) {
        executor.executeEventCommands(Config.Events.ON_STOP);
    }

    @Subscribe
    public void onPause(PauseEvent event) {
        executor.executeEventCommands(Config.Events.ON_PAUSE);
    }

    @Subscribe
    public void onResume(ResumeEvent event) {
        executor.executeEventCommands(Config.Events.ON_RESUME);
    }

    @Subscribe
    public void onEpisodeStart(EpisodeStartEvent event) {
        val replacements = new ArrayList<Function<String, String>>();
        replacements.add(replacement(ReplaceTargets.EPISODE, String.valueOf(event.episodeNumber())));
        replacements.add(replacement(ReplaceTargets.MINUTES, String.valueOf(event.minutesElapsed())));
        executor.executeEventCommands(Config.Events.ON_EPISODE_START, replacements);

        // Run episode-specific commands
        executor.executeMappedCommandsMatching(Config.Events.ON_START_EP_NUM, event.episodeNumber());
    }

    @Subscribe
    public void onEpisdeEnd(EpisodeEndEvent event) {
        val replacements = new ArrayList<Function<String, String>>();
        replacements.add(replacement(ReplaceTargets.EPISODE, String.valueOf(event.episodeNumber())));
        replacements.add(replacement(ReplaceTargets.MINUTES, String.valueOf(event.minutesElapsed())));
        executor.executeEventCommands(Config.Events.ON_EPISODE_END, replacements);
    }

    @Subscribe
    public void onMinute(MinuteEvent event) {
        executor.executeMappedCommandsMatching(Config.Events.ON_MINUTE, event.minuteNumber());
    }
}
