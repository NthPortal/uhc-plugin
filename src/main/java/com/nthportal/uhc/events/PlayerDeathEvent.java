package com.nthportal.uhc.events;

import lombok.Value;
import lombok.experimental.Accessors;
import org.bukkit.entity.Player;

@Value
@Accessors(fluent = true)
public class PlayerDeathEvent {
    Player player;
}
