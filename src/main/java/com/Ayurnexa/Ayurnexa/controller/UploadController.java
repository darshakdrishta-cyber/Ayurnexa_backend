package com.Ayurnexa.Ayurnexa.controller;

import com.Ayurnexa.Ayurnexa.Service.SupabaseStorageService;
import com.Ayurnexa.Ayurnexa.config.JWTAnnotation;
import com.Ayurnexa.Ayurnexa.dto.upload.UploadDTO;
import java.nio.charset.StandardCharsets;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final SupabaseStorageService storageService;

    public UploadController(SupabaseStorageService storageService) {
        this.storageService = storageService;
    }

    //@JWTAnnotation
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UploadDTO.UploadResponse upload(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file must not be empty");
        }
        String ownerEmail = authentication == null ? "anonymous" : authentication.getName();
        SupabaseStorageService.UploadResult result = storageService.upload(file, ownerEmail);
        var upload = result.upload();
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/uploads/public/")
                .path(result.publicAccessToken())
                .toUriString();
        return new UploadDTO.UploadResponse(
                upload.getId(),
                url,
                upload.getObjectKey(),
                upload.getBucket(),
                upload.getContentType(),
                upload.getSize(),
                upload.getCreatedAt());
    }

    //@JWTAnnotation
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> download(@PathVariable String id, Authentication authentication) {
        SupabaseStorageService.DownloadResult result = storageService.download(id, authentication.getName());
        return downloadResponse(result, true);
    }

    @GetMapping("/public/{token}")
    public ResponseEntity<byte[]> downloadPublic(@PathVariable String token) {
        SupabaseStorageService.DownloadResult result = storageService.downloadPublic(token);
        return downloadResponse(result, false);
    }

    private ResponseEntity<byte[]> downloadResponse(
            SupabaseStorageService.DownloadResult result,
            boolean attachment) {
        var upload = result.upload();
        String filename = upload.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            filename = "download";
        }
        filename = filename.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1);

        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(upload.getContentType());
        } catch (IllegalArgumentException exception) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }

        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .contentType(contentType)
                .header("X-Content-Type-Options", "nosniff");
        boolean inlineImage = !attachment && isInlineImage(contentType);
        ContentDisposition disposition = inlineImage
                ? ContentDisposition.inline().filename(filename, StandardCharsets.UTF_8).build()
                : ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build();
        return response
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(result.bytes());
    }

    private static boolean isInlineImage(MediaType contentType) {
        return MediaType.IMAGE_PNG.isCompatibleWith(contentType)
                || MediaType.IMAGE_JPEG.isCompatibleWith(contentType)
                || MediaType.IMAGE_GIF.isCompatibleWith(contentType)
                || "image/webp".equalsIgnoreCase(contentType.toString())
                || "image/avif".equalsIgnoreCase(contentType.toString())
                || "image/bmp".equalsIgnoreCase(contentType.toString());
    }
}
