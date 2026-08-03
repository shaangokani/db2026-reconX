package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.PagedResponse;
import com.dbtraining.reconx.dto.StatusCount;
import com.dbtraining.reconx.dto.TradeMapper;
import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.dto.TradeResponse;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * TICKET-ADV063-ADV067 — TradeController (full CRUD + filterable list)
 * TICKET-ADV080 — API versioning: every endpoint under /v1/
 *
 * Combined with the /api context-path from application.yml, full URLs are
 * /api/v1/trades, /api/v1/trades/{id} etc.
 * ============================================================================
 */
@RestController
@RequestMapping("/v1/trades")
@Tag(name = "trades", description = "Trade CRUD and search")
@SecurityRequirement(name = "bearerAuth")
public class TradeController {

    private final TradeService service;
    private final TradeMapper mapper;
    private final TradeRepository trades;

    public TradeController(TradeService service, TradeMapper mapper, TradeRepository trades) {
        this.service = service;
        this.mapper = mapper;
        this.trades = trades;
    }

    @Deprecated(since = "v1.4.0", forRemoval = true)
    @GetMapping(value = "/old-search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> oldSearch(HttpServletResponse response) {
        response.setHeader("Deprecation", "true");
        response.setHeader("Sunset", "Sat, 1 Jul 2026 00:00:00 GMT");
        response.setHeader("Link",
                "</api/v1/trades?status=...>; rel=\"successor-version\"");
        return ResponseEntity.status(HttpStatus.GONE).build();
    }

    @GetMapping
    @Operation(summary = "List trades — paginated, filterable, sortable")
    public PagedResponse<TradeResponse> list(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long counterpartyId,
            @PageableDefault(size = 20, sort = "tradeDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Trade> page = service.list(from, to, status, counterpartyId, pageable);
        return PagedResponse.from(page, mapper::toResponse);
    }

    /**
     * Persistent book totals for the dashboard. The dashboard's other figures
     * come from the SSE feed, which only ever reflects the current browser
     * session — this is the whole book, so the two can be shown side by side
     * without one being mistaken for the other.
     */
    @GetMapping("/summary")
    @Operation(summary = "Trade counts by status across the whole book")
    public Map<String, Object> summary() {
        List<StatusCount> rows = trades.countGroupedByStatus();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        // Seed the known statuses so a status with no trades still renders as 0
        // rather than vanishing from the breakdown entirely.
        for (String s : List.of("PENDING", "MATCHED", "UNMATCHED", "DISPUTED", "CANCELLED")) {
            byStatus.put(s, 0L);
        }
        for (StatusCount row : rows) {
            byStatus.merge(row.status(), row.count(), Long::sum);
        }
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        return Map.of("total", total, "byStatus", byStatus);
    }

    @PostMapping
    @Operation(summary = "Create a trade")
    public ResponseEntity<TradeResponse> create(@Valid @RequestBody TradeRequest req,
                                                @AuthenticationPrincipal Object principal) {
        // (TICKET-ADV064): call service.create(req, actor), build a Location
        //   header at /api/v1/trades/{id}, and return 201 Created with the
        //   mapped TradeResponse body.
        String actor = String.valueOf(principal);
        Trade saved = service.create(req, actor);
        return ResponseEntity
                .created(URI.create("/api/v1/trades/" + saved.getId()))
                .body(mapper.toResponse(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Full update of a trade")
    public TradeResponse update(@PathVariable Long id, @Valid @RequestBody TradeRequest req,
                                @AuthenticationPrincipal Object principal) {
        // (TICKET-ADV065): delegate to service.update(id, req, actor) and
        //   map the updated entity through mapper.toResponse.
        return mapper.toResponse(service.update(id, req, String.valueOf(principal)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update only the status field")
    public TradeResponse updateStatus(@PathVariable Long id,
                                      @RequestBody Map<String, String> body,
                                      @AuthenticationPrincipal Object principal) {
        // (TICKET-ADV066): read body.get("status") and call
        //   service.updateStatus(id, status, actor). Return mapper.toResponse(saved).
        String status = body.get("status");
        return mapper.toResponse(service.updateStatus(id, status, String.valueOf(principal)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete (sets deleted_at)")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal Object principal) {
        // (TICKET-ADV067): service.softDelete(id, actor); return 204 No Content.
        service.softDelete(id, String.valueOf(principal));
        return ResponseEntity.noContent().build();
    }
}
