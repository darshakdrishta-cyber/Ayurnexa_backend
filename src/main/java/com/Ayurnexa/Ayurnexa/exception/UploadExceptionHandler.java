package com.Ayurnexa.Ayurnexa.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class UploadExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleFileTooLarge(MaxUploadSizeExceededException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "FILE_TOO_LARGE");
        body.put("message", "Uploaded file exceeds the maximum allowed size of 50 MB");
        body.put("maxFileSizeBytes", 50L * 1024 * 1024);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException exception) {
        String message = exception.getMessage() != null ? exception.getMessage() : "Server configuration error";
        HttpStatus status = message.contains("not configured") ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.INTERNAL_SERVER_ERROR;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "SERVER_ERROR");
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
