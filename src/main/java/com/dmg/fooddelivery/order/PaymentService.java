package com.dmg.fooddelivery.order;

import com.dmg.fooddelivery.common.ApiException;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Local payment simulator. Its ledger participates in the same database transaction as inventory. */
@Service
public class PaymentService {
    private final JdbcTemplate jdbc;
    public PaymentService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(propagation = Propagation.MANDATORY)
    public void capture(long orderId, BigDecimal amount, OrderModels.PaymentToken token) {
        if (token == OrderModels.PaymentToken.TEST_DECLINE) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_DECLINED", "The simulated payment was declined");
        }
        jdbc.update("INSERT INTO payments(order_id, amount, status, reference) VALUES (?, ?, 'CAPTURED', ?)",
                orderId, amount, "pay_" + UUID.randomUUID());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void refund(long orderId) {
        if (jdbc.update("UPDATE payments SET status = 'REFUNDED' WHERE order_id = ? AND status = 'CAPTURED'", orderId) != 1) {
            throw ApiException.conflict("Payment cannot be refunded");
        }
    }
}
