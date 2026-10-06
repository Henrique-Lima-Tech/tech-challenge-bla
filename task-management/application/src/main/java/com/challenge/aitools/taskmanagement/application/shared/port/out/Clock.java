package com.challenge.aitools.taskmanagement.application.shared.port.out;

import java.time.Instant;

public interface Clock {

    Instant now();
}
