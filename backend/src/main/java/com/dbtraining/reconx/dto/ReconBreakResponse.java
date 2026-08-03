package com.dbtraining.reconx.dto;

import java.time.Instant;

/**
 * Recon break as the UI needs it. The entity only stores `tradeId`, which is
 * meaningless to an operator, so the trade reference is resolved here and the
 * engine's explanation is carried through as `details`.
 */
public record ReconBreakResponse(
        Long id,
        Long tradeId,
        String tradeRef,
        String discrepancyType,
        String status,
        String details,
        Instant detectedAt,
        Instant resolvedAt,
        String resolutionNote
) {}
