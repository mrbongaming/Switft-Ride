package com.swiftride.matching.domain.pricing;

/** Supply-and-demand fare policy. */
public final class SurgePricing implements PricingStrategy {
    private final PricingStrategy base;
    private final java.math.BigDecimal multiplier;
    public SurgePricing(PricingStrategy base, java.math.BigDecimal multiplier) { this.base = base; this.multiplier = multiplier; }
    public java.math.BigDecimal fare(double distanceKm) { return base.fare(distanceKm).multiply(multiplier).setScale(0, java.math.RoundingMode.HALF_UP); }
}
