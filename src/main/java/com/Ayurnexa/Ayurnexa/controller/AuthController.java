package com.Ayurnexa.Ayurnexa.controller;

import com.Ayurnexa.Ayurnexa.config.JWTAnnotation;
import com.Ayurnexa.Ayurnexa.dto.UserDTO;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    @JWTAnnotation
    public UserDTO.AuthenticatedUser me(Authentication authentication) {
        return new UserDTO.AuthenticatedUser(
                authentication.getName(),
                authentication.getAuthorities().stream().map(authority -> authority.getAuthority()).toList());
    }
}