package com.dbtraining.reconx.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/v1/trades")
@Tag(name = "trade-stream", description = "Server-Sent Events for live trades")
public class TradeStreamController {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to live trade updates via SSE")
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(600000L); // 10 minutes timeout
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        // Send an initial connection event to keep it alive immediately
        try {
            emitter.send(SseEmitter.event().name("connected").data("SSE Connection Established"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }
}
