package com.dmg.fooddelivery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:concurrency-tests",
            "app.demo.enabled=true",
            "app.notifications.enabled=false"
        })
class ConcurrencyIntegrationTest {

    private static final AtomicInteger UNIQUE_SEQUENCE = new AtomicInteger(300);

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void concurrentOrdersCannotOversellOneRemainingItem() throws Exception {
        int suffix = UNIQUE_SEQUENCE.incrementAndGet();
        long menuItemId = createMenuItem("Single Item " + suffix, 1);
        long firstCustomerId = createCustomer("stock-customer-a-" + suffix, suffix * 10 + 1);
        long secondCustomerId = createCustomer("stock-customer-b-" + suffix, suffix * 10 + 2);

        ResponseEntity<String>[] responses =
                runConcurrently(
                        () ->
                                placeOrder(
                                        firstCustomerId,
                                        "stock-race-a-" + suffix,
                                        menuItemId),
                        () ->
                                placeOrder(
                                        secondCustomerId,
                                        "stock-race-b-" + suffix,
                                        menuItemId));

        assertStatusCounts(responses, HttpStatus.CREATED, HttpStatus.CONFLICT);
        assertEquals(0, menuStock(menuItemId));
    }

    @Test
    void concurrentPartnerClaimsProduceExactlyOneWinner() throws Exception {
        int suffix = UNIQUE_SEQUENCE.incrementAndGet();
        long menuItemId = createMenuItem("Claim Item " + suffix, 2);
        long customerId = createCustomer("claim-customer-" + suffix, suffix * 10 + 3);
        ResponseEntity<String> placed =
                placeOrder(customerId, "claim-race-" + suffix, menuItemId);
        assertEquals(HttpStatus.CREATED, placed.getStatusCode());
        long orderId = objectMapper.readTree(placed.getBody()).get("id").asLong();

        ResponseEntity<String> accepted =
                exchange(
                        "/api/orders/" + orderId + "/status",
                        HttpMethod.PATCH,
                        2L,
                        null,
                        Map.of("status", "ACCEPTED"));
        assertEquals(HttpStatus.OK, accepted.getStatusCode());

        ResponseEntity<String>[] responses =
                runConcurrently(
                        () ->
                                exchange(
                                        "/api/orders/" + orderId + "/claim",
                                        HttpMethod.POST,
                                        6L,
                                        null,
                                        null),
                        () ->
                                exchange(
                                        "/api/orders/" + orderId + "/claim",
                                        HttpMethod.POST,
                                        7L,
                                        null,
                                        null));

        assertStatusCounts(responses, HttpStatus.OK, HttpStatus.CONFLICT);

        ResponseEntity<String> order =
                exchange(
                        "/api/orders/" + orderId,
                        HttpMethod.GET,
                        customerId,
                        null,
                        null);
        assertEquals(HttpStatus.OK, order.getStatusCode());
        assertNotNull(objectMapper.readTree(order.getBody()).get("partnerId").numberValue());
    }

    private long createCustomer(String username, int phoneSuffix) throws Exception {
        ResponseEntity<String> response =
                exchange(
                        "/api/customers",
                        HttpMethod.POST,
                        null,
                        null,
                        Map.of(
                                "username",
                                username,
                                "fullName",
                                "Concurrent Customer " + phoneSuffix,
                                "email",
                                username + "@example.com",
                                "phoneNumber",
                                "+9188" + String.format("%08d", phoneSuffix)));
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        return objectMapper.readTree(response.getBody()).get("id").asLong();
    }

    private long createMenuItem(String name, int stock) throws Exception {
        ResponseEntity<String> response =
                exchange(
                        "/api/restaurants/1/menu",
                        HttpMethod.POST,
                        2L,
                        null,
                        Map.of(
                                "name",
                                name,
                                "description",
                                "Created for a concurrency integration test",
                                "price",
                                99.00,
                                "stock",
                                stock,
                                "available",
                                true));
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        return objectMapper.readTree(response.getBody()).get("id").asLong();
    }

    private ResponseEntity<String> placeOrder(long customerId, String key, long menuItemId)
            throws Exception {
        return exchange(
                "/api/orders",
                HttpMethod.POST,
                customerId,
                key,
                Map.of(
                        "restaurantId",
                        1,
                        "items",
                        List.of(Map.of("menuItemId", menuItemId, "quantity", 1)),
                        "deliveryAddress",
                        "Concurrent Test Address",
                        "paymentToken",
                        "TEST_SUCCESS"));
    }

    private long menuStock(long menuItemId) throws Exception {
        ResponseEntity<String> response =
                exchange("/api/restaurants/1/menu?size=100", HttpMethod.GET, null, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode items = objectMapper.readTree(response.getBody()).get("content");
        for (JsonNode item : items) {
            if (item.get("id").asLong() == menuItemId) {
                return item.get("stock").asLong();
            }
        }

        throw new AssertionError("Menu item not found: " + menuItemId);
    }

    private ResponseEntity<String> exchange(
            String path,
            HttpMethod method,
            Long actorId,
            String idempotencyKey,
            Object body)
            throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (actorId != null) {
            headers.set("X-User-Id", Long.toString(actorId));
        }
        if (idempotencyKey != null) {
            headers.set("Idempotency-Key", idempotencyKey);
        }

        String content = body == null ? null : objectMapper.writeValueAsString(body);
        HttpEntity<String> request = new HttpEntity<>(content, headers);

        return restTemplate.exchange(path, method, request, String.class);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<String>[] runConcurrently(
            ThrowingRequest firstRequest, ThrowingRequest secondRequest) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<ResponseEntity<String>> first =
                    executor.submit(
                            () -> {
                                ready.countDown();
                                start.await();
                                return firstRequest.execute();
                            });
            Future<ResponseEntity<String>> second =
                    executor.submit(
                            () -> {
                                ready.countDown();
                                start.await();
                                return secondRequest.execute();
                            });

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            return new ResponseEntity[] {
                first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)
            };
        } finally {
            executor.shutdownNow();
        }
    }

    private void assertStatusCounts(
            ResponseEntity<String>[] responses, HttpStatus first, HttpStatus second) {
        long firstCount =
                List.of(responses).stream()
                        .filter(response -> response.getStatusCode() == first)
                        .count();
        long secondCount =
                List.of(responses).stream()
                        .filter(response -> response.getStatusCode() == second)
                        .count();

        assertEquals(1, firstCount);
        assertEquals(1, secondCount);
    }

    @FunctionalInterface
    private interface ThrowingRequest {
        ResponseEntity<String> execute() throws Exception;
    }
}
