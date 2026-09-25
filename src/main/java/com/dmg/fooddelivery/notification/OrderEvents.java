package com.dmg.fooddelivery.notification;

import com.dmg.fooddelivery.catalog.CatalogRepository;
import com.dmg.fooddelivery.common.Database;
import com.dmg.fooddelivery.delivery.DeliveryRepository;
import com.dmg.fooddelivery.order.OrderModels.OrderRow;
import java.util.LinkedHashSet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderEvents {
    private final Database database;
    private final JdbcTemplate jdbc;
    private final CatalogRepository catalog;
    private final DeliveryRepository partners;
    public OrderEvents(Database database, JdbcTemplate jdbc, CatalogRepository catalog, DeliveryRepository partners) {
        this.database = database; this.jdbc = jdbc; this.catalog = catalog; this.partners = partners;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(OrderRow order, String type, long actorId) {
        long eventId = database.insert("INSERT INTO order_events(order_id, type, status, actor_id) VALUES (?, ?, ?, ?)",
                order.id(), type, order.status().name(), actorId);
        var recipients = new LinkedHashSet<Long>();
        recipients.add(order.customerId());
        recipients.add(catalog.restaurant(order.restaurantId()).ownerId());
        if (order.partnerId() != null) recipients.add(partners.get(order.partnerId(), false).userId());
        for (long recipientId : recipients) {
            jdbc.update("INSERT INTO notification_outbox(event_id, recipient_id) VALUES (?, ?)", eventId, recipientId);
        }
    }
}
