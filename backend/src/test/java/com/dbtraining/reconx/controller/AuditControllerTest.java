package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeRevision;
import com.dbtraining.reconx.dto.TradeSnapshot;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.security.JwtTokenProvider;
import com.dbtraining.reconx.service.AuditService;
import com.dbtraining.reconx.service.TradeAggregator;
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
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditController.class)
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditService auditService;

    @MockBean
    private AuditLogRepository auditLogRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private TradeAggregator tradeAggregator;

    @Test
    @WithMockUser(roles = "TRADER")
    void historyReturnsRevisionsFromAuditService() throws Exception {
        TradeSnapshot snapshot = new TradeSnapshot(
                1L, "TRD-20260315-0001", 1L, 1L,
                "EQUITY", "BUY", BigDecimal.TEN, BigDecimal.valueOf(50.25),
                LocalDate.parse("2026-03-15"), "PENDING", false);
        TradeRevision revision = new TradeRevision(
                1, Instant.parse("2026-03-15T10:00:00Z"), "ADD", "admin@db.com", snapshot);

        when(auditService.findRevisions("TRD-20260315-0001"))
                .thenReturn(List.of(revision));

        mockMvc.perform(get("/v1/audit/trades/TRD-20260315-0001")
                        .with(user("admin@db.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].revisionType").value("ADD"))
                .andExpect(jsonPath("$[0].changedBy").value("admin@db.com"))
                .andExpect(jsonPath("$[0].snapshot.tradeRef").value("TRD-20260315-0001"));
    }
}
