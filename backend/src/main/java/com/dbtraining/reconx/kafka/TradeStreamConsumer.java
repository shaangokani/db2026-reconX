package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.sse.TradeStreamRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ============================================================================
 * Backend half of the day7/day8 live trade feed (TICKET-ADV104 / ADV116) —
 * turns each TradeEvent on trade-events into a {tradeRef, symbol, qty,
 * price, status} row for every browser connected to GET /v1/trades/stream.
 *
 * HOW:     @KafkaListener on trade-events, groupId sse-stream — a separate
 *          consumer group so this fan-out never competes with
 *          ReconciliationConsumer/AuditEventConsumer for partitions.
 * GOTCHA:  TradeEvent only carries opaque before/after state strings, not
 *          the structured fields the UI needs (symbol/qty/price) — so this
 *          re-reads the current row by tradeRef rather than trying to shape
 *          the SSE payload from the event alone.
 * ============================================================================
 */
@Component
public class TradeStreamConsumer {

    private static final Logger log = LoggerFactory.getLogger(TradeStreamConsumer.class);

    private final TradeRepository trades;
    private final TradeStreamRegistry registry;

    public TradeStreamConsumer(TradeRepository trades, TradeStreamRegistry registry) {
        this.trades = trades;
        this.registry = registry;
    }

    @KafkaListener(topics = "trade-events", groupId = "sse-stream")
    @Transactional(readOnly = true)
    public void onTradeEvent(TradeEvent event) {
        // TradeService publishes from inside its @Transactional method, so this
        // consumer can race the DB commit and momentarily see nothing for a
        // brand-new tradeRef. Throwing (instead of silently skipping) lets the
        // shared DefaultErrorHandler (TICKET-ADV134) retry at 1s/2s/4s, which is
        // far longer than a local commit ever takes — a genuinely missing trade
        // still ends up on trade-events-dlq after three tries, which is the
        // right outcome for a row that should exist but doesn't.
        //
        // @Transactional also keeps the Hibernate session open across the
        // getInstrument() lazy-load in broadcast() below — without it, the
        // repository call's own transaction closes on return and t.getInstrument()
        // throws LazyInitializationException ("no session").
        Trade t = trades.findByTradeRef(event.tradeRef())
                .orElseThrow(() -> new IllegalStateException("SSE: trade not found for tradeRef=" + event.tradeRef()));
        broadcast(t);
    }

    private void broadcast(Trade t) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tradeRef", t.getTradeRef());
        payload.put("symbol", t.getInstrument() != null ? t.getInstrument().getSymbol() : null);
        payload.put("qty", t.getQuantity());
        payload.put("price", t.getPrice());
        payload.put("status", t.getStatus());
        registry.broadcast(payload);
    }
}
