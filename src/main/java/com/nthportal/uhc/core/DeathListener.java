package com.nthportal.uhc.core;

import com.nthportal.uhc.events.PlayerDeathEvent;
import lombok.AllArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@AllArgsConstructor
public class DeathListener implements Listener {
    private final Context context;
    private final Timer timer;

    @EventHandler
    public void onPlayerDeath(final org.bukkit.event.entity.PlayerDeathEvent event) {
        if (timer.state() == Timer.State.RUNNING) {
            context.logger().info("Player died: " + event.getEntity().getName());
            context.eventBus().post(new PlayerDeathEvent(event.getEntity()));
        }
    }
}
