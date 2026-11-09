package com.Ayurnexa.Ayurnexa.controller;

import com.Ayurnexa.Ayurnexa.Service.SupabaseStorageService;
import com.Ayurnexa.Ayurnexa.config.JWTAnnotation;
import com.Ayurnexa.Ayurnexa.dto.upload.UploadDTO;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp",
            "image/avif", "image/bmp", "image/svg+xml",
            "application/pdf",
            "text/plain",
            "application/zip"
    );

    private static final List<String> DANGEROUS_EXTENSIONS = List.of(
            ".exe", ".bat", ".cmd", ".com", ".php", ".php3", ".php4", ".php5",
            ".phtml", ".asp", ".aspx", ".ashx", ".jsp", ".jspx", ".js", ".vbs",
            ".wsf", ".cgi", ".pl", ".py", ".rb", ".sh", ".html", ".htm"
    );

    private final SupabaseStorageService storageService;

    public UploadController(SupabaseStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            if (file == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file was provided");
            }
            if (file.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file must not be empty");
            }
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file must have a valid name");
            }
            String contentType = file.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }
            if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Unsupported file type: " + contentType);
            }
            String lowerName = originalFilename.toLowerCase();
            for (String dangerous : DANGEROUS_EXTENSIONS) {
                if (lowerName.endsWith(dangerous)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "File extension not allowed: " + dangerous);
                }
            }
            String ownerEmail = authentication == null ? "anonymous" : authentication.getName();
            SupabaseStorageService.UploadResult result = storageService.upload(file, ownerEmail);
            var upload = result.upload();
            String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/uploads/public/")
                    .path(result.publicAccessToken())
                    .toUriString();
            return ResponseEntity.status(HttpStatus.CREATED).body(new UploadDTO.UploadResponse(
                    upload.getId(),
                    url,
                    upload.getObjectKey(),
                    upload.getBucket(),
                    upload.getContentType(),
                    upload.getSize(),
                    upload.getCreatedAt()));
        } catch (ResponseStatusException exception) {
            return uploadError(exception.getStatusCode().value(), "UPLOAD_REQUEST_FAILED",
                    exception.getReason(), null);
        } catch (IllegalStateException exception) {
            HttpStatus status = exception.getMessage() != null && exception.getMessage().contains("not configured")
                    ? HttpStatus.SERVICE_UNAVAILABLE
                    : HttpStatus.INTERNAL_SERVER_ERROR;
            logger.error("Upload request failed", exception);
            return uploadError(status.value(),
                    status == HttpStatus.SERVICE_UNAVAILABLE ? "UPLOAD_NOT_CONFIGURED" : "UPLOAD_FAILED",
                    exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            logger.error("Unexpected upload request failure", exception);
            return uploadError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "UPLOAD_FAILED",
                    exception.getMessage(), exception);
        }
    }

    private static ResponseEntity<Map<String, Object>> uploadError(
            int status, String error, String message, Throwable exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("status", status);
        body.put("message", message == null || message.isBlank() ? error : message);
        Throwable rootCause = exception;
        while (rootCause != null && rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        if (rootCause != null && rootCause != exception
                && rootCause.getMessage() != null && !rootCause.getMessage().isBlank()) {
            body.put("details", rootCause.getMessage());
        }
        return ResponseEntity.status(status).body(body);
    }

    @JWTAnnotation
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
