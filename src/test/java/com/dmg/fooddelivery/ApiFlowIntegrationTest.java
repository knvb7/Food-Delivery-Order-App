package com.dmg.fooddelivery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:api-flow-tests",
            "app.demo.enabled=true",
            "app.notifications.enabled=false"
        })
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ApiFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Clock restaurantClock;

    @Test
    void publicCatalogBrowsingAndSearchWork() throws Exception {
        mockMvc.perform(get("/api/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content", hasSize(2)));

        mockMvc.perform(get("/api/restaurants").param("cityId", "1").param("q", "SPICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Spice Kitchen")))
                .andExpect(jsonPath("$.content[0].openNow", is(true)))
                .andExpect(jsonPath("$.content[0].timeZone", is("Asia/Kolkata")));

        mockMvc.perform(get("/api/restaurants/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.ownerId", is(2)));

        mockMvc.perform(get("/api/restaurants/1/menu").param("q", "rice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)));

        mockMvc.perform(get("/api/restaurants").param("q", "x".repeat(151)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_REQUEST")));
    }

    @Test
    void userRegistrationAdminCreationAndProfileValidationWork() throws Exception {
        long customerId =
                responseId(
                        mockMvc.perform(
                                        post("/api/customers")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(
                                                        json(
                                                                Map.of(
                                                                        "username",
                                                                        "new-customer",
                                                                        "fullName",
                                                                        "New Customer",
                                                                        "email",
                                                                        "NEW.CUSTOMER@example.com",
                                                                        "phoneNumber",
                                                                        "+919876543210"))))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.email", is("new.customer@example.com")))
                                .andExpect(jsonPath("$.role", is("CUSTOMER")))
                                .andReturn());

        mockMvc.perform(get("/api/me").header("X-User-Id", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("New Customer")))
                .andExpect(jsonPath("$.phoneNumber", is("+919876543210")));

        mockMvc.perform(
                        post("/api/admin/users")
                                .header("X-User-Id", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json(
                                                userPayload(
                                                        "new-admin",
                                                        "New Administrator",
                                                        "new.admin@example.com",
                                                        "+919876543211",
                                                        "ADMIN"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role", is("ADMIN")));

        mockMvc.perform(
                        post("/api/admin/users")
                                .header("X-User-Id", 4)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json(
                                                userPayload(
                                                        "forbidden-admin",
                                                        "Forbidden Administrator",
                                                        "forbidden.admin@example.com",
                                                        "+919876543212",
                                                        "ADMIN"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        post("/api/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json(
                                                Map.of(
                                                        "username",
                                                        "duplicate-email",
                                                        "fullName",
                                                        "Duplicate Email",
                                                        "email",
                                                        "new.customer@example.com",
                                                        "phoneNumber",
                                                        "+919876543213"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Email already exists")));

        mockMvc.perform(
                        post("/api/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "username": "invalid-phone",
                                          "fullName": "Invalid Phone",
                                          "email": "invalid.phone@example.com",
                                          "phoneNumber": "123"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.fields.phoneNumber", notNullValue()));
    }

    @Test
    void adminOwnerAndDeliveryManagementFlowsWork() throws Exception {
        long cityId =
                responseId(
                        mockMvc.perform(
                                        post("/api/admin/cities")
                                                .header("X-User-Id", 1)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(
                                                        json(
                                                                Map.of(
                                                                        "name",
                                                                        "Delhi",
                                                                        "active",
                                                                        true))))
                                .andExpect(status().isCreated())
                                .andReturn());

        mockMvc.perform(
                        put("/api/admin/cities/{id}", cityId)
                                .header("X-User-Id", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("name", "New Delhi", "active", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("New Delhi")));

        long ownerId = createUser("delhi-owner", "Delhi Owner", 220, "OWNER");
        long restaurantId =
                responseId(
                        mockMvc.perform(
                                        post("/api/admin/restaurants")
                                                .header("X-User-Id", 1)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(
                                                        json(
                                                                Map.of(
                                                                        "cityId",
                                                                        cityId,
                                                                        "ownerId",
                                                                        ownerId,
                                                                        "name",
                                                                        "Delhi Kitchen",
                                                                        "address",
                                                                        "10 Delhi Road",
                                                                        "active",
                                                                        true))))
                                .andExpect(status().isCreated())
                                .andReturn());

        mockMvc.perform(
                        put("/api/admin/restaurants/{id}", restaurantId)
                                .header("X-User-Id", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json(
                                                Map.of(
                                                        "name",
                                                        "Delhi Kitchen Updated",
                                                        "address",
                                                        "11 Delhi Road",
                                                        "active",
                                                        true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Delhi Kitchen Updated")));

        mockMvc.perform(
                        put("/api/restaurants/{id}/opening-hours", restaurantId)
                                .header("X-User-Id", ownerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json(
                                                Map.of(
                                                        "opensAt",
                                                        "09:00:00",
                                                        "closesAt",
                                                        "23:00:00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opensAt", is("09:00:00")))
                .andExpect(jsonPath("$.closesAt", is("23:00:00")));

        long menuItemId =
                responseId(
                        mockMvc.perform(
                                        post("/api/restaurants/{id}/menu", restaurantId)
                                                .header("X-User-Id", ownerId)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(
                                                        json(
                                                                Map.of(
                                                                        "name",
                                                                        "Delhi Bowl",
                                                                        "description",
                                                                        "Rice and vegetables",
                                                                        "price",
                                                                        250.00,
                                                                        "stock",
                                                                        5,
                                                                        "available",
                                                                        true))))
                                .andExpect(status().isCreated())
                                .andReturn());

        mockMvc.perform(
                        put("/api/restaurants/{restaurantId}/menu/{id}",
                                        restaurantId, menuItemId)
                                .header("X-User-Id", ownerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json(
                                                Map.of(
                                                        "name",
                                                        "Delhi Special Bowl",
                                                        "description",
                                                        "Rice, paneer and vegetables",
                                                        "price",
                                                        275.00,
                                                        "available",
                                                        true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Delhi Special Bowl")))
                .andExpect(jsonPath("$.stock", is(5)));

        mockMvc.perform(
                        post("/api/restaurants/{restaurantId}/menu/{id}/stock",
                                        restaurantId, menuItemId)
                                .header("X-User-Id", ownerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("delta", 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock", is(8)));

        long partnerUserId = createUser("delhi-partner", "Delhi Partner", 221, "PARTNER");
        long partnerId =
                responseId(
                        mockMvc.perform(
                                        post("/api/admin/partners")
                                                .header("X-User-Id", 1)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(
                                                        json(
                                                                Map.of(
                                                                        "userId",
                                                                        partnerUserId,
                                                                        "cityId",
                                                                        cityId,
                                                                        "active",
                                                                        true))))
                                .andExpect(status().isCreated())
                                .andReturn());

        mockMvc.perform(get("/api/admin/partners").header("X-User-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3)));

        mockMvc.perform(get("/api/delivery/me").header("X-User-Id", partnerUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) partnerId)));

        mockMvc.perform(
                        put("/api/admin/partners/{id}", partnerId)
                                .header("X-User-Id", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("cityId", cityId, "active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));

        mockMvc.perform(
                        get("/api/delivery/orders/available").header("X-User-Id", partnerUserId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Partner is inactive")));
    }

    @Test
    void completeOrderDeliveryReviewAndIdempotencyFlowWorks() throws Exception {
        String request = orderPayload(1, 1, 2, "TEST_SUCCESS");
        MvcResult created =
                mockMvc.perform(
                                post("/api/orders")
                                        .header("X-User-Id", 4)
                                        .header("Idempotency-Key", "complete-flow")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(request))
                        .andExpect(status().isCreated())
                        .andExpect(header().string("Idempotency-Replayed", "false"))
                        .andExpect(jsonPath("$.status", is("PLACED")))
                        .andExpect(jsonPath("$.payment.status", is("CAPTURED")))
                        .andExpect(jsonPath("$.total", is(360.0)))
                        .andReturn();
        long orderId = responseId(created);

        mockMvc.perform(
                        post("/api/orders")
                                .header("X-User-Id", 4)
                                .header("Idempotency-Key", "complete-flow")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(jsonPath("$.id", is((int) orderId)));

        mockMvc.perform(
                        post("/api/orders")
                                .header("X-User-Id", 4)
                                .header("Idempotency-Key", "complete-flow")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(orderPayload(1, 1, 1, "TEST_SUCCESS")))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/orders").header("X-User-Id", 4).param("status", "PLACED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)));

        mockMvc.perform(get("/api/orders/{id}", orderId).header("X-User-Id", 4))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history[0].type", is("PLACED")));

        transition(orderId, 2, "ACCEPTED", 200);

        mockMvc.perform(get("/api/delivery/orders/available").header("X-User-Id", 6))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id", is((int) orderId)));

        mockMvc.perform(post("/api/orders/{id}/claim", orderId).header("X-User-Id", 6))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partnerId", is(1)));

        mockMvc.perform(get("/api/delivery/me").header("X-User-Id", 6))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeOrderId", is((int) orderId)));

        transition(orderId, 2, "PREPARING", 200);
        transition(orderId, 6, "OUT_FOR_DELIVERY", 200);
        transition(orderId, 6, "DELIVERED", 200);

        mockMvc.perform(get("/api/delivery/me").header("X-User-Id", 6))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeOrderId").doesNotExist());

        mockMvc.perform(
                        post("/api/orders/{id}/review", orderId)
                                .header("X-User-Id", 4)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("rating", 5, "comment", "Excellent"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating", is(5)));

        mockMvc.perform(
                        post("/api/orders/{id}/review", orderId)
                                .header("X-User-Id", 4)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("rating", 4, "comment", "Again"))))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/restaurants/1/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating", is(5.0)))
                .andExpect(jsonPath("$.reviews.totalElements", is(1)));

        mockMvc.perform(get("/api/notifications").header("X-User-Id", 4))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    void cancellationAndRejectionRestoreStockAndRefundPayment() throws Exception {
        long initialStock = menuStock(1, 1);

        long cancelledOrderId = placeOrder(4, "cancel-flow", 1, 1, 2, "TEST_SUCCESS", 201);
        transition(cancelledOrderId, 4, "CANCELLED", 200)
                .andExpect(jsonPath("$.status", is("CANCELLED")))
                .andExpect(jsonPath("$.payment.status", is("REFUNDED")));
        org.junit.jupiter.api.Assertions.assertEquals(initialStock, menuStock(1, 1));

        long rejectedOrderId = placeOrder(4, "reject-flow", 1, 1, 3, "TEST_SUCCESS", 201);
        transition(rejectedOrderId, 2, "REJECTED", 200)
                .andExpect(jsonPath("$.status", is("REJECTED")))
                .andExpect(jsonPath("$.payment.status", is("REFUNDED")));
        org.junit.jupiter.api.Assertions.assertEquals(initialStock, menuStock(1, 1));
    }

    @Test
    void declinedPaymentClosedRestaurantAndInvalidRequestsRollbackCleanly() throws Exception {
        long initialStock = menuStock(1, 1);

        placeOrder(4, "declined-flow", 1, 1, 2, "TEST_DECLINE", 402);
        org.junit.jupiter.api.Assertions.assertEquals(initialStock, menuStock(1, 1));

        mockMvc.perform(get("/api/orders").header("X-User-Id", 4))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)));

        LocalTime oppositeTime = LocalTime.now(restaurantClock).plusHours(12).truncatedTo(ChronoUnit.SECONDS);
        LocalTime closeTime = oppositeTime.plusHours(1);
        mockMvc.perform(
                        put("/api/restaurants/1/opening-hours")
                                .header("X-User-Id", 2)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json(
                                                Map.of(
                                                        "opensAt",
                                                        oppositeTime.toString(),
                                                        "closesAt",
                                                        closeTime.toString()))))
                .andExpect(status().isOk());

        placeOrder(4, "closed-flow", 1, 1, 1, "TEST_SUCCESS", 409);
        org.junit.jupiter.api.Assertions.assertEquals(initialStock, menuStock(1, 1));

        mockMvc.perform(get("/api/cities").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_REQUEST")));

        mockMvc.perform(
                        post("/api/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "username": "unknown-field",
                                          "fullName": "Unknown Field",
                                          "email": "unknown.field@example.com",
                                          "phoneNumber": "+919876543250",
                                          "unexpected": true
                                        }
                                        """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/orders/99999").header("X-User-Id", 4))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Endpoint")));
    }

    private long createUser(String username, String fullName, int suffix, String role)
            throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/api/admin/users")
                                        .header("X-User-Id", 1)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                json(
                                                        userPayload(
                                                                username,
                                                                fullName,
                                                                username + "@example.com",
                                                                "+919876543" + suffix,
                                                                role))))
                        .andExpect(status().isCreated())
                        .andReturn();

        return responseId(result);
    }

    private Map<String, Object> userPayload(
            String username, String fullName, String email, String phoneNumber, String role) {
        return Map.of(
                "username", username,
                "fullName", fullName,
                "email", email,
                "phoneNumber", phoneNumber,
                "role", role);
    }

    private String orderPayload(
            long restaurantId, long menuItemId, int quantity, String paymentToken)
            throws Exception {
        return json(
                Map.of(
                        "restaurantId",
                        restaurantId,
                        "items",
                        List.of(Map.of("menuItemId", menuItemId, "quantity", quantity)),
                        "deliveryAddress",
                        "42 Lake Road, Mumbai",
                        "paymentToken",
                        paymentToken));
    }

    private long placeOrder(
            long actorId,
            String key,
            long restaurantId,
            long menuItemId,
            int quantity,
            String paymentToken,
            int expectedStatus)
            throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/api/orders")
                                        .header("X-User-Id", actorId)
                                        .header("Idempotency-Key", key)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                orderPayload(
                                                        restaurantId,
                                                        menuItemId,
                                                        quantity,
                                                        paymentToken)))
                        .andExpect(status().is(expectedStatus))
                        .andReturn();

        if (expectedStatus >= 400) {
            return 0;
        }

        return responseId(result);
    }

    private org.springframework.test.web.servlet.ResultActions transition(
            long orderId, long actorId, String target, int expectedStatus) throws Exception {
        return mockMvc.perform(
                        patch("/api/orders/{id}/status", orderId)
                                .header("X-User-Id", actorId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("status", target))))
                .andExpect(status().is(expectedStatus));
    }

    private long menuStock(long restaurantId, long menuItemId) throws Exception {
        MvcResult result =
                mockMvc.perform(get("/api/restaurants/{id}/menu", restaurantId))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString()).get("content");
        for (JsonNode menuItem : content) {
            if (menuItem.get("id").asLong() == menuItemId) {
                return menuItem.get("stock").asLong();
            }
        }

        throw new AssertionError("Menu item not found: " + menuItemId);
    }

    private long responseId(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
