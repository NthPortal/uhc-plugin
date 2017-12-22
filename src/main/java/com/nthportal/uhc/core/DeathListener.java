package com.nthportal.uhc.core;

import com.nthportal.uhc.events.PlayerDeathEvent;
import com.nthportal.uhc.events.PlayerKillEvent;
import lombok.AllArgsConstructor;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@AllArgsConstructor
public class DeathListener implements Listener {
    private final Context context;
    private final Timer timer;

    @EventHandler
    public void onPlayerDeath(final org.bukkit.event.entity.PlayerDeathEvent event) {
        if (timer.state() == Timer.State.RUNNING) {
            val player = event.getEntity();
            context.logger().info("Player died: " + player.getName());
            context.eventBus().post(new PlayerDeathEvent(player));

            // Check for PvP kill
            val killer = player.getKiller();
            if (killer != null) {
                context.logger().info(killer + " killed " + player.getName());
                context.eventBus().post(new PlayerKillEvent(killer, player));
            }

        }
    }
}
