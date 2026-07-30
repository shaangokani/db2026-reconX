package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.DlqMessageRepository;
import com.dbtraining.reconx.repository.entity.DlqMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * ============================================================================
 * TICKET-ADV136 — DlqConsumer
 *
 * WHAT:    Persists every message that lands on trade-events-dlq (after
 *          TICKET-ADV134's retries are exhausted) as a DlqMessage row.
 * HOW:     @KafkaListener on trade-events-dlq, groupId dlq-monitor. Stores
 *          the TradeEvent payload as JSON text so DlqAdminController can
 *          replay it later without needing the original Kafka record.
 * WHY:     One-at-a-time, inspectable replay beats a bulk "replay everything"
 *          button — see DlqAdminController.
 * ============================================================================
 */
@Component
public class DlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(DlqConsumer.class);

    private final DlqMessageRepository repo;
    private final ObjectMapper objectMapper;

    public DlqConsumer(DlqMessageRepository repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "trade-events-dlq", groupId = "dlq-monitor")
    public void onDlqMessage(ConsumerRecord<String, TradeEvent> record,
                             @Header(value = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) String exMsg) {
        TradeEvent event = record.value();
        log.error("DLQ: trade={} eventId={} reason={}",
                event != null ? event.tradeRef() : null,
                event != null ? event.eventId() : null,
                exMsg);

        try {
            repo.save(new DlqMessage(
                    event != null ? event.eventId().toString() : record.key(),
                    event != null ? event.tradeRef() : null,
                    record.topic().replace("-dlq", ""),
                    record.partition(),
                    record.offset(),
                    event != null ? objectMapper.writeValueAsString(event) : null,
                    exMsg,
                    Instant.now()
            ));
        } catch (Exception e) {
            log.error("Failed to persist DLQ row for offset={}", record.offset(), e);
        }
    }
}
