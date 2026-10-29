package com.Ayurnexa.Ayurnexa.controller;

import java.util.List;

import com.Ayurnexa.Ayurnexa.Service.PaymentService;
import com.Ayurnexa.Ayurnexa.dto.payment.PaymentDTO;
import com.Ayurnexa.Ayurnexa.model.User.User;
import com.Ayurnexa.Ayurnexa.repository.UserRepository;
import com.Ayurnexa.Ayurnexa.config.JWTAnnotation;
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
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    public PaymentController(PaymentService paymentService, UserRepository userRepository) {
        this.paymentService = paymentService;
        this.userRepository = userRepository;
    }

    @JWTAnnotation
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentDTO.Response create(
            @Valid @RequestBody PaymentDTO.CreateRequest request,
            Authentication authentication) {
        return paymentService.createPayment(currentUserId(authentication), request);
    }

    @JWTAnnotation
    @GetMapping
    public List<PaymentDTO.Response> list(Authentication authentication) {
        return paymentService.getPaymentsByUser(currentUserId(authentication));
    }

    @JWTAnnotation
    @GetMapping("/{paymentRecordId}")
    public PaymentDTO.Response get(
            @PathVariable String paymentRecordId,
            Authentication authentication) {
        return paymentService.getPaymentForUser(currentUserId(authentication), paymentRecordId);
    }

    @JWTAnnotation
    @PutMapping("/{paymentRecordId}")
    public PaymentDTO.Response update(
            @PathVariable String paymentRecordId,
            @Valid @RequestBody PaymentDTO.UpdateRequest request,
            Authentication authentication) {
        return paymentService.updatePayment(currentUserId(authentication), paymentRecordId, request);
    }

    @JWTAnnotation
    @DeleteMapping("/{paymentRecordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String paymentRecordId, Authentication authentication) {
        paymentService.deletePayment(currentUserId(authentication), paymentRecordId);
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