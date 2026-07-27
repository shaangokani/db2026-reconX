package com.dbtraining.reconx.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EquityTradeTest {

    @Test
    void builder_buildsWhenAllRequiredPresent() {
        // TODO(TICKET-ADV019): build an EquityTrade via the Builder with all required fields,
        //                     then assert tradeRef, notional (price*qty) and assetClass = EQUITY.
        org.junit.jupiter.api.Assertions.fail("TICKET-ADV019 not implemented yet");
    }

    @Test
    void builder_missingPrice_throws() {
        // TODO(TICKET-ADV019): omit .price(...) on the Builder and assert build() throws
        //                     NullPointerException whose message mentions "price".
        org.junit.jupiter.api.Assertions.fail("TICKET-ADV019 not implemented yet");
    }

    @Test
    void equality_byTradeRef() {
        // TODO(TICKET-ADV028): two EquityTrades with the same tradeRef are equal and share hashCode;
        //                     a third with a different tradeRef is not equal.
        EquityTrade t1 = sampleEquity("EQU-20260603-0001");
        EquityTrade t2 = EquityTrade.builder()
                .tradeRef(TradeRef.of("EQU-20260603-0001"))  // Same tradeRef
                .instrumentSymbol("SAP.DE")
                .quantity(new BigDecimal("50"))  // Different quantity
                .price(new BigDecimal("200"))     // Different price
                .currency("EUR").side(Side.SELL)  // Different side
                .tradeDate(LocalDate.of(2026, 6, 4))  // Different date
                .counterpartyId(2L).build();
        
        EquityTrade t3 = sampleEquity("EQU-20260603-0002");  // Different tradeRef
        
        assertThat(t1.equals(t2)).isTrue();
        assertThat(t1.hashCode() == t2.hashCode()).isTrue();
        assertThat(t1.equals(t3)).isFalse();
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
