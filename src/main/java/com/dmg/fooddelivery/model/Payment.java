package com.dmg.fooddelivery.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;

@Entity @Table(name = "payments")
@Check(constraints = "amount > 0")
@Getter @Setter
public class Payment extends BaseEntity {
    public enum Status { CAPTURED, REFUNDED }
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", unique = true)
    private CustomerOrder order;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Status status = Status.CAPTURED;
    @Column(nullable = false, unique = true, length = 50)
    private String reference = "pay_" + UUID.randomUUID();
}
