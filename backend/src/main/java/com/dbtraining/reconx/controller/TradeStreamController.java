package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.sse.TradeStreamRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * Emitter lifecycle (registration, cleanup, broadcast) lives in
 * TradeStreamRegistry, which TradeStreamConsumer also uses to fan out real
 * TradeEvents off Kafka's trade-events topic — this controller is just the
 * subscribe endpoint plus the original handshake message.
 */
@RestController
@RequestMapping("/v1/trades")
@Tag(name = "trade-stream", description = "Server-Sent Events for live trades")
public class TradeStreamController {

    private final TradeStreamRegistry registry;

    public TradeStreamController(TradeStreamRegistry registry) {
        this.registry = registry;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to live trade updates via SSE")
    public SseEmitter stream() {
        SseEmitter emitter = registry.subscribe();

        // Initial handshake so the connection has *something* to send right
        // away, independent of when the next real trade event arrives.
        try {
            emitter.send(SseEmitter.event().name("connected").data("SSE Connection Established"));
        } catch (IOException e) {
            // registry's onError/onCompletion callbacks handle de-registration
        }

        return emitter;
    }
}
