package com.Ayurnexa.Ayurnexa.controller;

import java.util.List;

import com.Ayurnexa.Ayurnexa.Service.ProductService;
import com.Ayurnexa.Ayurnexa.config.JWTAnnotation;
import com.Ayurnexa.Ayurnexa.dto.product.ProductDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @JWTAnnotation
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDTO.Response create(@Valid @RequestBody ProductDTO.CreateRequest request,
                                      Authentication authentication) {
        requireAdmin(authentication);
        return productService.create(request);
    }

   // @JWTAnnotation
    @GetMapping
    public List<ProductDTO.Response> listActive() {
        return productService.listActive();
    }

    @JWTAnnotation
    @GetMapping("/all")
    public List<ProductDTO.Response> listAll(Authentication authentication) {
        requireAdmin(authentication);
        return productService.listAll();
    }

   // @JWTAnnotation
    @GetMapping("/{productId}")
    public ProductDTO.Response get(@PathVariable String productId) {
        return productService.get(productId);
    }

    @JWTAnnotation
    @GetMapping("/search")
    public List<ProductDTO.Response> searchByName(@RequestParam String name) {
        return productService.searchByName(name);
    }

    @JWTAnnotation
    @PatchMapping("/{productId}")
    public ProductDTO.Response update(@PathVariable String productId,
                                      @Valid @RequestBody ProductDTO.UpdateRequest request,
                                      Authentication authentication) {
        requireAdmin(authentication);
        return productService.update(productId, request);
    }

    @JWTAnnotation
    @PatchMapping("/{productId}/toggle")
    public ProductDTO.Response toggleActive(@PathVariable String productId,
                                            Authentication authentication) {
        requireAdmin(authentication);
        return productService.toggleActive(productId);
    }

    @JWTAnnotation
    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String productId, Authentication authentication) {
        requireAdmin(authentication);
        productService.delete(productId);
    }

    private static void requireAdmin(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }
}
