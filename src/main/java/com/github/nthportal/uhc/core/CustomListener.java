package com.github.nthportal.uhc.core;

import com.github.nthportal.uhc.UHCPlugin;
import com.github.nthportal.uhc.events.UHCPlayerDeathEvent;
import com.github.nthportal.uhc.util.CommandUtil;
import com.google.common.base.Function;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.ArrayList;
import java.util.List;

public class CustomListener implements Listener {
    private final UHCPlugin plugin;

    public CustomListener(UHCPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent event) {
        if (plugin.timer.getState() != Timer.State.RUNNING) {
            return;
        }

        plugin.logger.info("Player died: " + event.getEntity().getName());
        plugin.eventBus.post(new UHCPlayerDeathEvent(event.getEntity()));
    }
}
