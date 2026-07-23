package be.kdg.backend.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for delivery-service.
 *
 * Exchange:     kdg.events (shared topic exchange)
 * Consumer queues:
 *   q.delivery.order-accepted        bound order.accepted
 *   q.delivery.order-ready-for-pickup bound order.ready_for_pickup
 *
 * Producers (to same exchange):
 *   order.picked_up      (routing key)
 *   order.delivered       (routing key)
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_NAME = "kdg.events";
    public static final String DLX_NAME       = "kdg.events.dlx";

    public static final String Q_DELIVERY_ORDER_ACCEPTED = "q.delivery.order-accepted";
    public static final String Q_DELIVERY_ORDER_READY    = "q.delivery.order-ready-for-pickup";
    public static final String Q_DLQ                      = "q.dlq.delivery";

    @Bean
    TopicExchange kdgEventsExchange() { return ExchangeBuilder.topicExchange(EXCHANGE_NAME).durable(true).build(); }
    @Bean
    TopicExchange kdgEventsDlx()       { return ExchangeBuilder.topicExchange(DLX_NAME).durable(true).build(); }

    @Bean
    Queue dlq() { return QueueBuilder.durable(Q_DLQ).build(); }
    @Bean
    Binding dlqBinding() { return BindingBuilder.bind(dlq()).to(kdgEventsDlx()).with("#"); }

    private Queue consumerQueue(String name) {
        return QueueBuilder.durable(name).deadLetterExchange(DLX_NAME).build();
    }

    @Bean
    Queue qOrderAccepted() { return consumerQueue(Q_DELIVERY_ORDER_ACCEPTED); }
    @Bean
    Queue qOrderReady()    { return consumerQueue(Q_DELIVERY_ORDER_READY); }

    @Bean
    Binding bOrderAccepted() { return BindingBuilder.bind(qOrderAccepted()).to(kdgEventsExchange()).with("order.accepted"); }
    @Bean
    Binding bOrderReady()    { return BindingBuilder.bind(qOrderReady()).to(kdgEventsExchange()).with("order.ready_for_pickup"); }

    @Bean
    MessageConverter jackson2JsonMessageConverter() { return new Jackson2JsonMessageConverter(); }
}