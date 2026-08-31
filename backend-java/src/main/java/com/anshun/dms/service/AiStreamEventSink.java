package com.anshun.dms.service;

import java.util.Map;

/** Transport-neutral output contract for one Agent response stream. */
@FunctionalInterface
public interface AiStreamEventSink {
    void emit(String event, Map<String, Object> data);

    /** Allows the provider loop to stop tool calls and persistence after the transport disconnects. */
    default boolean isCancelled() { return false; }
}
