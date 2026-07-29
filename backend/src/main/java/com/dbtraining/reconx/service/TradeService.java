package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.exception.DuplicateTradeRefException;
import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.observability.TradeMetrics;
import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Counterparty;
import com.dbtraining.reconx.repository.entity.Instrument;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.dto.TradeEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static com.dbtraining.reconx.repository.TradeSpecifications.*;

/**
 * ============================================================================
 * TICKET-ADV064 — TradeService.create (POST endpoint backing)
 * TICKET-ADV065 — update
 * TICKET-ADV066 — updateStatus (PATCH)
 * TICKET-ADV067 — softDelete
 * TICKET-ADV083 — increments trade_creation_total Counter on create
 * TICKET-ADV129 — publishes TradeEvent on every state change
 * TICKET-ADV055/ADV056 — list() uses Specifications + filter query
 * ============================================================================
 */
@Service
@Transactional
public class TradeService {

    private final TradeRepository tradeRepo;
    private final CounterpartyRepository cpRepo;
    private final InstrumentRepository instRepo;
    private final TradeEventProducer events;
    private final TradeMetrics metrics;

    public TradeService(TradeRepository tradeRepo,
                        CounterpartyRepository cpRepo,
                        InstrumentRepository instRepo,
                        TradeEventProducer events,
                        TradeMetrics metrics) {
        this.tradeRepo = tradeRepo;
        this.cpRepo = cpRepo;
        this.instRepo = instRepo;
        this.events = events;
        this.metrics = metrics;
    }

    public Trade create(TradeRequest req, String actor) {
        // (TICKET-ADV064): reject duplicate tradeRef via DuplicateTradeRefException,
        //   build a new Trade with instrument + counterparty looked up from
        //   their repos (throw TradeNotFoundException on miss), status = "PENDING",
        //   save, then:
        //     - metrics.incrementTradeCreated() + metrics.recordTradeValue(qty*price) — TICKET-ADV083
        //     - events.publish(new TradeEvent(... TRADE_CREATED ... actor ...)) — TICKET-ADV129

        if (tradeRepo.findByTradeRef(req.tradeRef()).isPresent()) {
            throw new DuplicateTradeRefException(req.tradeRef());
        }

        Instrument instrument = instRepo.findById(req.instrumentId())
                .orElseThrow(() -> new TradeNotFoundException(
                        "Instrument not found: " + req.instrumentId()));

        Counterparty counterparty = cpRepo.findById(req.counterpartyId())
                .orElseThrow(() -> new TradeNotFoundException(
                        "Counterparty not found: " + req.counterpartyId()));

        Trade trade = new Trade();
        trade.setTradeRef(req.tradeRef());
        trade.setInstrument(instrument);
        trade.setCounterparty(counterparty);
        trade.setQuantity(req.quantity());
        trade.setPrice(req.price());
        trade.setAssetClass(req.assetClass());
        trade.setSide(req.side());
        trade.setTradeDate(req.tradeDate());
        trade.setStatus("PENDING");

        Trade saved = tradeRepo.save(trade);
        metrics.incrementTradeCreated();

        return saved;
    }

    public Trade update(Long id, TradeRequest req, String actor) {
        // (TICKET-ADV065): load by id (throw TradeNotFoundException if missing),
        //   copy mutable fields from req, save, publish a TRADE_UPDATED event.
        Trade trade = tradeRepo.findById(id)
                .orElseThrow(() -> new TradeNotFoundException(req.tradeRef()));

        Instrument instrument = instRepo.findById(req.instrumentId())
                .orElseThrow(() -> new TradeNotFoundException(req.instrumentId().toString()));

        Counterparty counterparty = cpRepo.findById(req.counterpartyId())
                .orElseThrow(() -> new TradeNotFoundException(req.counterpartyId().toString()));

        trade.setInstrument(instrument);
        trade.setCounterparty(counterparty);
        trade.setQuantity(req.quantity());
        trade.setPrice(req.price());
        trade.setAssetClass(req.assetClass());
        trade.setSide(req.side());
        trade.setTradeDate(req.tradeDate());

        Trade saved = tradeRepo.save(trade);

        return saved;
    }

    public Trade updateStatus(Long id, String status, String actor) {
        // (TICKET-ADV066): load, setStatus(status), save, publish TRADE_UPDATED
        //   with the new status in the "after" slot of the event.
        Trade trade = tradeRepo.findByIdWithDetails(id)
                .orElseThrow(() -> new TradeNotFoundException(id.toString()));

        String previousStatus = trade.getStatus();

        trade.setStatus(status);
        Trade saved = tradeRepo.save(trade);

        /*events.publish(new TradeEvent(
                UUID.randomUUID(),
                saved.getTradeRef(),
                TradeEvent.EventType.TRADE_UPDATED,
                Instant.now(),
                actor,
                previousStatus,
                status
        ));
*/
        return saved;
    }

    public void softDelete(Long id, String actor) {
        // (TICKET-ADV067): load, call t.softDelete() (sets deleted_at), save,
        //   publish a TRADE_CANCELLED event.
        Trade t = tradeRepo.findById(id)
                .orElseThrow(() -> new TradeNotFoundException(id.toString()));
        t.softDelete();
        tradeRepo.save(t);
        //events.publish(new TradeEvent(UUID.randomUUID(), t.getTradeRef(),
        //        TradeEvent.EventType.TRADE_CANCELLED, Instant.now(), actor, null, null));
    }

    @Transactional(readOnly = true)
    public Page<Trade> list(LocalDate from, LocalDate to, String status, Long counterpartyId, Pageable pageable) {
        Specification<Trade> spec = Specification
                .where(tradeDateBetween(from, to))
                .and(hasStatus(status))
                .and(hasCounterparty(counterpartyId))
                .and((root, query, cb) -> {
                    // open-in-view is disabled, so instrument/counterparty (both LAZY) must be
                    // fetch-joined here or TradeMapper throws LazyInitializationException once
                    // this transaction/session closes. Skip on the COUNT query used for paging.
                    if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                        root.fetch("instrument", jakarta.persistence.criteria.JoinType.LEFT);
                        root.fetch("counterparty", jakarta.persistence.criteria.JoinType.LEFT);
                    }
                    return cb.conjunction();
                });
        return tradeRepo.findAll(spec, pageable);
    }
}
