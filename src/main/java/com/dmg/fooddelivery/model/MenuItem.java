package com.dmg.fooddelivery.model;

import com.dmg.fooddelivery.common.ApiException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;

@Entity @Table(name = "menu_items", indexes = @Index(name = "ix_menu_restaurant", columnList = "restaurant_id"))
@Check(constraints = "stock >= 0 AND price > 0")
@Getter @Setter
public class MenuItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(nullable = false, length = 1000)
    private String description = "";
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @Column(nullable = false)
    private long stock;
    @Column(nullable = false)
    private boolean available = true;

    public void adjustStock(long delta) {
        long updated = Math.addExact(stock, delta);
        if (updated < 0) throw ApiException.conflict("Insufficient stock for menu item " + getId());
        stock = updated;
    }
}
