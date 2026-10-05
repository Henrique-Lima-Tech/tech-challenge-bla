package com.challenge.aitools.taskmanagement.application.shared.port.out;

import java.time.Instant;

/**
 * The only source of time for the use cases, so tests control "now".
 */
public interface Clock {

    Instant now();
}
