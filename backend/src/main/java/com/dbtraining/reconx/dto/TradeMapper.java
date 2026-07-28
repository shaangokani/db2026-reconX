package com.dbtraining.reconx.dto;

import com.dbtraining.reconx.repository.entity.Trade;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.dto.TradeRequest;

/**
 * ============================================================================
 * TICKET-ADV054 — MapStruct mapper: Trade entity <-> DTO
 *
 * WHAT:    Generates the entity↔DTO conversion at compile time.
 * HOW:     componentModel="spring" → MapStruct emits a @Component bean named
 *          tradeMapper that you can @Autowire.
 * WHY:     Hand-written mappers drift. MapStruct fails the build if a new
 *          field is added to one side and forgotten on the other.
 * ============================================================================
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TradeMapper {

    @Mapping(source = "instrument.id", target = "instrumentId")
    @Mapping(source = "instrument.symbol", target = "instrumentSymbol")
    @Mapping(source = "counterparty.id", target = "counterpartyId")
    @Mapping(source = "counterparty.name", target = "counterpartyName")
    TradeResponse toResponse(Trade trade);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "instrument", ignore = true)
    @Mapping(target = "counterparty", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    Trade toEntity(TradeRequest req);
}
