package com.dmg.fooddelivery.config;

import com.dmg.fooddelivery.dto.CatalogDtos.CityInput;
import com.dmg.fooddelivery.dto.CatalogDtos.CityResponse;
import com.dmg.fooddelivery.dto.CatalogDtos.MenuInput;
import com.dmg.fooddelivery.dto.CatalogDtos.RestaurantInput;
import com.dmg.fooddelivery.dto.CatalogDtos.RestaurantResponse;
import com.dmg.fooddelivery.dto.DeliveryDtos.PartnerInput;
import com.dmg.fooddelivery.dto.UserDtos.CreateUser;
import com.dmg.fooddelivery.dto.UserDtos.UserResponse;
import com.dmg.fooddelivery.model.Role;
import com.dmg.fooddelivery.model.User;
import com.dmg.fooddelivery.repository.UserRepository;
import com.dmg.fooddelivery.service.CatalogService;
import com.dmg.fooddelivery.service.DeliveryService;
import com.dmg.fooddelivery.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoData implements ApplicationRunner {

    private final UserRepository userRepository;
    private final UserService users;
    private final CatalogService catalog;
    private final DeliveryService delivery;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() != 0) {
            return;
        }

        User admin = new User();
        admin.setUsername("admin");
        admin.setRole(Role.ADMIN);
        long adminId = userRepository.save(admin).getId();

        UserResponse owner = users.create(adminId, new CreateUser("owner", Role.OWNER));
        UserResponse owner2 = users.create(adminId, new CreateUser("owner2", Role.OWNER));
        users.create(adminId, new CreateUser("customer", Role.CUSTOMER));
        users.create(adminId, new CreateUser("customer2", Role.CUSTOMER));
        UserResponse partner = users.create(adminId, new CreateUser("partner", Role.PARTNER));
        UserResponse partner2 = users.create(adminId, new CreateUser("partner2", Role.PARTNER));

        CityResponse mumbai = catalog.createCity(adminId, new CityInput("Mumbai", true));
        CityResponse pune = catalog.createCity(adminId, new CityInput("Pune", true));

        RestaurantResponse kitchen =
                catalog.createRestaurant(
                        adminId,
                        new RestaurantInput(
                                mumbai.id(),
                                owner.id(),
                                "Spice Kitchen",
                                "10 Market Road, Mumbai",
                                true));
        RestaurantResponse cafe =
                catalog.createRestaurant(
                        adminId,
                        new RestaurantInput(
                                pune.id(), owner2.id(), "Garden Cafe", "20 Park Road, Pune", true));

        catalog.createMenu(
                owner.id(),
                kitchen.id(),
                new MenuInput(
                        "Paneer Bowl",
                        "Paneer, rice and vegetables",
                        new BigDecimal("180.00"),
                        20L,
                        true));

        catalog.createMenu(
                owner.id(),
                kitchen.id(),
                new MenuInput(
                        "Rice Bowl",
                        "Seasonal vegetables and rice",
                        new BigDecimal("120.00"),
                        10L,
                        true));

        catalog.createMenu(
                owner2.id(),
                cafe.id(),
                new MenuInput(
                        "Pasta", "Tomato and basil pasta", new BigDecimal("220.00"), 15L, true));

        delivery.create(adminId, new PartnerInput(partner.id(), mumbai.id(), true));
        delivery.create(adminId, new PartnerInput(partner2.id(), mumbai.id(), true));
    }
}
