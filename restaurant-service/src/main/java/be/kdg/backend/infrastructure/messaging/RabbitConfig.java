package be.kdg.backend.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for restaurant-service.
 *
 * Exchange:     kdg.events                          (topic, durable — shared with order/delivery)
 * DLX:          kdg.events.dlx                      (topic, durable — shared)
 * Consumer queue:
 *   q.restaurant.order-events          bound order.placed (from order-service)
 * Producer: restaurant-service publishes order.accepted, order.rejected, order.ready_for_pickup
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_NAME  = "kdg.events";
    public static final String DLX_NAME        = "kdg.events.dlx";

    public static final String Q_RESTAURANT_ORDER_EVENTS = "q.restaurant.order-events";
    public static final String Q_DLQ                      = "q.dlq.restaurant";

    @Bean
    TopicExchange kdgEventsExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_NAME).durable(true).build();
    }

    @Bean
    TopicExchange kdgEventsDlx() {
        return ExchangeBuilder.topicExchange(DLX_NAME).durable(true).build();
    }

    @Bean
    Queue dlq() {
        return QueueBuilder.durable(Q_DLQ).build();
    }

    @Bean
    Binding dlqBinding() {
        return BindingBuilder.bind(dlq()).to(kdgEventsDlx()).with("#");
    }

    @Bean
    Queue qRestaurantOrderEvents() {
        return QueueBuilder.durable(Q_RESTAURANT_ORDER_EVENTS)
                .deadLetterExchange(DLX_NAME)
                .build();
    }

    @Bean
    Binding bRestaurantOrderEvents() {
        return BindingBuilder.bind(qRestaurantOrderEvents())
                .to(kdgEventsExchange())
                .with("order.placed");
    }

    @Bean
    MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}