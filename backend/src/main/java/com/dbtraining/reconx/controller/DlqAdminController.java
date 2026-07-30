package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.repository.DlqMessageRepository;
import com.dbtraining.reconx.repository.entity.DlqMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TICKET-ADV136 — Operator escape hatch for messages parked on trade-events-dlq.
 * One-at-a-time replay by eventId — never a bulk "replay everything" endpoint,
 * or an unfixed bug just re-poisons the same messages.
 * ADMIN-only enforcement lives in SecurityConfig (path matcher), matching
 * this codebase's convention — @EnableMethodSecurity isn't on, so a
 * @PreAuthorize here would silently do nothing.
 */
@RestController
@RequestMapping("/v1/admin/dlq")
@Tag(name = "admin-dlq")
@SecurityRequirement(name = "bearerAuth")
public class DlqAdminController {

    private final DlqMessageRepository repo;
    private final TradeEventProducer producer;
    private final ObjectMapper objectMapper;

    public DlqAdminController(DlqMessageRepository repo, TradeEventProducer producer, ObjectMapper objectMapper) {
        this.repo = repo;
        this.producer = producer;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    @Operation(summary = "List all messages currently parked on a DLQ topic")
    public List<DlqMessage> list() {
        return repo.findAll();
    }

    @PostMapping("/replay")
    @Operation(summary = "Re-publish one DLQ message back to its original topic by eventId")
    public ResponseEntity<Map<String, Object>> replay(
            @RequestParam UUID eventId,
            @RequestParam(defaultValue = "false") boolean dryRun) throws Exception {

        DlqMessage msg = repo.findByEventId(eventId.toString())
                .orElseThrow(() -> new IllegalArgumentException("No DLQ message: " + eventId));

        if (dryRun) {
            return ResponseEntity.ok(Map.of(
                    "dryRun", true,
                    "wouldReplayTo", msg.getOriginalTopic(),
                    "tradeRef", msg.getTradeRef() == null ? "" : msg.getTradeRef()
            ));
        }

        TradeEvent event = objectMapper.readValue(msg.getPayload(), TradeEvent.class);
        producer.publish(event);
        repo.delete(msg);

        return ResponseEntity.ok(Map.of(
                "replayed", true,
                "eventId", eventId,
                "topic", msg.getOriginalTopic()
        ));
    }
}
