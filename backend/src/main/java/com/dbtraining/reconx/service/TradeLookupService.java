package com.dbtraining.reconx.service;

import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Counterparty;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

/**
 * ============================================================================
 * TICKET-ADV039 — Optional chaining for null-safe lookups
 *
 * WHAT:    Resolves a trade reference to its Counterparty using a single
 *          Optional chain — no if-null checks, no isPresent(), no .get().
 * HOW:     tradeRepo.findByTradeRef(ref)
 *              .map(Trade::getCounterparty)
 *              .orElseThrow(...)
 * WHY:     The Optional.map/flatMap/orElseThrow discipline keeps service code
 *          free of nested null checks. Day 4's controllers reuse this pattern
 *          for 404 handling.
 * OBSERVE: grep -E 'isPresent|\.get\(\)' on this file returns zero hits.
 * ============================================================================
 */
@Service
public class TradeLookupService {

    private final TradeRepository tradeRepo;
    private final CounterpartyRepository cpRepo;

    public TradeLookupService(TradeRepository tradeRepo, CounterpartyRepository cpRepo) {
        this.tradeRepo = tradeRepo;
        this.cpRepo = cpRepo;
    }

    /**
     * Walk from trade reference → Trade → Counterparty in a single Optional chain.
     *
     * @param tradeRef the unique trade reference to look up
     * @return the resolved Counterparty
     * @throws NoSuchElementException if no trade exists for the given ref,
     *                                 or the trade has no counterparty
     */
    public Counterparty counterpartyForTradeRef(String tradeRef) {
        return tradeRepo.findByTradeRef(tradeRef)
                .map(trade -> trade.getCounterparty())
                .orElseThrow(() -> new NoSuchElementException(
                        "No counterparty resolvable for trade " + tradeRef));
    }
}
