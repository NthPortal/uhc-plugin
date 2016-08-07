package com.github.nthportal.uhc.events;

public class UHCCountdownStartEvent {
    public final int countingDownFrom;

    public UHCCountdownStartEvent(int countingDownFrom) {
        this.countingDownFrom = countingDownFrom;
    }
}
