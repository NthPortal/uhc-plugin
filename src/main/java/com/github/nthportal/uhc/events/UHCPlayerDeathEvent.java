package com.github.nthportal.uhc.events;

import org.bukkit.entity.Player;

public class UHCPlayerDeathEvent {
    public final Player player;

    public UHCPlayerDeathEvent(Player player) {
        this.player = player;
    }
}
