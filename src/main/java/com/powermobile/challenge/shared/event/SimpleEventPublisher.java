package com.powermobile.challenge.shared.event;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
public class SimpleEventPublisher implements EventPublisher {

    private final List<EventListener> listeners;

    public SimpleEventPublisher(List<EventListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public void publish(String eventType, Object event) {

        for (EventListener listener : listeners) {
            listener.handle(event);
        }
    }
}
