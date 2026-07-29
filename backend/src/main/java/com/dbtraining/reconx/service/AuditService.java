package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeRevision;
import com.dbtraining.reconx.dto.TradeSnapshot;
import com.dbtraining.reconx.repository.entity.CustomRevisionEntity;
import com.dbtraining.reconx.repository.entity.Trade;
import jakarta.persistence.EntityManager;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * TICKET-ADV071 — GET /api/v1/audit/trades/{tradeRef}
 *
 * Reads Hibernate Envers' trades_aud/revinfo tables directly, so history is
 * available the moment a trade is created/updated — no Kafka pipeline
 * required (that's TICKET-ADV129/ADV132, Day 9's event-sourcing exercise).
 */
@Service
public class AuditService {

    private final EntityManager em;

    public AuditService(EntityManager em) {
        this.em = em;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<TradeRevision> findRevisions(String tradeRef) {
        AuditReader reader = AuditReaderFactory.get(em);

        List<Object[]> rows = reader.createQuery()
                .forRevisionsOfEntity(Trade.class, false, true)
                .add(AuditEntity.property("tradeRef").eq(tradeRef))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();

        return rows.stream()
                .map(row -> {
                    Trade trade = (Trade) row[0];
                    CustomRevisionEntity rev = (CustomRevisionEntity) row[1];
                    RevisionType type = (RevisionType) row[2];
                    return new TradeRevision(
                            rev.getId(),
                            Instant.ofEpochMilli(rev.getTimestamp()),
                            type.name(),
                            rev.getUsername(),
                            TradeSnapshot.from(trade));
                })
                .toList();
    }
}
