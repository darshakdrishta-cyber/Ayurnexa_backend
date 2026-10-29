
package com.Ayurnexa.Ayurnexa.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class UserDTO {

    private UserDTO() {}

    public record GoogleCredentialRequest(
        @NotBlank
        String credential,
        @JsonProperty(required = false)
        String phoneNumber
    ) {}

    public record Response(
        String userId,
        String username,
        String email,
        boolean emailVerified,
        boolean phoneVerified,
        String role
    ) {}

    public record AuthenticatedUser(String email, List<String> roles) {}

    public record GoogleAuthResponse(
        String action,
        String token,
        String tokenType,
        long expiresIn,
        Response user
    ) {}
}
