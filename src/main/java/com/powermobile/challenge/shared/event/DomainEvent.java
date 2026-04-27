package com.powermobile.challenge.shared.event;

import java.time.LocalDateTime;

public abstract class DomainEvent {

    private LocalDateTime occurredAt = LocalDateTime.now();

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}