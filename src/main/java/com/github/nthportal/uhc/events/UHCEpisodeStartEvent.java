package com.github.nthportal.uhc.events;

public class UHCEpisodeStartEvent {
    private final int episodeNumber;
    private final int episodeLength;
    private final int minutesElapsed;

    public UHCEpisodeStartEvent(int episodeNumber, int episodeLength) {
        this.episodeNumber = episodeNumber;
        this.episodeLength = episodeLength;
        this.minutesElapsed = episodeLength * (episodeNumber - 1);
    }

    public int getEpisodeNumber() {
        return episodeNumber;
    }

    public int getEpisodeLength() {
        return episodeLength;
    }

    public int getMinutesElapsed() {
        return minutesElapsed;
    }
}
