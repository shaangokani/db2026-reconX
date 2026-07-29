package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.dto.TradeResponse;
import com.dbtraining.reconx.repository.entity.Counterparty;
import com.dbtraining.reconx.repository.entity.Instrument;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.service.TradeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TradeController.class)
class TradeControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private TradeService tradeService;

    private TradeRequest validRequest() {
        return new TradeRequest(
                "TRD-20260315-9999",
                1L,
                1L,
                "EQUITY",
                "BUY",
                new BigDecimal("100.0000"),
                new BigDecimal("245.50"),
                LocalDate.now());
    }

    @Test
    @WithMockUser(roles = "TRADER")
    void testCreateTrade_authenticated_returns201() throws Exception {
        Instant now = Instant.now();
        when(tradeService.create(any(), any())).thenReturn(
                buildTrade(42L, "TRD-20260315-9999", "PENDING", now));

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest()))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                        .csrf()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/trades/42")))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.tradeRef").value("TRD-20260315-9999"));
    }

    @Test
    void testCreateTrade_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void testCreateTrade_viewerRole_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest()))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                        .csrf()))
                .andExpect(status().isForbidden());
    }

    private Trade buildTrade(Long id, String tradeRef, String status, Instant timestamp) {
        Instrument instrument = new Instrument();
        Counterparty counterparty = new Counterparty();

        Trade trade = new Trade();
        //trade(id);
        trade.setTradeRef(tradeRef);
        trade.setInstrument(instrument);
        trade.setCounterparty(counterparty);
        trade.setAssetClass("EQUITY");
        trade.setSide("BUY");
        trade.setQuantity(new BigDecimal("100.0000"));
        trade.setPrice(new BigDecimal("245.50"));
        trade.setTradeDate(LocalDate.now());
        trade.setStatus(status);
        // createdAt/modifiedAt likely set by @PrePersist/auditing, not directly settable — check your entity
        return trade;
    }
}
