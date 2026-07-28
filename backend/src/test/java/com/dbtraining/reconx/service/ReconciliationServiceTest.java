package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.repository.ReconResultRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReconciliationServiceTest {

    @Test
    void testReconcile_savesResultWithMatchedStatus() {
        // given
        ReconResultRepository repo = mock(ReconResultRepository.class);
        ReconciliationService svc = new ReconciliationService(repo);

        List<ReconResult> results = List.of(
            ReconResult.matched("TRD-1"),
            ReconResult.breakResult("TRD-2", "PRICE_DIFF", "Price mismatch")
        );

        // when
        svc.saveResults(results);

        // then
        ArgumentCaptor<ReconResult> captor = ArgumentCaptor.forClass(ReconResult.class);
        verify(repo, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).tradeRef()).isEqualTo("TRD-1");
        assertThat(captor.getAllValues().get(0).status()).isEqualTo(ReconResult.Status.MATCHED);
    }
}