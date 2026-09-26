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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoData implements ApplicationRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private DeliveryService deliveryService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() != 0) {
            return;
        }

        User admin = new User();
        admin.setUsername("admin");
        admin.setFullName("System Administrator");
        admin.setEmail("admin@fooddelivery.local");
        admin.setPhoneNumber("+919000000001");
        admin.setRole(Role.ADMIN);
        long adminId = userRepository.save(admin).getId();

        UserResponse owner =
                userService.create(
                        adminId,
                        new CreateUser(
                                "owner",
                                "Spice Kitchen Owner",
                                "owner@fooddelivery.local",
                                "+919000000002",
                                Role.OWNER));
        UserResponse owner2 =
                userService.create(
                        adminId,
                        new CreateUser(
                                "owner2",
                                "Garden Cafe Owner",
                                "owner2@fooddelivery.local",
                                "+919000000003",
                                Role.OWNER));
        userService.create(
                adminId,
                new CreateUser(
                        "customer",
                        "Demo Customer",
                        "customer@fooddelivery.local",
                        "+919000000004",
                        Role.CUSTOMER));
        userService.create(
                adminId,
                new CreateUser(
                        "customer2",
                        "Second Customer",
                        "customer2@fooddelivery.local",
                        "+919000000005",
                        Role.CUSTOMER));
        UserResponse partner =
                userService.create(
                        adminId,
                        new CreateUser(
                                "partner",
                                "Demo Delivery Partner",
                                "partner@fooddelivery.local",
                                "+919000000006",
                                Role.PARTNER));
        UserResponse partner2 =
                userService.create(
                        adminId,
                        new CreateUser(
                                "partner2",
                                "Second Delivery Partner",
                                "partner2@fooddelivery.local",
                                "+919000000007",
                                Role.PARTNER));

        CityResponse mumbai = catalogService.createCity(adminId, new CityInput("Mumbai", true));
        CityResponse pune = catalogService.createCity(adminId, new CityInput("Pune", true));

        RestaurantResponse kitchen =
                catalogService.createRestaurant(
                        adminId,
                        new RestaurantInput(
                                mumbai.id(),
                                owner.id(),
                                "Spice Kitchen",
                                "10 Market Road, Mumbai",
                                true));
        RestaurantResponse cafe =
                catalogService.createRestaurant(
                        adminId,
                        new RestaurantInput(
                                pune.id(), owner2.id(), "Garden Cafe", "20 Park Road, Pune", true));

        catalogService.createMenu(
                owner.id(),
                kitchen.id(),
                new MenuInput(
                        "Paneer Bowl",
                        "Paneer, rice and vegetables",
                        new BigDecimal("180.00"),
                        20L,
                        true));

        catalogService.createMenu(
                owner.id(),
                kitchen.id(),
                new MenuInput(
                        "Rice Bowl",
                        "Seasonal vegetables and rice",
                        new BigDecimal("120.00"),
                        10L,
                        true));

        catalogService.createMenu(
                owner2.id(),
                cafe.id(),
                new MenuInput(
                        "Pasta", "Tomato and basil pasta", new BigDecimal("220.00"), 15L, true));

        deliveryService.create(adminId, new PartnerInput(partner.id(), mumbai.id(), true));
        deliveryService.create(adminId, new PartnerInput(partner2.id(), mumbai.id(), true));
    }
}
