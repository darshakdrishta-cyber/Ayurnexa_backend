package com.Ayurnexa.Ayurnexa.controller;

import com.Ayurnexa.Ayurnexa.Service.JwtService;
import com.Ayurnexa.Ayurnexa.dto.UserDTO;
import com.Ayurnexa.Ayurnexa.model.User.User;
import com.Ayurnexa.Ayurnexa.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class GoogleAuthController {

    private final GoogleIdTokenVerifier verifier;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public GoogleAuthController(@Value("${google.client-id}") String clientId,
                                UserRepository userRepository,
                                JwtService jwtService) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleAuth(@Valid @RequestBody UserDTO.GoogleCredentialRequest request) {
        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(request.credential());
        } catch (GeneralSecurityException | IOException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Token verification failed"));
        }

        if (idToken == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        String email = payload.getEmail();
        boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        if (!emailVerified) {
            return ResponseEntity.status(403).body(Map.of("error", "Email not verified"));
        }

        User existing = userRepository.findByEmail(email).orElse(null);

        if (existing != null) {
            String token = jwtService.createToken(existing.getEmail(), existing.getRole());
            return ResponseEntity.ok(new UserDTO.GoogleAuthResponse(
                    "SIGNED_IN", token, "Bearer", jwtService.getExpirationSeconds(), toResponse(existing)));
        }

        User user = new User();
        user.setUsername(name != null && !name.isBlank() ? name : email.split("@")[0]);
        user.setEmail(email);
        user.setEmailVerified(true);
        user.setActive(true);
        user.setRole("USER");
        user.setProfilepicture(picture);
        if (request.phoneNumber() != null && !request.phoneNumber().isBlank()) {
            user.setPhoneNumber(request.phoneNumber());
        }

        User saved = userRepository.save(user);

        String token = jwtService.createToken(saved.getEmail(), saved.getRole());
        return ResponseEntity.ok(new UserDTO.GoogleAuthResponse(
                "SIGNED_UP", token, "Bearer", jwtService.getExpirationSeconds(), toResponse(saved)));
    }

    private UserDTO.Response toResponse(User user) {
        return new UserDTO.Response(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.isEmailVerified(),
                user.isPhoneVerified(),
                user.getRole());
    }
}
