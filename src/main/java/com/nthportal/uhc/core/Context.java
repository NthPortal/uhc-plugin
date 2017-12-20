package com.nthportal.uhc.core;

import com.google.common.eventbus.EventBus;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.logging.Logger;

@Value
@Accessors(fluent = true)
public class Context {
    UHCPlugin plugin;
    Logger logger;
    EventBus eventBus;
}
