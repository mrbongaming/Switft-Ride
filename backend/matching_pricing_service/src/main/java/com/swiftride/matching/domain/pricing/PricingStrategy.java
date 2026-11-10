package com.swiftride.matching.domain.pricing;

/** Domain policy implemented by standard, surge and promotional pricing. */
public interface PricingStrategy {
    java.math.BigDecimal fare(double distanceKm);
}

