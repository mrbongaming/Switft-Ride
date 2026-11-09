package com.swiftride.contracts.events;

/** Shared broker names. This module is a library, not a fourth microservice. */
public final class MessagingTopology {
    public static final String EVENTS_EXCHANGE = "swiftride.events";
    public static final String DEAD_LETTER_EXCHANGE = "swiftride.dead";
    public static final String TRIP_ACCEPTED_KEY = "trip.accepted.v1";
    public static final String RIDE_COMPLETED_KEY = "ride.completed.v1";
    public static final String PAYMENT_SETTLED_KEY = "payment.settled.v1";
    public static final String PAYMENT_FAILED_KEY = "payment.failed.v1";
    public static final String RIDE_COMPLETED_QUEUE = "swiftride.payment.ride-completed";
    public static final String TRIP_ACCEPTED_QUEUE = "swiftride.matching.trip-accepted";
    public static final String PAYMENT_SETTLED_QUEUE = "swiftride.trip.payment-settled";
    public static final String PAYMENT_FAILED_QUEUE = "swiftride.trip.payment-failed";

    private MessagingTopology() { }
}
