package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * ============================================================================
 * TICKET-ADV038 — Custom Collector returning a ReconSummary
 *
 * WHAT:    Collects a Stream<ReconResult> into a ReconSummary carrying total,
 *          matched, and broken counts.
 * HOW:    Implements Collector<ReconResult, Builder, ReconSummary> with a
 *          mutable Builder accumulator. Combiner returns a fresh builder
 *          (parallel-safe). Characteristics: UNORDERED only.
 * WHY:    Count aggregates are commutative, so UNORDERED is correct.
 *          IDENTITY_FINISH is wrong (finisher is non-trivial).
 *          CONCURRENT is wrong (Builder is not thread-safe).
 * OBSERVE: Serial and parallel streams over the same input produce identical
 *          ReconSummary values.
 * ============================================================================
 */
public final class ReconSummaryCollector
        implements Collector<ReconResult, ReconSummary.Builder, ReconSummary> {

    @Override
    public Supplier<ReconSummary.Builder> supplier() {
        return ReconSummary.Builder::new;
    }

    @Override
    public BiConsumer<ReconSummary.Builder, ReconResult> accumulator() {
        return (b, r) -> {
            b.total++;
            if (r.status() == ReconResult.Status.MATCHED) {
                b.matched++;
            } else {
                b.broken++;
            }
        };
    }

    @Override
    public BinaryOperator<ReconSummary.Builder> combiner() {
        return (a, b) -> {
            ReconSummary.Builder out = new ReconSummary.Builder();
            out.total   = a.total   + b.total;
            out.matched = a.matched + b.matched;
            out.broken  = a.broken  + b.broken;
            return out;
        };
    }

    @Override
    public Function<ReconSummary.Builder, ReconSummary> finisher() {
        return b -> new ReconSummary(b.total, b.matched, b.broken);
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.UNORDERED);
    }
}
