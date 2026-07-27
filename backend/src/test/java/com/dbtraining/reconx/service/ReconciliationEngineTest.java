package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-ADV040 / ADV041 / ADV042 — TDD: write the test FIRST, then the impl.
 */
class ReconciliationEngineTest {

    private final ReconciliationEngine engine = new ReconciliationEngine();

    // ── TICKET-ADV040 — exact-match TDD cycle ──────────────────────────────

    @Test
    @DisplayName("exact match on price and qty returns MATCHED")
    void testReconcile_exactMatch_returnsMatched() {
        // given
        EquityTrade internal = equity("EQU-20260603-0001", "100.00", "1000");
        EquityTrade external = equity("EQU-20260603-0001", "100.00", "1000");

        // when
        List<ReconResult> out = engine.reconcile(
                List.of(internal), List.of(external), ReconciliationRule.EXACT);

        // then
        assertThat(out).hasSize(1);
        assertThat(out.get(0).status()).isEqualTo(ReconResult.Status.MATCHED);
    }

    @Test
    @DisplayName("empty internal list returns empty result list")
    void testReconcile_emptyInternal_returnsEmpty() {
        // given — empty lists on both sides

        // when
        List<ReconResult> out = engine.reconcile(List.of(), List.of(), ReconciliationRule.EXACT);

        // then
        assertThat(out).isEmpty();
    }

    // ── TICKET-ADV041 — tolerance test with @ParameterizedTest ─────────────

    @ParameterizedTest(name = "price diff {0} stays within 1% tolerance -> MATCHED")
    @ValueSource(strings = {"0.10", "0.50", "0.99"})
    void testReconcile_priceTolerance_withinThreshold(String diff) {
        // given
        BigDecimal basePrice = new BigDecimal("100.00");
        BigDecimal externalPrice = basePrice.add(new BigDecimal(diff));

        EquityTrade internal = equity("EQU-20260603-0002", "100.00", "1000");
        EquityTrade external = equity("EQU-20260603-0002", externalPrice.toPlainString(), "1000");

        // when
        List<ReconResult> out = engine.reconcile(
                List.of(internal), List.of(external), ReconciliationRule.PRICE_TOLERANCE_1PCT);

        // then
        assertThat(out).hasSize(1);
        assertThat(out.get(0).status()).isEqualTo(ReconResult.Status.MATCHED);
    }

    // ── TICKET-ADV042 — missing counterparty trade (stub for next ticket) ──

    @Test
    @DisplayName("missing external trade returns BREAK with MISSING_EXTERNAL")
    void testReconcile_missingCounterpartyTrade_returnsBreak() {
        EquityTrade internal = equity("EQU-20260603-0003", "100.00", "1000");

        List<ReconResult> out = engine.reconcile(List.of(internal), List.of(), ReconciliationRule.EXACT);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).status()).isEqualTo(ReconResult.Status.BREAK);
        assertThat(out.get(0).discrepancyType()).isEqualTo("MISSING_EXTERNAL");
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    private EquityTrade equity(String ref, String price, String qty) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .instrumentSymbol("SAP.DE")
                .price(new BigDecimal(price))
                .quantity(new BigDecimal(qty))
                .currency("EUR").side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L)
                .build();
    }
}
