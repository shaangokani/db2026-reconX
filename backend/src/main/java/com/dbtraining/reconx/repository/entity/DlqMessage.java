package com.dbtraining.reconx.repository.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * TICKET-ADV136 — One row per message that landed on a *-dlq topic after
 * DefaultErrorHandler (TICKET-ADV134) exhausted retries.
 */
@Entity
@Table(name = "dlq_messages")
public class DlqMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "trade_ref", length = 30)
    private String tradeRef;

    @Column(name = "original_topic", nullable = false, length = 100)
    private String originalTopic;

    @Column(name = "partition_no", nullable = false)
    private int partition;

    @Column(name = "kafka_offset", nullable = false)
    private long offset;

    // JSON text of the original TradeEvent, so a replay can deserialize and
    // republish it without needing the raw Kafka record any more.
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "first_seen", nullable = false)
    private Instant firstSeen;

    public DlqMessage() {}

    public DlqMessage(String eventId, String tradeRef, String originalTopic, int partition,
                       long offset, String payload, String reason, Instant firstSeen) {
        this.eventId = eventId;
        this.tradeRef = tradeRef;
        this.originalTopic = originalTopic;
        this.partition = partition;
        this.offset = offset;
        this.payload = payload;
        this.reason = reason;
        this.firstSeen = firstSeen;
    }

    public Long getId()             { return id; }
    public String getEventId()      { return eventId; }
    public String getTradeRef()     { return tradeRef; }
    public String getOriginalTopic(){ return originalTopic; }
    public int getPartition()       { return partition; }
    public long getOffset()         { return offset; }
    public String getPayload()      { return payload; }
    public String getReason()       { return reason; }
    public Instant getFirstSeen()   { return firstSeen; }
}
