package com.nthportal.uhc.events;

import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(fluent = true)
public class UHCCountdownMarkEvent {
    int countdownMark;
}