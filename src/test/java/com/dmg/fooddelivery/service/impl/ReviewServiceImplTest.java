package com.dmg.fooddelivery.service.impl;

import com.dmg.fooddelivery.common.ApiException;
import com.dmg.fooddelivery.dto.ReviewDtos.RestaurantReviews;
import com.dmg.fooddelivery.dto.ReviewDtos.ReviewInput;
import com.dmg.fooddelivery.dto.ReviewDtos.ReviewResponse;
import com.dmg.fooddelivery.model.CustomerOrder;
import com.dmg.fooddelivery.model.OrderStatus;
import com.dmg.fooddelivery.model.Restaurant;
import com.dmg.fooddelivery.model.Review;
import com.dmg.fooddelivery.model.Role;
import com.dmg.fooddelivery.model.User;
import com.dmg.fooddelivery.repository.OrderRepository;
import com.dmg.fooddelivery.repository.RestaurantRepository;
import com.dmg.fooddelivery.repository.ReviewRepository;
import com.dmg.fooddelivery.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User customer;
    private Restaurant restaurant;
    private CustomerOrder order;

    @BeforeEach
    void setUp() {
        customer = user(4, Role.CUSTOMER);
        restaurant = new Restaurant();
        restaurant.setId(1L);

        order = new CustomerOrder();
        order.setId(10L);
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setStatus(OrderStatus.DELIVERED);
    }

    @Test
    void customerCanReviewOwnDeliveredOrder() {
        when(userService.get(4)).thenReturn(customer);
        when(orderRepository.findLockedById(10L)).thenReturn(Optional.of(order));
        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(
                        invocation -> {
                            Review review = invocation.getArgument(0);
                            review.setId(20L);
                            return review;
                        });

        ReviewResponse response =
                reviewService.create(4, 10, new ReviewInput(5, "  Excellent delivery  "));

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(reviewCaptor.capture());
        Review savedReview = reviewCaptor.getValue();
        assertEquals(order, savedReview.getOrder());
        assertEquals(customer, savedReview.getCustomer());
        assertEquals(restaurant, savedReview.getRestaurant());
        assertEquals("Excellent delivery", savedReview.getComment());
        assertEquals(5, response.rating());
    }

    @Test
    void reviewRequiresCustomerRole() {
        when(userService.get(2)).thenReturn(user(2, Role.OWNER));

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> reviewService.create(2, 10, new ReviewInput(5, "Great")));

        assertEquals(403, exception.status().value());
        verify(orderRepository, never()).findLockedById(10L);
    }

    @Test
    void customerCannotReviewAnotherCustomersOrder() {
        User anotherCustomer = user(5, Role.CUSTOMER);
        when(userService.get(5)).thenReturn(anotherCustomer);
        when(orderRepository.findLockedById(10L)).thenReturn(Optional.of(order));

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> reviewService.create(5, 10, new ReviewInput(5, "Great")));

        assertEquals(403, exception.status().value());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void orderMustBeDeliveredBeforeReview() {
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        when(userService.get(4)).thenReturn(customer);
        when(orderRepository.findLockedById(10L)).thenReturn(Optional.of(order));

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> reviewService.create(4, 10, new ReviewInput(5, "Great")));

        assertEquals("Only delivered orders can be reviewed", exception.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void orderCanOnlyBeReviewedOnce() {
        when(userService.get(4)).thenReturn(customer);
        when(orderRepository.findLockedById(10L)).thenReturn(Optional.of(order));
        when(reviewRepository.existsByOrderId(10L)).thenReturn(true);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> reviewService.create(4, 10, new ReviewInput(5, "Again")));

        assertEquals("This order has already been reviewed", exception.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void listReturnsRoundedAverageAndPaginatedReviews() {
        Review review = review(20, 5, "Excellent");
        when(restaurantRepository.existsById(1L)).thenReturn(true);
        when(reviewRepository.findByRestaurantId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));
        when(reviewRepository.averageRating(1L)).thenReturn(4.666D);

        RestaurantReviews result = reviewService.list(1, 0, 20);

        assertEquals(new BigDecimal("4.67"), result.averageRating());
        assertEquals(1, result.reviews().totalElements());
        assertEquals("Excellent", result.reviews().content().get(0).comment());
    }

    @Test
    void listUsesNullAverageWhenRestaurantHasNoReviews() {
        when(restaurantRepository.existsById(1L)).thenReturn(true);
        when(reviewRepository.findByRestaurantId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(reviewRepository.averageRating(1L)).thenReturn(null);

        RestaurantReviews result = reviewService.list(1, 0, 20);

        assertNull(result.averageRating());
        assertEquals(0, result.reviews().totalElements());
    }

    @Test
    void listRejectsUnknownRestaurantBeforeQueryingReviews() {
        when(restaurantRepository.existsById(404L)).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class, () -> reviewService.list(404, 0, 20));

        assertEquals(404, exception.status().value());
        verify(reviewRepository, never()).findByRestaurantId(eq(404L), any(Pageable.class));
    }

    private Review review(long id, int rating, String comment) {
        Review review = new Review();
        review.setId(id);
        review.setOrder(order);
        review.setRestaurant(restaurant);
        review.setCustomer(customer);
        review.setRating(rating);
        review.setComment(comment);
        return review;
    }

    private static User user(long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }
}
