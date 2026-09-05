package be.kdg.backend.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for order-service.
 *
 * Exchange:     kdg.events                          (topic, durable, auto-delete=false)
 * DLX:          kdg.events.dlx                      (topic, durable)
 * Order queues (consumer side, durable, single consumer):
 *   q.order.order-accepted              bound order.accepted
 *   q.order.order-rejected              bound order.rejected
 *   q.order.order-ready-for-pickup      bound order.ready_for_pickup
 *   q.order.order-picked-up              bound order.picked_up
 *   q.order.order-delivered              bound order.delivered
 * Producer: order-service publishes   order.placed     (and order.cancelled) to kdg.events
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_NAME  = "kdg.events";
    public static final String DLX_NAME        = "kdg.events.dlx";

    public static final String Q_ORDER_ACCEPTED        = "q.order.order-accepted";
    public static final String Q_ORDER_REJECTED         = "q.order.order-rejected";
    public static final String Q_ORDER_READY            = "q.order.order-ready-for-pickup";
    public static final String Q_ORDER_PICKED_UP        = "q.order.order-picked-up";
    public static final String Q_ORDER_DELIVERED        = "q.order.order-delivered";
    public static final String Q_DLQ                     = "q.dlq";

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

    /**
     * Helper: a queue with a dead-letter exchange argument — used by all consumer queues for poison handling.
     */
    private Queue consumerQueue(String name) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(DLX_NAME)
                .build();
    }

    @Bean
    Queue qOrderAccepted() { return consumerQueue(Q_ORDER_ACCEPTED); }
    @Bean
    Queue qOrderRejected()  { return consumerQueue(Q_ORDER_REJECTED); }
    @Bean
    Queue qOrderReady()     { return consumerQueue(Q_ORDER_READY); }
    @Bean
    Queue qOrderPickedUp()  { return consumerQueue(Q_ORDER_PICKED_UP); }
    @Bean
    Queue qOrderDelivered() { return consumerQueue(Q_ORDER_DELIVERED); }

    @Bean
    Binding bOrderAccepted() {
        return BindingBuilder.bind(qOrderAccepted()).to(kdgEventsExchange()).with("order.accepted");
    }
    @Bean
    Binding bOrderRejected() {
        return BindingBuilder.bind(qOrderRejected()).to(kdgEventsExchange()).with("order.rejected");
    }
    @Bean
    Binding bOrderReady() {
        return BindingBuilder.bind(qOrderReady()).to(kdgEventsExchange()).with("order.ready_for_pickup");
    }
    @Bean
    Binding bOrderPickedUp() {
        return BindingBuilder.bind(qOrderPickedUp()).to(kdgEventsExchange()).with("order.picked_up");
    }
    @Bean
    Binding bOrderDelivered() {
        return BindingBuilder.bind(qOrderDelivered()).to(kdgEventsExchange()).with("order.delivered");
    }

    @Bean
    MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}