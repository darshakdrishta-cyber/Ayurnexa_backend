package com.Ayurnexa.Ayurnexa.controller;

import java.util.List;

import com.Ayurnexa.Ayurnexa.Service.OrderService;
import com.Ayurnexa.Ayurnexa.config.JWTAnnotation;
import com.Ayurnexa.Ayurnexa.dto.order.OrderDTO;
import com.Ayurnexa.Ayurnexa.model.User.User;
import com.Ayurnexa.Ayurnexa.repository.OrderRepository;
import com.Ayurnexa.Ayurnexa.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public AdminController(UserRepository userRepository,
                           OrderRepository orderRepository,
                           OrderService orderService) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    /**
     * Check that the authenticated user has the ADMIN role (case-insensitive).
     * Checks both Spring Security authorities (e.g. ROLE_ADMIN) and database user role.
     */
    private void verifyAdminRole(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        // Check authorities in SecurityContext (ignoring case, e.g. "ROLE_ADMIN", "ADMIN")
        boolean hasAdminAuthority = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> {
                    String auth = grantedAuthority.getAuthority();
                    return auth.equalsIgnoreCase("ADMIN") || auth.equalsIgnoreCase("ROLE_ADMIN");
                });

        if (hasAdminAuthority) {
            return;
        }

        // Check user entity in repository (case-insensitive check for "ADMIN")
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (currentUser.getRole() == null || !currentUser.getRole().equalsIgnoreCase("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: ADMIN role required");
        }
    }

    /**
     * Fetch user details by userId or user email (Admin only).
     */
    @JWTAnnotation
    @GetMapping("/users/{userId}")
    public User getUserById(@PathVariable String userId, Authentication authentication) {
        verifyAdminRole(authentication);
        return userRepository.findByUserId(userId)
                .or(() -> userRepository.findById(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found for userId: " + userId));
    }

    /**
     * Get user details and/or orders by user email (Admin only).
     */
    @JWTAnnotation
    @GetMapping("/users/by-email")
    public User getUserByEmail(@RequestParam("email") String email, Authentication authentication) {
        verifyAdminRole(authentication);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found for email: " + email));
    }

    /**
     * Get order details by orderId (Admin only).
     */
    @JWTAnnotation
    @GetMapping("/orders/{orderId}")
    public OrderDTO.Response getOrderById(@PathVariable String orderId, Authentication authentication) {
        verifyAdminRole(authentication);
        try {
            return orderService.getByOrderId(orderId);
        } catch (java.util.NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    /**
     * Update order status by orderId (Admin only).
     */
    @JWTAnnotation
    @PutMapping("/orders/{orderId}/status")
    public OrderDTO.Response updateOrderStatus(@PathVariable String orderId,
                                              @Valid @RequestBody OrderDTO.UpdateRequest request,
                                              Authentication authentication) {
        verifyAdminRole(authentication);
        return orderService.adminUpdateOrderStatus(orderId, request);
    }
}
