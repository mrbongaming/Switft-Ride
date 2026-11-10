package com.swiftride.matching.domain.pricing;

/** Promotional fare policy. */
public final class PromoPricing implements PricingStrategy {
    private final PricingStrategy base;
    public PromoPricing(PricingStrategy base) { this.base = base; }
    public java.math.BigDecimal fare(double distanceKm) {
        return base.fare(distanceKm).multiply(java.math.BigDecimal.valueOf(0.9)).setScale(0, java.math.RoundingMode.HALF_UP);
    }
}
