package com.Ayurnexa.Ayurnexa.controller;

import com.Ayurnexa.Ayurnexa.Service.EmailService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/emails")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    public record SendRequest(@NotBlank @Email String to,
                              @NotBlank String subject,
                              @NotBlank String body,
                              boolean html) {}

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void send(@Valid @RequestBody SendRequest request) {
        if (request.html()) {
            try {
                emailService.sendHtmlEmail(request.to(), request.subject(), request.body());
            } catch (jakarta.mail.MessagingException e) {
                throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
            }
        } else {
            emailService.sendSimpleEmail(request.to(), request.subject(), request.body());
        }
    }
}
