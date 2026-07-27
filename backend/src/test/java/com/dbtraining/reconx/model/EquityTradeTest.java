package com.dbtraining.reconx.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class EquityTradeTest {

    @Test
    void builder_buildsWhenAllRequiredPresent() {
        TradeRef ref = TradeRef.of("EQU-20260602-0001");

        EquityTrade trade = EquityTrade.builder()
                .tradeRef(ref)
                .instrumentSymbol("SAP.DE")
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("100"))
                .currency("EUR").side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L)
                .build();

        assertEquals(ref, trade.tradeRef());
        assertEquals(TradeType.AssetClass.EQUITY, trade.assetClass());
        assertEquals(new Money(new BigDecimal("10000"), Currency.getInstance("EUR")), trade.notional());
    }

    @Test
    void builder_missingPrice_throws() {
        TradeRef ref = TradeRef.of("EQU-20260602-0001");

        assertThatThrownBy(() ->
                EquityTrade.builder()
                .tradeRef(ref)
                .instrumentSymbol("SAP.DE")
                .quantity(new BigDecimal("100"))
                //.price(new BigDecimal("100"))
                .currency("EUR").side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L)
                .build()
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("price");
    }

    @Test
    void equality_byTradeRef() {
        EquityTrade trade1 = sampleEquity("EQU-20260602-0001");
        EquityTrade trade2 = sampleEquity("EQU-20260602-0001");
        EquityTrade trade3 = sampleEquity("EQU-20260602-0002");

        assertEquals(trade1, trade2);
        assertEquals(trade1.hashCode(), trade2.hashCode());
        assertNotEquals(trade1, trade3);
    }

    private EquityTrade sampleEquity(String ref) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .instrumentSymbol("SAP.DE")
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("100"))
                .currency("EUR").side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L).build();
    }
}
