package com.dmg.fooddelivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:notification-tests",
            "app.demo.enabled=true",
            "app.notifications.enabled=true",
            "app.notifications.initial-delay-ms=50",
            "app.notifications.delay-ms=50",
            "app.async.core-pool-size=2",
            "app.async.max-pool-size=4",
            "app.async.queue-capacity=20"
        })
@AutoConfigureMockMvc
class NotificationAsyncIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void committedOrderEventsAreDeliveredAsynchronouslyToEveryRecipient() throws Exception {
        MvcResult placed =
                mockMvc.perform(
                                post("/api/orders")
                                        .header("X-User-Id", 4)
                                        .header("Idempotency-Key", "notification-flow")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "restaurantId": 1,
                                                  "items": [{"menuItemId": 1, "quantity": 1}],
                                                  "deliveryAddress": "42 Lake Road, Mumbai",
                                                  "paymentToken": "TEST_SUCCESS"
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andReturn();
        long orderId =
                new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(placed.getResponse().getContentAsString())
                        .get("id")
                        .asLong();

        transition(orderId, 2, "ACCEPTED");

        mockMvc.perform(post("/api/orders/{id}/claim", orderId).header("X-User-Id", 6))
                .andExpect(status().isOk());

        transition(orderId, 2, "PREPARING");

        await().atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(
                        () -> {
                            mockMvc.perform(
                                            get("/api/notifications")
                                                    .header("X-User-Id", 4))
                                    .andExpect(status().isOk())
                                    .andExpect(jsonPath("$.totalElements", is(4)));

                            mockMvc.perform(
                                            get("/api/notifications")
                                                    .header("X-User-Id", 2))
                                    .andExpect(status().isOk())
                                    .andExpect(jsonPath("$.totalElements", is(4)));

                            mockMvc.perform(
                                            get("/api/notifications")
                                                    .header("X-User-Id", 6))
                                    .andExpect(status().isOk())
                                    .andExpect(
                                            jsonPath(
                                                    "$.totalElements",
                                                    greaterThanOrEqualTo(2)));
                        });
    }

    private void transition(long orderId, long actorId, String target) throws Exception {
        mockMvc.perform(
                        patch("/api/orders/{id}/status", orderId)
                                .header("X-User-Id", actorId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"" + target + "\"}"))
                .andExpect(status().isOk());
    }
}
