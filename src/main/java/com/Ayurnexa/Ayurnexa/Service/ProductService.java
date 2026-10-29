package com.Ayurnexa.Ayurnexa.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import com.Ayurnexa.Ayurnexa.dto.product.ProductDTO;
import com.Ayurnexa.Ayurnexa.model.product.Product;
import com.Ayurnexa.Ayurnexa.repository.OrderRepository;
import com.Ayurnexa.Ayurnexa.repository.ProductRepository;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public ProductService(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    public ProductDTO.Response create(ProductDTO.CreateRequest request) {
        Product product = new Product();
        apply(product, request);
        return toResponse(productRepository.save(product));
    }

    public List<ProductDTO.Response> listActive() {
        return productRepository.findByIsActive(true).stream()
                .map(ProductService::toResponse).toList();
    }

    public List<ProductDTO.Response> listAll() {
        return productRepository.findAll().stream().map(ProductService::toResponse).toList();
    }

    public ProductDTO.Response get(String productId) {
        return toResponse(findProduct(productId));
    }

    public List<ProductDTO.Response> searchByName(String name) {
        return productRepository.findByProductNameContainingIgnoreCase(name).stream()
                .map(ProductService::toResponse).toList();
    }

    public ProductDTO.Response update(String productId, ProductDTO.UpdateRequest request) {
        Product product = findProduct(productId);
        applyUpdate(product, request);
        return toResponse(productRepository.save(product));
    }

    public ProductDTO.Response toggleActive(String productId) {
        Product product = findProduct(productId);
        product.setIsActive(!product.isActive());
        return toResponse(productRepository.save(product));
    }

    public void delete(String productId) {
        Product product = findProduct(productId);
        if (orderRepository.existsByItemsProductId(productId)) {
            throw new IllegalStateException("Product is referenced by an order and cannot be deleted");
        }
        productRepository.delete(product);
    }

    private Product findProduct(String productId) {
        return productRepository.findByProductId(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found"));
    }

    private static void apply(Product product, ProductDTO.CreateRequest request) {
        product.setProductName(request.productName().trim());
        product.setProductDescription(request.productDescription().trim());
        product.setProductCategory(request.productCategory().trim());
        product.setProductWeight(request.productWeight().trim());
        product.setForm(request.form().trim());
        product.setProductImage(request.productImage());
        product.setIsActive(request.isActive());
        product.setProductPrice(request.productPrice());
    }

    private static void applyUpdate(Product product, ProductDTO.UpdateRequest request) {
        if (request.productName() != null) {
            product.setProductName(request.productName().trim());
        }
        if (request.productDescription() != null) {
            product.setProductDescription(request.productDescription().trim());
        }
        if (request.productCategory() != null) {
            product.setProductCategory(request.productCategory().trim());
        }
        if (request.productWeight() != null) {
            product.setProductWeight(request.productWeight().trim());
        }
        if (request.form() != null) {
            product.setForm(request.form().trim());
        }
        if (request.productImage() != null) {
            product.setProductImage(request.productImage());
        }
        if (request.isActive() != null) {
            product.setIsActive(request.isActive());
        }
        if (request.productPrice() != null) {
            product.setProductPrice(request.productPrice());
        }
    }

    private static ProductDTO.Response toResponse(Product p) {
        return new ProductDTO.Response(p.getProductId(), p.getProductName(), p.getProductDescription(),
                p.getProductCategory(), p.getProductWeight(), p.getForm(), p.getProductImage(),
                p.isActive(), p.getProductPrice(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
