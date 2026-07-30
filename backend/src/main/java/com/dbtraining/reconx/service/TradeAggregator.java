package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * TICKET-ADV137 — Event sourcing rebuild
 *
 * WHAT:    Rebuilds a trade's current state by folding every audit_log row
 *          for its tradeRef, oldest first.
 * HOW:     TRADE_CREATED / TRADE_UPDATED set running state to the event's
 *          after-snapshot; TRADE_CANCELLED clears it to null.
 * WHY:     Proves the system can reconstruct a trade from its event log
 *          alone — the payoff of AuditEventConsumer (TICKET-ADV132).
 * ============================================================================
 */
@Service
public class TradeAggregator {

    private final AuditLogRepository auditRepo;

    public TradeAggregator(AuditLogRepository auditRepo) {
        this.auditRepo = auditRepo;
    }

    public Optional<String> rebuild(String tradeRef) {
        List<AuditLogEntry> events = auditRepo.findByTradeRefOrderByEventTimestampAsc(tradeRef);
        if (events.isEmpty()) {
            return Optional.empty();
        }

        String state = null;
        for (AuditLogEntry e : events) {
            TradeEvent.EventType type = TradeEvent.EventType.valueOf(e.getEventType());
            switch (type) {
                case TRADE_CREATED, TRADE_UPDATED -> state = e.getAfterState();
                case TRADE_CANCELLED -> state = null;
            }
        }
        return Optional.ofNullable(state);
    }
}
