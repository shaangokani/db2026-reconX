package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeRevision;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import com.dbtraining.reconx.service.AuditService;
import com.dbtraining.reconx.service.TradeAggregator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * TICKET-ADV071 — GET /api/v1/audit/trades/{tradeRef}
 * TICKET-ADV138 — GET /api/v1/audit/trades/{tradeRef}/events
 * TICKET-ADV137 — GET /api/v1/audit/trades/{tradeRef}/rebuild
 */
@RestController
@RequestMapping("/v1/audit")
@Tag(name = "audit")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditService auditService;
    private final AuditLogRepository auditRepo;
    private final TradeAggregator aggregator;

    public AuditController(AuditService auditService, AuditLogRepository auditRepo, TradeAggregator aggregator) {
        this.auditService = auditService;
        this.auditRepo = auditRepo;
        this.aggregator = aggregator;
    }

    @GetMapping("/trades/{tradeRef}")
    @Operation(summary = "Get audit history for a trade (by tradeRef)")
    public List<TradeRevision> history(@PathVariable String tradeRef) {
        return auditService.findRevisions(tradeRef);
    }

    @GetMapping("/trades/{tradeRef}/events")
    @Operation(summary = "Stream of all Kafka-sourced events for a trade")
    public List<AuditLogEntry> events(@PathVariable String tradeRef) {
        return auditRepo.findByTradeRefOrderByEventTimestampAsc(tradeRef);
    }

    @GetMapping("/trades/{tradeRef}/rebuild")
    @Operation(summary = "Rebuild a trade's current state by folding its Kafka event log")
    public Map<String, Object> rebuild(@PathVariable String tradeRef) {
        return aggregator.rebuild(tradeRef)
                .<Map<String, Object>>map(state -> Map.of("tradeRef", tradeRef, "state", state))
                .orElseGet(() -> Map.of("tradeRef", tradeRef, "state", "null (no events or last event was a cancellation)"));
    }
}
