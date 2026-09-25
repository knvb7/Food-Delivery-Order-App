package com.dmg.fooddelivery.config;

import com.dmg.fooddelivery.catalog.CatalogModels.*;
import com.dmg.fooddelivery.catalog.CatalogRepository;
import com.dmg.fooddelivery.delivery.DeliveryModels.PartnerInput;
import com.dmg.fooddelivery.delivery.DeliveryRepository;
import com.dmg.fooddelivery.security.Accounts;
import com.dmg.fooddelivery.security.Role;
import java.math.BigDecimal;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoData implements ApplicationRunner {
    private final Accounts accounts;
    private final CatalogRepository catalog;
    private final DeliveryRepository partners;
    private final JdbcTemplate jdbc;
    public DemoData(Accounts accounts, CatalogRepository catalog, DeliveryRepository partners, JdbcTemplate jdbc) {
        this.accounts = accounts; this.catalog = catalog; this.partners = partners; this.jdbc = jdbc;
    }

    @Override @Transactional
    public void run(ApplicationArguments args) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM app_users", Long.class) != 0) return;
        String password = "demo-password";
        accounts.create("admin", password, Role.ADMIN);
        var owner = accounts.create("owner", password, Role.OWNER);
        var owner2 = accounts.create("owner2", password, Role.OWNER);
        accounts.create("customer", password, Role.CUSTOMER);
        accounts.create("customer2", password, Role.CUSTOMER);
        var partner = accounts.create("partner", password, Role.PARTNER);
        var partner2 = accounts.create("partner2", password, Role.PARTNER);
        var mumbai = catalog.createCity(new CityInput("Mumbai", true));
        var pune = catalog.createCity(new CityInput("Pune", true));
        var kitchen = catalog.createRestaurant(new RestaurantInput(mumbai.id(), owner.id(), "Spice Kitchen", "10 Market Road, Mumbai", true));
        var cafe = catalog.createRestaurant(new RestaurantInput(pune.id(), owner2.id(), "Garden Cafe", "20 Park Road, Pune", true));
        catalog.createMenu(kitchen.id(), new MenuInput("Paneer Bowl", "Paneer, rice and vegetables", new BigDecimal("180.00"), 20, true));
        catalog.createMenu(kitchen.id(), new MenuInput("Rice Bowl", "Seasonal vegetables and rice", new BigDecimal("120.00"), 10, true));
        catalog.createMenu(cafe.id(), new MenuInput("Pasta", "Tomato and basil pasta", new BigDecimal("220.00"), 15, true));
        partners.create(new PartnerInput(partner.id(), mumbai.id(), true));
        partners.create(new PartnerInput(partner2.id(), mumbai.id(), true));
    }
}
