package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import com.dbtraining.reconx.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
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
    private AuditLogRepository auditLogRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void historyReturnsEntriesFromRepository() throws Exception {
        AuditLogEntry entry = new AuditLogEntry(
                "evt-1",
                "TRD-20260315-0001",
                "TRADE_CREATED",
                Instant.parse("2026-03-15T10:00:00Z"),
                "admin",
                "{}",
                "{}"
        );
        when(auditLogRepository.findByTradeRefOrderByEventTimestampAsc("TRD-20260315-0001"))
                .thenReturn(List.of(entry));

        mockMvc.perform(get("/v1/audit/trades/TRD-20260315-0001")
                        .with(user("admin@db.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].tradeRef").value("TRD-20260315-0001"))
                .andExpect(jsonPath("$[0].eventType").value("TRADE_CREATED"));
    }
}
