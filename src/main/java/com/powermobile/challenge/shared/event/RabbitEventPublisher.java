package com.powermobile.challenge.shared.event;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

//@Component
public class RabbitEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(String routingKey, Object event) {
        rabbitTemplate.convertAndSend("proposal.exchange", routingKey, event);
    }
}
