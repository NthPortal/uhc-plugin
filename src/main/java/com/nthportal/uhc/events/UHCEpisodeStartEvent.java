package com.nthportal.uhc.events;

import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(fluent = true)
public class UHCEpisodeStartEvent {
    int episodeNumber;
    int episodeLength;

    public int minutesElapsed() {
        return episodeLength * (episodeNumber - 1);
    }
}
