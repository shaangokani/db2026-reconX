package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.repository.entity.Trade;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Testcontainers
public class ReconciliationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("reconx")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    /*@Autowired
    private InternalTradeRepository internalTradeRepo;

    @Autowired
    private ExternalTradeRepository externalTradeRepo;

    @Autowired
    private ReconResultRepository reconResultRepo;

    @Autowired
    private ReconciliationService reconciliationService;*/

    @Test
    public void containerIsRunning() {
        assertThat(postgres.isRunning()).isTrue();
    }

    /*@Test
    @Transactional
    public void insertTradesAreReconciledAndPersisted() {
        // given — two matching trades, one in each repo
        Trade internal = new Trade("TRD-INT-1", "CP-1", "SAP.DE",
                new BigDecimal("100"), new BigDecimal("245.50"), LocalDate.now());
        Trade external = new Trade("TRD-INT-1", "CP-1", "SAP.DE",
                new BigDecimal("100"), new BigDecimal("245.50"), LocalDate.now());

        internalTradeRepo.save(internal);
        externalTradeRepo.save(external);

        // when
        reconciliationService.runRecon(
                internalTradeRepo.findAll(),
                externalTradeRepo.findAll());

        // then — exactly one MATCHED row landed in recon_results
        List<ReconResult> persisted = reconResultRepo.findAll();
        assertThat(persisted).hasSize(1);
        assertThat(persisted.get(0).status()).isEqualTo(ReconResult.Status.MATCHED);
        assertThat(persisted.get(0).tradeRef()).isEqualTo("TRD-INT-1");
    }*/
}
