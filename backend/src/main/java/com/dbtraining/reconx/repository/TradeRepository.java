package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.repository.entity.Trade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * TICKET-ADV055 — Custom JPQL filter query
 * TICKET-ADV056 — Specification-based dynamic queries (JpaSpecificationExecutor)
 * TICKET-ADV057 — Pageable / Page<T> for paginated list endpoints
 * ============================================================================
 */
public interface TradeRepository
        extends JpaRepository<Trade, Long>, JpaSpecificationExecutor<Trade> {

    Optional<Trade> findByTradeRef(String tradeRef);

    /**
     * Counts per status in a single query, for the dashboard summary. Trade
     * carries @SQLRestriction("deleted_at IS NULL"), so soft-deleted rows are
     * excluded here automatically — no extra predicate needed.
     */
    @Query("""
        SELECT new com.dbtraining.reconx.dto.StatusCount(t.status, COUNT(t))
        FROM Trade t
        GROUP BY t.status
        """)
    List<com.dbtraining.reconx.dto.StatusCount> countGroupedByStatus();

    @Query("""
        SELECT t FROM Trade t
        JOIN FETCH t.instrument
        JOIN FETCH t.counterparty
        WHERE t.id = :id
        """)
    Optional<Trade> findByIdWithDetails(@Param("id") Long id);

    @Query("""
        SELECT t FROM Trade t
        WHERE t.tradeDate BETWEEN :from AND :to
          AND (:status IS NULL OR t.status = :status)
        """)
    Page<Trade> findByFilters(@Param("from") LocalDate from,
                              @Param("to") LocalDate to,
                              @Param("status") String status,
                              Pageable pageable);

    long countByStatus(String status);

    /**
     * TICKET-ADV084 — internal feed for ReconciliationEngine.reconcile(). Scoped to
     * EQUITY: Trade's generic schema (no isin/couponRate/ccy1-2/strike columns)
     * can only be losslessly converted to EquityTrade, not the other TradeType variants.
     */
    @Query("""
        SELECT t FROM Trade t
        JOIN FETCH t.instrument
        WHERE t.assetClass = 'EQUITY'
          AND t.tradeDate BETWEEN :from AND :to
          AND (:counterpartyId IS NULL OR t.counterparty.id = :counterpartyId)
        """)
    List<Trade> findEquityTradesForReconciliation(@Param("from") LocalDate from,
                                                  @Param("to") LocalDate to,
                                                  @Param("counterpartyId") Long counterpartyId);
}
