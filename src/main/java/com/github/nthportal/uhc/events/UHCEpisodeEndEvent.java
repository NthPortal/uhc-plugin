package com.github.nthportal.uhc.events;

public class UHCEpisodeEndEvent {
    private final int episodeNumber;
    private final int episodeLength;
    private final int minutesElapsed;

    public UHCEpisodeEndEvent(int episodeNumber, int episodeLength) {
        this.episodeNumber = episodeNumber;
        this.episodeLength = episodeLength;
        this.minutesElapsed = episodeNumber * episodeLength;
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
