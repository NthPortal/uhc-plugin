package com.github.nthportal.uhc.core;

import com.github.nthportal.uhc.events.UHCPlayerDeathEvent;
import lombok.AllArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

@AllArgsConstructor
public class DeathListener implements Listener {
    private final Context context;
    private final Timer timer;

    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent event) {
        if (timer.state() == Timer.State.RUNNING) {
            context.logger().info("Player died: " + event.getEntity().getName());
            context.eventBus().post(new UHCPlayerDeathEvent(event.getEntity()));
        }
    }
}
