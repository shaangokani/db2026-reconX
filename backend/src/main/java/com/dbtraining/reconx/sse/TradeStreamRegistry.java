package com.dbtraining.reconx.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Backing store for the live trade feed (day7 TICKET-ADV104 / day8
 * TICKET-ADV116 frontend both assume a backend SSE producer at
 * GET /v1/trades/stream — this is that producer's emitter registry).
 *
 * WHAT:    Holds every currently-connected SseEmitter and broadcasts each
 *          trade event to all of them.
 * WHY:     One Kafka-consumed event fans out to N open browser tabs; each
 *          tab's HTTP request lives as long as its emitter is registered
 *          here, independent of the Kafka listener thread.
 */
@Component
public class TradeStreamRegistry {

    private static final Logger log = LoggerFactory.getLogger(TradeStreamRegistry.class);
    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L; // 30 minutes

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));

        return emitter;
    }

    public void broadcast(Object payload) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data(payload));
            } catch (IOException | IllegalStateException e) {
                // Client disconnected without a clean close (e.g. tab closed) —
                // drop it rather than let send() keep failing on every event.
                log.debug("Dropping dead SSE emitter: {}", e.toString());
                emitters.remove(emitter);
            }
        }
    }
}
