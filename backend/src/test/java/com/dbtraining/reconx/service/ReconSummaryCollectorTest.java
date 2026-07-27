package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-ADV038 — ReconSummaryCollector: serial vs parallel over 10k results.
 */
class ReconSummaryCollectorTest {

    @Test
    @DisplayName("10k-result parallel stream produces identical ReconSummary to serial stream")
    void testCollector_serialAndParallel_produceIdenticalSummary() {
        // given — 10k results: 6k matched, 4k broken
        List<ReconResult> results = IntStream.range(0, 10_000)
                .mapToObj(i -> i < 6_000
                        ? ReconResult.matched("EQU-20260603-" + String.format("%04d", i))
                        : ReconResult.breakResult(
                                "EQU-20260603-" + String.format("%04d", i),
                                "VALUE_MISMATCH", "test mismatch"))
                .toList();

        // when
        ReconSummary serial   = results.stream().collect(new ReconSummaryCollector());
        ReconSummary parallel = results.parallelStream().collect(new ReconSummaryCollector());

        // then — both must be identical
        assertThat(serial.total()).isEqualTo(10_000);
        assertThat(serial.matched()).isEqualTo(6_000);
        assertThat(serial.broken()).isEqualTo(4_000);

        assertThat(parallel.total()).isEqualTo(serial.total());
        assertThat(parallel.matched()).isEqualTo(serial.matched());
        assertThat(parallel.broken()).isEqualTo(serial.broken());
    }

    @Test
    @DisplayName("empty stream returns ReconSummary with all zeros")
    void testCollector_emptyStream_returnsZeros() {
        // when
        ReconSummary summary = List.<ReconResult>of().stream()
                .collect(new ReconSummaryCollector());

        // then
        assertThat(summary.total()).isEqualTo(0);
        assertThat(summary.matched()).isEqualTo(0);
        assertThat(summary.broken()).isEqualTo(0);
    }

    @Test
    @DisplayName("ReconSummary.empty() factory returns all zeros")
    void testEmpty_returnsZeros() {
        ReconSummary empty = ReconSummary.empty();

        assertThat(empty.total()).isEqualTo(0);
        assertThat(empty.matched()).isEqualTo(0);
        assertThat(empty.broken()).isEqualTo(0);
    }
}
