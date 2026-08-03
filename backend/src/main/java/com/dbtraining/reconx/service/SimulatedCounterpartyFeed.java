package com.dbtraining.reconx.service;

import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.TradeType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * Stand-in for the counterparty trade feed.
 *
 * There is no real external feed yet — consuming one is Day 9's Kafka work.
 * Reconciling the internal book against itself (what runRecon did before)
 * matches every row by definition and can never raise a break, so the engine,
 * the recon_breaks table and the Recon Breaks page all looked broken when they
 * were in fact never given anything to disagree about.
 *
 * This derives a counterparty view from the internal book and injects the
 * discrepancies you would actually expect to see: a slightly different fill
 * price, a partial fill, or a trade the counterparty never booked. The
 * comparison itself is done by the real ReconciliationEngine — nothing here
 * fabricates a break, it only supplies the other side of the comparison.
 *
 * Deterministic: the perturbation is chosen from a hash of the tradeRef, so a
 * given book always yields the same breaks and a demo is reproducible.
 * ============================================================================
 */
@Service
public class SimulatedCounterpartyFeed {

    /**
     * 3 of every 20 trades disagree — a ~15% break rate. High for a real desk,
     * but the point is a demo where every break type is visibly represented
     * without the match rate looking broken.
     */
    private static final int BUCKETS = 20;

    public List<TradeType> deriveFrom(List<TradeType> internal) {
        List<TradeType> external = new ArrayList<>(internal.size());
        for (TradeType t : internal) {
            if (!(t instanceof EquityTrade e)) {
                external.add(t);
                continue;
            }
            switch (bucket(e)) {
                // price disagreement, ~1.6% out: beyond PRICE_TOLERANCE_1PCT.
                case 17 -> external.add(copyWith(e, e.price().multiply(new BigDecimal("1.016"))
                        .setScale(4, RoundingMode.HALF_UP), e.quantity()));
                // partial fill — counterparty booked ~10% fewer units.
                case 18 -> external.add(copyWith(e, e.price(),
                        e.quantity().multiply(new BigDecimal("0.9")).setScale(4, RoundingMode.HALF_UP)));
                // counterparty has no record of it at all: omitted entirely,
                // which the engine reports as MISSING_EXTERNAL.
                case 19 -> { }
                // everything else → counterparty agrees exactly.
                default -> external.add(e);
            }
        }
        return external;
    }

    private static int bucket(EquityTrade e) {
        return Math.floorMod(e.tradeRef().value().hashCode(), BUCKETS);
    }

    private static EquityTrade copyWith(EquityTrade e, BigDecimal price, BigDecimal quantity) {
        return EquityTrade.builder()
                .tradeRef(e.tradeRef())
                .instrumentSymbol(e.instrumentSymbol())
                .quantity(quantity)
                .price(price)
                .currency(e.currency())
                .side(e.side())
                .tradeDate(e.tradeDate())
                .counterpartyId(e.counterpartyId())
                .build();
    }
}
