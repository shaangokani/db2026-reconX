package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.ReconBreakResponse;
import com.dbtraining.reconx.dto.ReconResult;
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
import com.dbtraining.reconx.service.SimulatedCounterpartyFeed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final SimulatedCounterpartyFeed counterpartyFeed;

    public ReconController(ReconBreakRepository breaks, TradeRepository trades,
                           ReconciliationEngine engine, SimulatedCounterpartyFeed counterpartyFeed) {
        this.breaks = breaks;
        this.trades = trades;
        this.engine = engine;
        this.counterpartyFeed = counterpartyFeed;
    }

    @PostMapping("/run")
    @Operation(summary = "Trigger a reconciliation job")
    @Transactional
    public ResponseEntity<Map<String, Object>> runRecon(@Valid @RequestBody ReconRunRequest req) {
        String jobId = UUID.randomUUID().toString();

        List<Trade> rows = trades.findEquityTradesForReconciliation(
                req.from(), req.to(), req.counterpartyId());

        List<TradeType> internal = rows.stream()
                .<TradeType>map(ReconController::toEquityTradeOrNull)
                .filter(Objects::nonNull)
                .toList();

        // The counterparty side is simulated (see SimulatedCounterpartyFeed) —
        // there is no real external feed yet. The comparison below is the real
        // engine; only its input is stood in for.
        List<TradeType> external = counterpartyFeed.deriveFrom(internal);
        List<ReconResult> results = engine.reconcile(internal, external,
                ReconciliationRule.PRICE_TOLERANCE_1PCT);

        Map<String, Long> tradeIdByRef = rows.stream()
                .collect(Collectors.toMap(Trade::getTradeRef, Trade::getId, (a, b) -> a));

        // Don't stack duplicates: a trade that is already flagged stays as the
        // one open break until someone resolves it.
        Set<Long> alreadyOpen = breaks.findAll().stream()
                .filter(b -> "OPEN".equals(b.getStatus()))
                .map(ReconBreak::getTradeId)
                .collect(Collectors.toSet());

        List<ReconBreak> raised = new ArrayList<>();
        for (ReconResult r : results) {
            if (r.status() != ReconResult.Status.BREAK) continue;
            Long tradeId = tradeIdByRef.get(r.tradeRef());
            if (tradeId == null || !alreadyOpen.add(tradeId)) continue;
            raised.add(ReconBreak.detected(tradeId, r.discrepancyType(), r.details()));
        }
        breaks.saveAll(raised);

        long matched = results.stream().filter(r -> r.status() == ReconResult.Status.MATCHED).count();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "jobId", jobId,
                "status", "COMPLETED",
                "tradesCompared", results.size(),
                "matched", matched,
                "breaksRaised", raised.size()));
    }

    /**
     * Rows inserted outside the API (seed data, manual SQL) can carry a
     * tradeRef/side/quantity that never passed TradeRequest's validation, so
     * they can't always become a valid domain TradeType. Skip those rather
     * than fail the whole reconciliation run over one unreconcilable row.
     */
    private static EquityTrade toEquityTradeOrNull(Trade t) {
        try {
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
        } catch (IllegalArgumentException | IllegalStateException e) {
            return null;
        }
    }

    @GetMapping("/jobs/{jobId}/results")
    @Operation(summary = "Get results for a recon job")
    @Transactional(readOnly = true)
    public List<ReconBreakResponse> results(@PathVariable String jobId) {
        // NOTE: jobId is currently ignored — breaks are not yet scoped to a job,
        //   so every caller gets the whole open/resolved set. The UI relies on
        //   this and passes the literal "latest"; wiring findByJobId later has
        //   to update that call site too.
        List<ReconBreak> all = breaks.findAll();

        // Resolve tradeId -> tradeRef in one query; the entity only stores the
        // id, which is not something an operator can act on.
        Map<Long, String> refById = trades
                .findAllById(all.stream().map(ReconBreak::getTradeId).filter(Objects::nonNull).toList())
                .stream()
                .collect(Collectors.toMap(Trade::getId, Trade::getTradeRef, (a, b) -> a));

        return all.stream().map(b -> new ReconBreakResponse(
                b.getId(),
                b.getTradeId(),
                // a soft-deleted trade won't come back from findAllById
                refById.getOrDefault(b.getTradeId(), "trade #" + b.getTradeId()),
                b.getDiscrepancyType(),
                b.getStatus(),
                b.getDetails(),
                b.getDetectedAt(),
                b.getResolvedAt(),
                b.getResolutionNote()
        )).toList();
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
