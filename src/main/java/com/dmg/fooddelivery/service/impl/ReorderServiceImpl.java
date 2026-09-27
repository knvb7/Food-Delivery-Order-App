package com.dmg.fooddelivery.service.impl;

import com.dmg.fooddelivery.common.Access;
import com.dmg.fooddelivery.common.ApiException;
import com.dmg.fooddelivery.dto.OrderDtos.LineInput;
import com.dmg.fooddelivery.dto.ReorderDtos.BlockReason;
import com.dmg.fooddelivery.dto.ReorderDtos.ItemAvailability;
import com.dmg.fooddelivery.dto.ReorderDtos.ReorderBasket;
import com.dmg.fooddelivery.dto.ReorderDtos.ReorderLine;
import com.dmg.fooddelivery.model.CustomerOrder;
import com.dmg.fooddelivery.model.MenuItem;
import com.dmg.fooddelivery.model.OrderItem;
import com.dmg.fooddelivery.model.Restaurant;
import com.dmg.fooddelivery.model.Role;
import com.dmg.fooddelivery.model.User;
import com.dmg.fooddelivery.repository.MenuItemRepository;
import com.dmg.fooddelivery.repository.OrderRepository;
import com.dmg.fooddelivery.service.ReorderService;
import com.dmg.fooddelivery.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReorderServiceImpl implements ReorderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private Clock restaurantClock;

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ReorderBasket buildBasket(long actorId, long orderId) {
        User customer = userService.get(actorId);
        Access.requireRole(customer, Role.CUSTOMER);
        CustomerOrder order =
                orderRepository.findById(orderId).orElseThrow(() -> ApiException.notFound("Order"));
        Access.requireOrderAccess(customer, order);
        Restaurant restaurant = order.getRestaurant();

        Map<Long, MenuItem> currentItems = new HashMap<>();
        for (MenuItem item : menuItemRepository.findForReorder(orderId, restaurant.getId())) {
            currentItems.put(item.getId(), item);
        }

        List<ReorderLine> lines = new ArrayList<>();
        List<LineInput> items = new ArrayList<>();
        BigDecimal total = new BigDecimal("0.00");
        for (OrderItem previousLine : order.getItems()) {
            long menuItemId = previousLine.getMenuItem().getId();
            ReorderLine line = rebuildLine(previousLine, currentItems.get(menuItemId));
            lines.add(line);
            if (line.quantity() > 0) {
                items.add(new LineInput(menuItemId, line.quantity()));
                BigDecimal lineTotal =
                        line.unitPrice().multiply(BigDecimal.valueOf(line.quantity()));
                total = total.add(lineTotal);
            }
        }

        BlockReason blockReason = blockReason(restaurant, items.isEmpty());
        return new ReorderBasket(
                orderId,
                restaurant.getId(),
                order.getDeliveryAddress(),
                "INR",
                total,
                lines,
                items,
                blockReason == null,
                blockReason);
    }

    private ReorderLine rebuildLine(OrderItem previousLine, MenuItem currentItem) {
        long menuItemId = previousLine.getMenuItem().getId();
        if (currentItem == null) {
            return new ReorderLine(
                    menuItemId,
                    previousLine.getName(),
                    previousLine.getUnitPrice(),
                    null,
                    previousLine.getQuantity(),
                    0,
                    false,
                    ItemAvailability.NO_LONGER_OFFERED);
        }

        int quantity = 0;
        ItemAvailability availability;
        if (!currentItem.isAvailable()) {
            availability = ItemAvailability.UNAVAILABLE;
        } else if (currentItem.getStock() == 0) {
            availability = ItemAvailability.OUT_OF_STOCK;
        } else {
            quantity = (int) Math.min(previousLine.getQuantity(), currentItem.getStock());
            availability =
                    quantity < previousLine.getQuantity()
                            ? ItemAvailability.QUANTITY_REDUCED
                            : ItemAvailability.AVAILABLE;
        }

        boolean priceChanged = currentItem.getPrice().compareTo(previousLine.getUnitPrice()) != 0;
        return new ReorderLine(
                menuItemId,
                currentItem.getName(),
                previousLine.getUnitPrice(),
                currentItem.getPrice(),
                previousLine.getQuantity(),
                quantity,
                priceChanged,
                availability);
    }

    private BlockReason blockReason(Restaurant restaurant, boolean emptyBasket) {
        if (!restaurant.isActive()) {
            return BlockReason.RESTAURANT_INACTIVE;
        }

        if (!restaurant.getCity().isActive()) {
            return BlockReason.CITY_INACTIVE;
        }

        if (!restaurant.isOpenAt(LocalTime.now(restaurantClock))) {
            return BlockReason.RESTAURANT_CLOSED;
        }

        if (emptyBasket) {
            return BlockReason.NO_AVAILABLE_ITEMS;
        }

        return null;
    }
}
