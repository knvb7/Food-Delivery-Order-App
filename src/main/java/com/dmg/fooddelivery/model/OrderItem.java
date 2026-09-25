package com.dmg.fooddelivery.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;

@Entity @Table(name = "order_items", uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "menu_item_id"}))
@Check(constraints = "quantity BETWEEN 1 AND 100 AND unit_price > 0")
@Getter @Setter
public class OrderItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id")
    private CustomerOrder order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;
    @Column(nullable = false)
    private int quantity;
}
