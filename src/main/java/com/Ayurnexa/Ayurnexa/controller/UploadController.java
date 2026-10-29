package com.Ayurnexa.Ayurnexa.controller;

import com.Ayurnexa.Ayurnexa.Service.SupabaseStorageService;
import com.Ayurnexa.Ayurnexa.config.JWTAnnotation;
import com.Ayurnexa.Ayurnexa.dto.upload.UploadDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final SupabaseStorageService storageService;

    public UploadController(SupabaseStorageService storageService) {
        this.storageService = storageService;
    }

    @JWTAnnotation
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UploadDTO.UploadResponse upload(@RequestParam("file") MultipartFile file) {
        SupabaseStorageService.UploadResult result = storageService.upload(file);
        return new UploadDTO.UploadResponse(result.url(), result.path(), result.bucket());
    }
}
