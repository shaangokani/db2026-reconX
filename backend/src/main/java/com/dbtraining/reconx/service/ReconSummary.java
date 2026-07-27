package com.dbtraining.reconx.service;

/**
 * ============================================================================
 * TICKET-ADV038 — ReconSummary domain object
 *
 * WHAT:    Immutable summary of a reconciliation run carrying total, matched,
 *          and broken counts.
 * HOW:     Java record — three long fields, a static empty() factory, and a
 *          mutable Builder used by ReconSummaryCollector.
 * WHY:     The collector (ADV038) needs a mutable accumulator during the
 *          stream, but the final value must be immutable. Builder bridges
 *          the two shapes.
 * ============================================================================
 */
public record ReconSummary(long total, long matched, long broken) {

    public static ReconSummary empty() { return new ReconSummary(0, 0, 0); }

    /** Mutable accumulator used by {@link ReconSummaryCollector}. */
    public static final class Builder {
        long total;
        long matched;
        long broken;
    }
}
