package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.service.ReconciliationEngine;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * ============================================================================
 * TICKET-ADV144 — Proves a failing listener routes to trade-events-dlq after
 * TICKET-ADV135's retries are exhausted (producer -> topic -> throwing
 * listener -> retry x3 -> TICKET-ADV134's recoverer -> DLQ topic).
 *
 * NOTE: the guide's own reference mocks scheduleRecon(anyString()) — this
 * codebase's real ReconciliationEngine.scheduleRecon takes a TradeEvent, not
 * a String, so the mock below matches on any(TradeEvent.class) instead.
 * ============================================================================
 */
@SpringBootTest
@Testcontainers
class DlqRoutingIT {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
    );

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    TradeEventProducer producer;

    @MockBean
    ReconciliationEngine reconEngine;

    @Test
    void failingConsumerRoutesToDlq() {
        Mockito.doThrow(new RuntimeException("boom"))
                .when(reconEngine).scheduleRecon(Mockito.any(TradeEvent.class));

        TradeEvent event = new TradeEvent(
                UUID.randomUUID(),
                "TRD-DLQ-1",
                TradeEvent.EventType.TRADE_CREATED,
                Instant.now(),
                "integration-test",
                null,
                "PENDING"
        );
        producer.publish(event);

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(dlqHas("TRD-DLQ-1")).isTrue());
    }

    private boolean dlqHas(String tradeRef) {
        Properties p = new Properties();
        p.put(BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        p.put(GROUP_ID_CONFIG, "dlq-assert-" + System.nanoTime());
        p.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        p.put(VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        p.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        // The producer never adds type headers (spring.json.add.type.headers:
        // false in application.yml), so without a default type this raw
        // consumer can't know what class to deserialize the value into and
        // throws RecordDeserializationException.
        p.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TradeEvent.class.getName());

        try (var consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<String, TradeEvent>(p)) {
            consumer.subscribe(java.util.List.of("trade-events-dlq"));
            var records = consumer.poll(Duration.ofSeconds(5));
            for (ConsumerRecord<String, TradeEvent> r : records) {
                if (tradeRef.equals(r.value().tradeRef())) return true;
            }
        }
        return false;
    }
}
