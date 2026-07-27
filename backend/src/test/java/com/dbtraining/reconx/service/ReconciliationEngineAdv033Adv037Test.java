package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.Side;
import com.dbtraining.reconx.model.TradeRef;
import com.dbtraining.reconx.model.TradeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationEngineAdv033Adv037Test {

    private final ReconciliationEngine engine = new ReconciliationEngine();

    @AfterEach
    void tearDown() {
        engine.shutdown();
    }

    @Test
    void reconcile_handlesMatchMismatchAndMissingExternal() {
        EquityTrade in1 = equity("EQU-20260603-0101", "100.00", "1000", 1L);
        EquityTrade in2 = equity("EQU-20260603-0102", "100.00", "1000", 1L);
        EquityTrade in3 = equity("EQU-20260603-0103", "100.00", "1000", 2L);

        EquityTrade ex1 = equity("EQU-20260603-0101", "100.00", "1000", 1L); // matched
        EquityTrade ex2 = equity("EQU-20260603-0102", "120.00", "1000", 1L); // value mismatch
        // no ex3 to trigger missing external

        List<ReconResult> out = engine.reconcile(
                List.of(in1, in2, in3),
                List.of(ex1, ex2),
                ReconciliationRule.EXACT);

        assertThat(out).hasSize(3);
        Map<String, ReconResult> byRef = out.stream().collect(java.util.stream.Collectors.toMap(ReconResult::tradeRef, r -> r));
        assertThat(byRef.get("EQU-20260603-0101").status()).isEqualTo(ReconResult.Status.MATCHED);
        assertThat(byRef.get("EQU-20260603-0102").discrepancyType()).isEqualTo("VALUE_MISMATCH");
        assertThat(byRef.get("EQU-20260603-0103").discrepancyType()).isEqualTo("MISSING_EXTERNAL");
    }

    @Test
    void reconcileByCounterparty_mergesAllCounterpartyResults() {
        Map<Long, List<TradeType>> internalByCp = Map.of(
                1L, List.of(equity("EQU-20260603-0201", "100.00", "1000", 1L)),
                2L, List.of(equity("EQU-20260603-0202", "100.00", "1000", 2L))
        );

        Map<Long, List<TradeType>> externalByCp = Map.of(
                1L, List.of(equity("EQU-20260603-0201", "100.00", "1000", 1L))
                // cp2 missing external batch -> break
        );

        List<ReconResult> out = engine.reconcileByCounterparty(
                internalByCp,
                externalByCp,
                ReconciliationRule.EXACT).join();

        assertThat(out).hasSize(2);
        assertThat(out.stream().filter(r -> r.status() == ReconResult.Status.MATCHED).count()).isEqualTo(1);
        assertThat(out.stream().filter(r -> "MISSING_EXTERNAL".equals(r.discrepancyType())).count()).isEqualTo(1);
    }

    private EquityTrade equity(String ref, String price, String qty, long counterpartyId) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .instrumentSymbol("SAP.DE")
                .price(new BigDecimal(price))
                .quantity(new BigDecimal(qty))
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(counterpartyId)
                .build();
    }
}

