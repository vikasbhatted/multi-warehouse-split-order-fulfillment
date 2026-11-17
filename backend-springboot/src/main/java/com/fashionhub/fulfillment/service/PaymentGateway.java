package com.fashionhub.fulfillment.service;

import com.fashionhub.fulfillment.common.PaymentGatewayException;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Mock payment provider. Fails on demand so the REFUND_FAILED error path can be demonstrated. */
@Component
public class PaymentGateway {
    private static final Logger log = LoggerFactory.getLogger(PaymentGateway.class);

    public String refund(String orderId, String sku, BigDecimal amount, boolean simulateFailure) {
        if (simulateFailure) {
            log.warn("Payment gateway refused refund for order {} / {} (simulated outage)", orderId, sku);
            throw new PaymentGatewayException("Payment provider returned HTTP 503 while refunding " + sku);
        }
        String ref = "RF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Refunded INR {} for order {} / {} -> {}", amount, orderId, sku, ref);
        return ref;
    }
}
