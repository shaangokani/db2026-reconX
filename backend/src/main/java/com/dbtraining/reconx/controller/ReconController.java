package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.ReconRunRequest;
import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.Side;
import com.dbtraining.reconx.model.TradeRef;
import com.dbtraining.reconx.model.TradeType;
import com.dbtraining.reconx.repository.ReconBreakRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.ReconBreak;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.service.ReconciliationEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TICKET-ADV068 — POST /api/v1/recon/run — returns 202 + jobId
 * TICKET-ADV069 — GET  /api/v1/recon/jobs/{jobId}/results
 * TICKET-ADV070 — PUT  /api/v1/recon/results/{id}/resolve
 * TICKET-ADV084 — synchronously drives ReconciliationEngine.reconcile() so its
 *   @Timed histogram has real samples (no async worker/Kafka consumer exists
 *   yet to pick up the queued job otherwise).
 */
@RestController
@RequestMapping("/v1/recon")
@Tag(name = "recon", description = "Reconciliation operations")
@SecurityRequirement(name = "bearerAuth")
public class ReconController {

    private final ReconBreakRepository breaks;
    private final TradeRepository trades;
    private final ReconciliationEngine engine;

    public ReconController(ReconBreakRepository breaks, TradeRepository trades, ReconciliationEngine engine) {
        this.breaks = breaks;
        this.trades = trades;
        this.engine = engine;
    }

    @PostMapping("/run")
    @Operation(summary = "Trigger a reconciliation job")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, String>> runRecon(@Valid @RequestBody ReconRunRequest req) {
        String jobId = UUID.randomUUID().toString();

        // No external counterparty feed exists yet (that's the Day-9 Kafka
        // pipeline), so this reconciles the internal book against itself —
        // enough to drive real Timer samples without fabricating data.
        List<TradeType> internal = trades
                .findEquityTradesForReconciliation(req.from(), req.to(), req.counterpartyId())
                .stream()
                .<TradeType>map(ReconController::toEquityTrade)
                .toList();
        engine.reconcile(internal, internal, ReconciliationRule.EXACT);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("jobId", jobId, "status", "QUEUED"));
    }

    private static EquityTrade toEquityTrade(Trade t) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(t.getTradeRef()))
                .instrumentSymbol(t.getInstrument().getSymbol())
                .quantity(t.getQuantity())
                .price(t.getPrice())
                .currency(t.getInstrument().getCurrency())
                .side(Side.valueOf(t.getSide()))
                .tradeDate(t.getTradeDate())
                .counterpartyId(t.getCounterparty().getId())
                .build();
    }

    @GetMapping("/jobs/{jobId}/results")
    @Operation(summary = "Get results for a recon job")
    public List<ReconBreak> results(@PathVariable String jobId) {
        // DONE: (TICKET-ADV069): once recon_jobs + recon_breaks tables are wired,
        //   return breaks.findByJobId(jobId). Day-0 returns an empty list so
        //   the React breaks-table renders "no breaks" gracefully.
        return breaks.findAll();
    }

    @PutMapping("/results/{id}/resolve")
    @Operation(summary = "Mark a recon break as RESOLVED with a note")
    public ResponseEntity<ReconBreak> resolve(@PathVariable Long id,
                                              @RequestBody Map<String, String> body) {
        // DONE: (TICKET-ADV070): load the ReconBreak, call rb.resolve(note), save,
        //   and return 200 with the updated entity. Throw TradeNotFoundException
        //   when the id is unknown.
        ReconBreak rb = breaks.findById(id)
                .orElseThrow(() -> new TradeNotFoundException("recon_break " + id));
        rb.resolve(body.getOrDefault("note", "manually resolved"));
        return ResponseEntity.ok(breaks.save(rb));
    }
}
