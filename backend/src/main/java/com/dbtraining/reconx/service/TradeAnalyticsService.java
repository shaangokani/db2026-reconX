package com.dbtraining.reconx.service;

import com.dbtraining.reconx.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * TICKET-ADV034 — Trade analytics with Collectors (groupingBy + summarizing)
 * TICKET-ADV035 — VWAP calculator using Streams + custom collector
 * TICKET-ADV036 — P&L per instrument: stream reduction
 * ============================================================================
 */
@Service
public class TradeAnalyticsService {

    /** TICKET-ADV034 — count + sum of notional per counterparty. */
    public Map<Long, NotionalSummary> notionalByCounterparty(List<? extends TradeType> trades) {
        return trades.stream().collect(Collectors.groupingBy(
                t -> counterpartyIdOf(t),
                Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> new NotionalSummary(
                                list.size(),
                                list.stream()
                                        .map(t -> t.notional().amount())
                                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                )));
    }

    /**
     * TICKET-ADV035 — VWAP = SUM(price * qty) / SUM(qty). Equity-only — only
     * EquityTrade has a meaningful price-volume pair.
     */
    public Map<String, BigDecimal> vwapByInstrument(List<EquityTrade> equityTrades) {
        //   Group by EquityTrade::instrumentSymbol, then for
        //   each bucket compute SUM(price * qty) / SUM(qty) using BigDecimal
        //   with RoundingMode.HALF_UP. Return BigDecimal.ZERO when totalQty is 0
        //   (avoid ArithmeticException on division by zero).
        Map<String, List<EquityTrade>> bySymbol = equityTrades.stream()
                .collect(Collectors.groupingBy(EquityTrade::instrumentSymbol));

        return bySymbol.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> {
                    BigDecimal totalQty = e.getValue().stream()
                            .map(EquityTrade::quantity)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    if (totalQty.signum() == 0) return BigDecimal.ZERO;
                    BigDecimal weighted = e.getValue().stream()
                            .map(t -> t.price().multiply(t.quantity()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return weighted.divide(totalQty, 4, RoundingMode.HALF_UP);
                }
        ));
    }

    /** TICKET-ADV036 — P&L per instrument symbol (sign by Side). */
    public Map<String, BigDecimal> pnlByInstrument(List<EquityTrade> equityTrades) {
        //  (TICKET-ADV036): groupingBy(EquityTrade::instrumentSymbol,
        //   mapping(this::pnl, reducing(BigDecimal.ZERO, BigDecimal::add))).
        //   Side.SELL contributes positively; Side.BUY contributes negatively.
        return equityTrades.stream().collect(Collectors.groupingBy(
                EquityTrade::instrumentSymbol,
                Collectors.mapping(this::pnl,
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
        ));
    }

    private BigDecimal pnl(EquityTrade t) {
        // (TICKET-ADV036): BigDecimal abs = price * qty; SELL -> abs, BUY -> abs.negate().
        BigDecimal abs = t.price().multiply(t.quantity());
        return t.side() == Side.SELL ? abs : abs.negate();
    }

    private long counterpartyIdOf(TradeType t) {
        return switch (t) {
            case EquityTrade e -> e.counterpartyId();
            case FXTrade fx -> fx.counterpartyId();
            case BondTrade b -> b.counterpartyId();
            case DerivativeTrade d -> d.counterpartyId();
        };
    }

    public record NotionalSummary(long count, BigDecimal total) {}
}
