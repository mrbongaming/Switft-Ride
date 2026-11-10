package com.swiftride.matching.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class PriceQuote {

    private final UUID quoteId;
    private final UUID tripId;
    private final BigDecimal amount;
    private final String currency;
    private final RouteMetrics route;
    private final BigDecimal surgeMultiplier;
    private final Instant createdAt;
    private final Instant expiresAt;

    public PriceQuote(
            UUID quoteId,
            UUID tripId,
            BigDecimal amount,
            String currency,
            RouteMetrics route,
            BigDecimal surgeMultiplier,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.quoteId = Objects.requireNonNull(
                quoteId,
                "Quote ID must not be null"
        );
        this.tripId = Objects.requireNonNull(
                tripId,
                "Trip ID must not be null"
        );
        this.amount = validateAmount(amount);
        this.currency = validateCurrency(currency);
        this.route = Objects.requireNonNull(
                route,
                "Route must not be null"
        );
        this.surgeMultiplier = validateSurgeMultiplier(surgeMultiplier);
        this.createdAt = Objects.requireNonNull(
                createdAt,
                "Created time must not be null"
        );
        this.expiresAt = validateExpiresAt(expiresAt, createdAt);
    }

    private static BigDecimal validateAmount(BigDecimal amount) {
        Objects.requireNonNull(amount, "Amount must not be null");

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Amount cannot be negative"
            );
        }

        return amount;
    }

    private static String validateCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException(
                    "Currency must not be blank"
            );
        }

        String normalizedCurrency = currency
                .trim()
                .toUpperCase(Locale.ROOT);

        if (normalizedCurrency.length() != 3) {
            throw new IllegalArgumentException(
                    "Currency must contain exactly 3 characters"
            );
        }

        return normalizedCurrency;
    }

    private static BigDecimal validateSurgeMultiplier(
            BigDecimal surgeMultiplier
    ) {
        Objects.requireNonNull(
                surgeMultiplier,
                "Surge multiplier must not be null"
        );

        if (surgeMultiplier.compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException(
                    "Surge multiplier cannot be less than 1"
            );
        }

        return surgeMultiplier;
    }

    private static Instant validateExpiresAt(
            Instant expiresAt,
            Instant createdAt
    ) {
        Objects.requireNonNull(
                expiresAt,
                "Expiration time must not be null"
        );

        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException(
                    "Expiration time must be after creation time"
            );
        }

        return expiresAt;
    }

    public boolean isExpired(Instant currentTime) {
        Objects.requireNonNull(
                currentTime,
                "Current time must not be null"
        );

        return !currentTime.isBefore(expiresAt);
    }

    public UUID quoteId() {
        return quoteId;
    }

    public UUID tripId() {
        return tripId;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    public RouteMetrics route() {
        return route;
    }

    public BigDecimal surgeMultiplier() {
        return surgeMultiplier;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }
}