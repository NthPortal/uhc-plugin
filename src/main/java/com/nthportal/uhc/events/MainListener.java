package com.nthportal.uhc.events;

import com.nthportal.uhc.core.Config;
import com.nthportal.uhc.util.CommandExecutor;
import com.google.common.eventbus.Subscribe;
import lombok.AllArgsConstructor;
import lombok.val;

import java.util.Arrays;
import java.util.Collections;

import static com.nthportal.uhc.util.CommandExecutor.*;

@AllArgsConstructor
public class MainListener {
    private final CommandExecutor executor;

    @Subscribe
    public void onPlayerDeath(PlayerDeathEvent event) {
        val player = event.player();
        val replacements = Collections.singleton(
                replacement(ReplaceTargets.PLAYER, player.getName())
        );
        executor.executeEventCommands(Config.Events.ON_DEATH, replacements);

        // Run player-specific death commands
        executor.executeMappedCommandsMatchingString(Config.Events.ON_DEATH_OF, player.getName());
        executor.executeMappedCommandsMatchingString(Config.Events.ON_DEATH_OF, player.getUniqueId().toString());
    }

    @Subscribe
    public void onPlayerKill(PlayerKillEvent event) {
        val killer = event.killer();
        val corpse = event.dead();

        val replacements = Arrays.asList(
                replacement(ReplaceTargets.KILLER, killer.getName()),
                replacement(ReplaceTargets.CORPSE, corpse.getName())
        );
        executor.executeEventCommands(Config.Events.ON_PVP_KILL, replacements);

        // Run player-specific kill commands
        executor.executeMappedCommandsMatchingString(Config.Events.ON_PVP_DEATH_OF, corpse.getName(), replacements);
        executor.executeMappedCommandsMatchingString(Config.Events.ON_PVP_DEATH_OF, corpse.getUniqueId().toString(), replacements);
        executor.executeMappedCommandsMatchingString(Config.Events.ON_PVP_KILL_BY, killer.getName(), replacements);
        executor.executeMappedCommandsMatchingString(Config.Events.ON_PVP_KILL_BY, killer.getUniqueId().toString(), replacements);
    }

    @Subscribe
    public void onCountdownStart(CountdownStartEvent event) {
        executor.executeEventCommands(Config.Events.ON_COUNTDOWN_START);
    }

    @Subscribe
    public void onCountdownMark(CountdownMarkEvent event) {
        val replacements = Collections.singleton(
                replacement(ReplaceTargets.COUNTDOWN_MARK, String.valueOf(event.countdownMark()))
        );
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
        val minutesReplacement = replacement(ReplaceTargets.MINUTES, String.valueOf(event.minutesElapsed()));
        val replacements = Arrays.asList(
                replacement(ReplaceTargets.EPISODE, String.valueOf(event.episodeNumber())),
                minutesReplacement
        );
        executor.executeEventCommands(Config.Events.ON_EPISODE_START, replacements);

        // Run episode-specific commands
        executor.executeMappedCommandsMatchingInt(Config.Events.ON_START_EP_NUM, event.episodeNumber(),
                Collections.singleton(minutesReplacement));
    }

    @Subscribe
    public void onEpisdeEnd(EpisodeEndEvent event) {
        val replacements = Arrays.asList(
                replacement(ReplaceTargets.EPISODE, String.valueOf(event.episodeNumber())),
                replacement(ReplaceTargets.MINUTES, String.valueOf(event.minutesElapsed()))
        );
        executor.executeEventCommands(Config.Events.ON_EPISODE_END, replacements);
    }

    @Subscribe
    public void onMinute(MinuteEvent event) {
        executor.executeMappedCommandsMatchingInt(Config.Events.ON_MINUTE, event.minuteNumber());
    }
}
