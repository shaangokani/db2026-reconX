package com.dbtraining.reconx.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ============================================================================
 * TICKET-ADV029 — JSR-380 validation on the request DTO
 *
 * WHAT:    TradeRequest DTO record with jakarta.validation.constraints
 *          annotations on every field. Validation only fires when @Valid is
 *          applied to the @RequestBody parameter in the controller.
 * WHY:     The DTO is the wire contract — putting validation on the JPA
 *          entity would couple persistence to wire format. The domain Builder
 *          is the second line of defence (Builder invariants).
 * OBSERVE: Validator.validate(...) on a malformed request returns the
 *          expected violations, one per constraint that fails.
 * ============================================================================
 */
class TradeRequestValidationTest {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void wellFormed_returnsNoViolations() {
        TradeRequest request = new TradeRequest(
                "EQT-20260727-0001",    // tradeRef
                1L,                      // instrumentId
                100L,                    // counterpartyId
                "EQUITY",                // assetClass
                "BUY",                   // side
                new BigDecimal("100"),   // quantity
                new BigDecimal("150.50"), // price
                LocalDate.of(2026, 7, 27)  // tradeDate
        );

        Set<ConstraintViolation<TradeRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void quantity_negative_violatesPositive() {
        TradeRequest request = new TradeRequest(
                "EQT-20260727-0001",
                1L,
                100L,
                "EQUITY",
                "BUY",
                new BigDecimal("-1"),     // negative quantity
                new BigDecimal("150.50"),
                LocalDate.of(2026, 7, 27)
        );

        Set<ConstraintViolation<TradeRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("quantity");
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .anyMatch(msg -> msg.contains("greater than 0"));
    }

    @Test
    void quantity_zero_violatesPositive() {
        TradeRequest request = new TradeRequest(
                "EQT-20260727-0001",
                1L,
                100L,
                "EQUITY",
                "BUY",
                BigDecimal.ZERO,         // zero quantity
                new BigDecimal("150.50"),
                LocalDate.of(2026, 7, 27)
        );

        Set<ConstraintViolation<TradeRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("quantity");
    }

    @Test
    void price_negative_violatesPositiveOrZero() {
        TradeRequest request = new TradeRequest(
                "EQT-20260727-0001",
                1L,
                100L,
                "EQUITY",
                "BUY",
                new BigDecimal("100"),
                new BigDecimal("-1"),    // negative price
                LocalDate.of(2026, 7, 27)
        );

        Set<ConstraintViolation<TradeRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("price");
    }

    @Test
    void price_zero_isValid() {
        TradeRequest request = new TradeRequest(
                "EQT-20260727-0001",
                1L,
                100L,
                "EQUITY",
                "BUY",
                new BigDecimal("100"),
                BigDecimal.ZERO,         // zero price is OK
                LocalDate.of(2026, 7, 27)
        );

        Set<ConstraintViolation<TradeRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void tradeRef_badFormat_violatesPattern() {
        TradeRequest request = new TradeRequest(
                "invalid-ref",           // bad format
                1L,
                100L,
                "EQUITY",
                "BUY",
                new BigDecimal("100"),
                new BigDecimal("150.50"),
                LocalDate.of(2026, 7, 27)
        );

        Set<ConstraintViolation<TradeRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("tradeRef");
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("tradeRef must match AAA-YYYYMMDD-NNNN");
    }

    @Test
    void side_invalidValue_violatesPattern() {
        TradeRequest request = new TradeRequest(
                "EQT-20260727-0001",
                1L,
                100L,
                "EQUITY",
                "INVALID",               // bad side
                new BigDecimal("100"),
                new BigDecimal("150.50"),
                LocalDate.of(2026, 7, 27)
        );

        Set<ConstraintViolation<TradeRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("side");
    }

    @Test
    void side_buy_isValid() {
        TradeRequest request = new TradeRequest(
                "EQT-20260727-0001",
                1L,
                100L,
                "EQUITY",
                "BUY",
                new BigDecimal("100"),
                new BigDecimal("150.50"),
                LocalDate.of(2026, 7, 27)
        );

        Set<ConstraintViolation<TradeRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void side_sell_isValid() {
        TradeRequest request = new TradeRequest(
                "EQT-20260727-0001",
                1L,
                100L,
                "EQUITY",
                "SELL",
                new BigDecimal("100"),
                new BigDecimal("150.50"),
                LocalDate.of(2026, 7, 27)
        );

        Set<ConstraintViolation<TradeRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void assetClass_blank_violatesNotBlank() {
        TradeRequest request = new TradeRequest(
                "EQT-20260727-0001",
                1L,
                100L,
                "   ",                   // blank assetClass
                "BUY",
                new BigDecimal("100"),
                new BigDecimal("150.50"),
                LocalDate.of(2026, 7, 27)
        );

        Set<ConstraintViolation<TradeRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("assetClass");
    }

    @Test
    void tradeRef_null_violatesNotNull() {
        TradeRequest request = new TradeRequest(
                null,                    // null tradeRef
                1L,
                100L,
                "EQUITY",
                "BUY",
                new BigDecimal("100"),
                new BigDecimal("150.50"),
                LocalDate.of(2026, 7, 27)
        );

        Set<ConstraintViolation<TradeRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("tradeRef");
    }

    @Test
    void multipleViolations_returnsAll() {
        TradeRequest request = new TradeRequest(
                "bad-ref",               // violates @Pattern
                1L,
                100L,
                "",                      // violates @NotBlank
                "INVALID",               // violates @Pattern
                new BigDecimal("-1"),    // violates @Positive
                new BigDecimal("-1"),    // violates @PositiveOrZero
                LocalDate.of(2026, 7, 27)
        );

        Set<ConstraintViolation<TradeRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(5);
    }
}
