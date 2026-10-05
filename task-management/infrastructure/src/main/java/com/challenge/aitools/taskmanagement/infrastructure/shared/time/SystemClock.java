package com.challenge.aitools.taskmanagement.infrastructure.shared.time;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.challenge.aitools.taskmanagement.application.shared.port.out.Clock;

@Component
public class SystemClock implements Clock {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
