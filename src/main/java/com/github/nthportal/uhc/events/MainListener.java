package com.github.nthportal.uhc.events;

import com.github.nthportal.uhc.core.Config;
import com.github.nthportal.uhc.core.Context;
import com.github.nthportal.uhc.util.CommandUtil;
import com.google.common.base.Function;
import com.google.common.eventbus.Subscribe;
import lombok.AllArgsConstructor;
import lombok.val;

import java.util.ArrayList;

@AllArgsConstructor
public class MainListener {
    private final Context context;

    @Subscribe
    public void onPlayerDeath(UHCPlayerDeathEvent event) {
        val replacements = new ArrayList<Function<String, String>>();
        replacements.add(CommandUtil.replacementFunction(CommandUtil.ReplaceTargets.PLAYER, event.player().getName()));
        CommandUtil.executeEventCommands(context, Config.Events.ON_DEATH, replacements);
    }

    @Subscribe
    public void onCountdownStart(UHCCountdownStartEvent event) {
        CommandUtil.executeEventCommands(context, Config.Events.ON_COUNTDOWN_START);
    }

    @Subscribe
    public void onCountdownMark(UHCCountdownMarkEvent event) {
        val replacements = new ArrayList<Function<String, String>>();
        replacements.add(CommandUtil.replacementFunction(CommandUtil.ReplaceTargets.COUNTDOWN_MARK, String.valueOf(event.countdownMark())));
        CommandUtil.executeEventCommands(context, Config.Events.ON_COUNTDOWN_MARK, replacements);
    }

    @Subscribe
    public void onStart(UHCStartEvent event) {
        CommandUtil.executeEventCommands(context, Config.Events.ON_START);
    }

    @Subscribe
    public void onStop(UHCStopEvent event) {
        CommandUtil.executeEventCommands(context, Config.Events.ON_STOP);
    }

    @Subscribe
    public void onPause(UHCPauseEvent event) {
        CommandUtil.executeEventCommands(context, Config.Events.ON_PAUSE);
    }

    @Subscribe
    public void onResume(UHCResumeEvent event) {
        CommandUtil.executeEventCommands(context, Config.Events.ON_RESUME);
    }

    @Subscribe
    public void onEpisodeStart(UHCEpisodeStartEvent event) {
        val replacements = new ArrayList<Function<String, String>>();
        replacements.add(CommandUtil.replacementFunction(CommandUtil.ReplaceTargets.EPISODE, String.valueOf(event.episodeNumber())));
        replacements.add(CommandUtil.replacementFunction(CommandUtil.ReplaceTargets.MINUTES, String.valueOf(event.minutesElapsed())));
        CommandUtil.executeEventCommands(context, Config.Events.ON_EPISODE_START, replacements);

        // Run episode-specific commands
        CommandUtil.executeMappedCommandsMatching(context, Config.Events.ON_START_EP_NUM, event.episodeNumber());
    }

    @Subscribe
    public void onEpisdeEnd(UHCEpisodeEndEvent event) {
        val replacements = new ArrayList<Function<String, String>>();
        replacements.add(CommandUtil.replacementFunction(CommandUtil.ReplaceTargets.EPISODE, String.valueOf(event.episodeNumber())));
        replacements.add(CommandUtil.replacementFunction(CommandUtil.ReplaceTargets.MINUTES, String.valueOf(event.minutesElapsed())));
        CommandUtil.executeEventCommands(context, Config.Events.ON_EPISODE_END, replacements);
    }
}
