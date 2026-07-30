package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeMapper;
import com.dbtraining.reconx.dto.TradeResponse;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.security.JwtTokenProvider;
import com.dbtraining.reconx.security.SecurityConfig;
import com.dbtraining.reconx.service.TradeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TradeController.class)
@Import(SecurityConfig.class)
class TradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradeService service;

    @MockBean
    private TradeMapper mapper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(roles = "TRADER")
    void listReturnsStablePagedEnvelopeAndBindsPageable() throws Exception {
        Trade trade = new Trade();
        TradeResponse response = new TradeResponse(
                1L,
                "TRD-001",
                10L,
                "AAPL",
                20L,
                "Counterparty A",
                "EQUITY",
                "BUY",
                new java.math.BigDecimal("100"),
                new java.math.BigDecimal("10.25"),
                LocalDate.of(2026, 7, 28),
                "PENDING",
                null,
                null);

        when(service.list(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(trade),
                        PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "tradeDate")), 1));
        when(mapper.toResponse(trade)).thenReturn(response);

        mockMvc.perform(get("/v1/trades?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].tradeRef").value("TRD-001"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.pageable").doesNotExist());

        var pageableCaptor = forClass(Pageable.class);
        verify(service).list(isNull(), isNull(), isNull(), isNull(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "tradeDate"));
    }
}
