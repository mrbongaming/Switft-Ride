package com.swiftride.payment.application;

import java.util.UUID;

public interface PaymentEvents { void settled(UUID tripId, UUID paymentId); }
