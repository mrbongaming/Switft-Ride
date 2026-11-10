package com.swiftride.matching.domain.pricing;

/** Standard fare policy. Keep all calculations framework-free. */
public final class StandardPricing implements PricingStrategy {
    public java.math.BigDecimal fare(double distanceKm) {
        return java.math.BigDecimal.valueOf(12_000 + Math.ceil(distanceKm * 9_000 / 1_000) * 1_000);
    }
}
