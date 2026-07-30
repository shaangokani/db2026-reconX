package com.dbtraining.reconx.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * ============================================================================
 * TICKET-ADV134 — DLQ via DeadLetterPublishingRecoverer (failed messages
 *                routed to {topic}-dlq with the same partition number)
 * TICKET-ADV135 — Retry strategy: 3 attempts with exponential backoff
 *                (1s, 2s, 4s) before giving up to DLQ
 *
 * WHAT:    Spring Kafka error handler that retries with backoff and on
 *          final failure publishes the poison record to the corresponding
 *          DLQ topic.
 * HOW:     One @Bean DefaultErrorHandler combining a
 *          DeadLetterPublishingRecoverer + ExponentialBackOff. Spring Boot's
 *          autoconfigured listener container factory picks up a single
 *          unqualified DefaultErrorHandler bean automatically — no need to
 *          name/wire a custom container factory.
 * WHY:     Without this, an exception in a listener kills the consumer
 *          thread and the whole partition stalls. With it, retries happen,
 *          and a final failure is observable (DLQ topic) rather than lost.
 * OBSERVE: Force an exception in a consumer — Kafdrop should show the
 *          record on `trade-events-dlq` with the same partition as the
 *          original.
 *
 *  GOTCHA: trade-events-dlq must already exist (TICKET-ADV128). The
 *          recoverer does NOT auto-create the topic.
 * ============================================================================
 */
@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                template,
                (ConsumerRecord<?, ?> rec, Exception ex) ->
                        new TopicPartition(rec.topic() + "-dlq", rec.partition())
        );

        // 1s, 2s, 4s — three attempts total, then DLQ.
        ExponentialBackOff backoff = new ExponentialBackOff(1000L, 2.0);
        backoff.setMaxAttempts(3);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backoff);
        // Poison-pill (bad JSON) messages skip straight to the DLQ instead of
        // burning three retries that can never succeed.
        handler.addNotRetryableExceptions(DeserializationException.class, IllegalArgumentException.class);
        return handler;
    }
}
