package com.Ayurnexa.Ayurnexa.controller;

import java.util.List;

import com.Ayurnexa.Ayurnexa.Service.OrderService;
import com.Ayurnexa.Ayurnexa.config.JWTAnnotation;
import com.Ayurnexa.Ayurnexa.dto.order.OrderDTO;
import com.Ayurnexa.Ayurnexa.model.User.User;
import com.Ayurnexa.Ayurnexa.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    public OrderController(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @JWTAnnotation
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDTO.Response create(@Valid @RequestBody OrderDTO.CreateRequest request,
                                    Authentication authentication) {
        return orderService.create(currentUserId(authentication), request);
    }

    @JWTAnnotation
    @GetMapping
    public List<OrderDTO.Response> list(Authentication authentication) {
        return orderService.list(currentUserId(authentication));
    }

    @JWTAnnotation
    @GetMapping("/{orderId}")
    public OrderDTO.Response get(@PathVariable String orderId, Authentication authentication) {
        return orderService.get(currentUserId(authentication), orderId);
    }

    @JWTAnnotation
    @PutMapping("/{orderId}")
    public OrderDTO.Response update(@PathVariable String orderId,
                                    @Valid @RequestBody OrderDTO.UpdateRequest request,
                                    Authentication authentication) {
        return orderService.update(currentUserId(authentication), orderId, request);
    }

    @JWTAnnotation
    @DeleteMapping("/{orderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String orderId, Authentication authentication) {
        orderService.delete(currentUserId(authentication), orderId);
    }

    private String currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return user.getUserId();
    }
}