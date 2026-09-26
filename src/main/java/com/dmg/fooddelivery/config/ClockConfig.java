package com.dmg.fooddelivery.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    @Value("${app.restaurant.time-zone:Asia/Kolkata}")
    private String restaurantTimeZone;

    @Bean
    public Clock restaurantClock() {
        return Clock.system(ZoneId.of(restaurantTimeZone));
    }
}
