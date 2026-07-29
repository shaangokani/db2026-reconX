package com.dbtraining.reconx.dto;

import java.time.Instant;

/** TICKET-ADV071 — one Envers revision of a trade, oldest-first. */
public record TradeRevision(
        Number revisionId,
        Instant revisionTimestamp,
        String revisionType,
        String changedBy,
        TradeSnapshot snapshot
) {}
