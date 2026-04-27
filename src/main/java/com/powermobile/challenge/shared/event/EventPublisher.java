package com.powermobile.challenge.shared.event;

public interface EventPublisher {
    //Para implementar RabbitMQ, descomentar o método abaixo e comente o método publish atual
    //void publish(String routingKey, DomainEvent event);
    void publish(String eventType, Object event);
}
