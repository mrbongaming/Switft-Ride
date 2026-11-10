package com.swiftride.payment;

import static org.junit.jupiter.api.Assertions.*;
import com.swiftride.payment.application.PaymentApplicationService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:paytest;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "swiftride.messaging.consumers-enabled=false",
    "spring.rabbitmq.listener.simple.auto-startup=false",
    "logging.level.root=ERROR"
})
class PaymentPersistenceTest {
    @Autowired PaymentApplicationService payments;
    @Test void settlementIsIdempotentAndRefundReversesWallet() {
        UUID trip = UUID.randomUUID();
        var payment = payments.create(trip, "rider-1", "driver-1", BigDecimal.valueOf(100000), "VND", "WALLET", "key-1");
        assertEquals(payment.id(), payments.create(trip, "rider-1", "driver-1", BigDecimal.valueOf(100000), "VND", "WALLET", "key-1").id());
        assertEquals(0, payments.wallet("driver-1").balance().compareTo(BigDecimal.valueOf(80000)));
        payments.refund(payment.id(), "test", "refund-1");
        assertEquals(0, payments.wallet("driver-1").balance().compareTo(BigDecimal.ZERO));
        assertEquals(2, payments.wallet("driver-1").entries().size());
    }
}
