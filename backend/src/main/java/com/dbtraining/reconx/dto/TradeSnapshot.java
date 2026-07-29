package com.dbtraining.reconx.dto;

import com.dbtraining.reconx.repository.entity.Trade;

import java.math.BigDecimal;
import java.time.LocalDate;

/** TICKET-ADV071 — scalar trade state at a given Envers revision. */
public record TradeSnapshot(
        Long id,
        String tradeRef,
        Long instrumentId,
        Long counterpartyId,
        String assetClass,
        String side,
        BigDecimal quantity,
        BigDecimal price,
        LocalDate tradeDate,
        String status,
        boolean deleted
) {
    public static TradeSnapshot from(Trade t) {
        return new TradeSnapshot(
                t.getId(),
                t.getTradeRef(),
                t.getInstrument() != null ? t.getInstrument().getId() : null,
                t.getCounterparty() != null ? t.getCounterparty().getId() : null,
                t.getAssetClass(),
                t.getSide(),
                t.getQuantity(),
                t.getPrice(),
                t.getTradeDate(),
                t.getStatus(),
                t.getDeletedAt() != null);
    }
}
