package com.github.nthportal.uhc.events;

public class UHCPauseEvent {
    public final long timeElapsed;

    public UHCPauseEvent(long timeElapsed) {
        this.timeElapsed = timeElapsed;
    }
}
