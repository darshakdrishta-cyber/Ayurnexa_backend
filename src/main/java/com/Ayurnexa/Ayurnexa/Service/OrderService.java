package com.Ayurnexa.Ayurnexa.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import com.Ayurnexa.Ayurnexa.dto.order.OrderDTO;
import com.Ayurnexa.Ayurnexa.model.Payment.Payment;
import com.Ayurnexa.Ayurnexa.model.User.User;
import com.Ayurnexa.Ayurnexa.model.order.Order;
import com.Ayurnexa.Ayurnexa.model.order.OrderItem;
import com.Ayurnexa.Ayurnexa.model.product.Product;
import com.Ayurnexa.Ayurnexa.repository.OrderRepository;
import com.Ayurnexa.Ayurnexa.repository.PaymentRepository;
import com.Ayurnexa.Ayurnexa.repository.ProductRepository;
import com.Ayurnexa.Ayurnexa.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository,
                        PaymentRepository paymentRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public OrderDTO.Response create(String userId, OrderDTO.CreateRequest request) {
        Payment payment = paymentRepository.findById(request.paymentRecordId())
                .filter(candidate -> userId.equals(candidate.getUserId()))
                .orElseThrow(() -> new NoSuchElementException("Payment not found"));
        if (!PaymentService.COD.equalsIgnoreCase(payment.getPaymentMethod())
                && !PaymentService.SUCCESS.equalsIgnoreCase(payment.getPaymentStatus())) {
            throw new IllegalStateException("Order requires COD or a successfully completed payment");
        }
        if (orderRepository.findByPaymentId(payment.getId()).isPresent()) {
            throw new IllegalStateException("An order already exists for this payment");
        }

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (OrderDTO.ItemRequest itemRequest : request.items()) {
            if (itemRequest.quantity() < 1) {
                throw new IllegalArgumentException("Item quantity must be at least 1");
            }
            Product product = productRepository.findByProductId(itemRequest.productId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "Product not found: " + itemRequest.productId()));
            OrderItem item = new OrderItem();
            item.setProductId(product.getProductId());
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(product.getProductPrice());
            items.add(item);
            total = total.add(product.getProductPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
        }

        if (total.compareTo(payment.getamount()) != 0) {
            throw new IllegalArgumentException("Order total does not match the payment amount");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setPaymentId(payment.getId());
        order.setPaymentMethod(payment.getPaymentMethod());
        order.setPrice(total);
        order.setOrderStatus("CREATED");
        order.setStatus("CREATED");
        order.setOrderProgress("PLACED");
        order.setExpectedDeliveryDate(request.expectedDeliveryDate());
        order.setItems(items);
        Order savedOrder = orderRepository.save(order);
        payment.setOrderId(savedOrder.getOrderId());
        paymentRepository.save(payment);
        return toResponse(savedOrder, payment.getPaymentStatus());
    }

    public List<OrderDTO.Response> list(String userId) {
        return orderRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public OrderDTO.Response get(String userId, String orderId) {
        return toResponse(findOwnedActiveOrder(userId, orderId));
    }

    public OrderDTO.Response update(String userId, String orderId, OrderDTO.UpdateRequest request) {
        Order order = findOwnedOrder(userId, orderId);
        if (request.orderStatus() == null && request.orderProgress() == null
                && request.expectedDeliveryDate() == null && request.deliveryDate() == null
                && request.active() == null) {
            throw new IllegalArgumentException("At least one order field must be provided");
        }
        if (request.orderStatus() != null) {
            order.setOrderStatus(request.orderStatus().trim());
            order.setStatus(request.orderStatus().trim());
        }
        if (request.orderProgress() != null) order.setOrderProgress(request.orderProgress().trim());
        if (request.expectedDeliveryDate() != null) order.setExpectedDeliveryDate(request.expectedDeliveryDate());
        if (request.deliveryDate() != null) order.setDeliveryDate(request.deliveryDate());
        if (request.active() != null) order.setActive(request.active());

        Order saved = orderRepository.save(order);
        Payment payment = paymentRepository.findById(saved.getPaymentId()).orElse(null);
        return toResponse(saved, payment == null ? null : payment.getPaymentStatus());
    }

    public OrderDTO.Response getByOrderId(String orderId) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        return toResponse(order);
    }

    public OrderDTO.Response adminUpdateOrderStatus(String orderId, OrderDTO.UpdateRequest request) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        if (request.orderStatus() == null && request.orderProgress() == null
                && request.expectedDeliveryDate() == null && request.deliveryDate() == null
                && request.active() == null) {
            throw new IllegalArgumentException("At least one order field must be provided");
        }
        if (request.orderStatus() != null) {
            order.setOrderStatus(request.orderStatus().trim());
            order.setStatus(request.orderStatus().trim());
        }
        if (request.orderProgress() != null) order.setOrderProgress(request.orderProgress().trim());
        if (request.expectedDeliveryDate() != null) order.setExpectedDeliveryDate(request.expectedDeliveryDate());
        if (request.deliveryDate() != null) order.setDeliveryDate(request.deliveryDate());
        if (request.active() != null) order.setActive(request.active());

        Order saved = orderRepository.save(order);
        Payment payment = paymentRepository.findById(saved.getPaymentId()).orElse(null);
        return toResponse(saved, payment == null ? null : payment.getPaymentStatus());
    }

    public List<OrderDTO.AdminOrderResponse> listAllOrdersForAdmin() {
        return orderRepository.findAll().stream()
                .map(this::toAdminOrderResponse)
                .toList();
    }

    public OrderDTO.Response adminUpdateStatusOnly(String orderId, String status) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be empty");
        }
        order.setOrderStatus(status.trim());
        order.setStatus(status.trim());
        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    private OrderDTO.AdminOrderResponse toAdminOrderResponse(Order order) {
        User user = null;
        if (order.getUserId() != null) {
            user = userRepository.findByUserId(order.getUserId())
                    .or(() -> userRepository.findById(order.getUserId()))
                    .orElse(null);
        }

        OrderDTO.AdminUserSummary userSummary = user == null ? null : new OrderDTO.AdminUserSummary(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getAddress()
        );

        List<OrderDTO.ItemResponse> items = order.getItems() == null ? List.of() : order.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return new OrderDTO.AdminOrderResponse(
                order.getOrderId(),
                order.getPaymentMethod(),
                order.getOrderStatus(),
                order.getOrderProgress(),
                order.getPrice(),
                order.isActive(),
                order.getExpectedDeliveryDate(),
                order.getDeliveryDate(),
                items,
                userSummary,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private OrderDTO.ItemResponse toItemResponse(OrderItem item) {
        Product product = productRepository.findByProductId(item.getProductId()).orElse(null);
        OrderDTO.ProductSummary productSummary = product == null ? null : new OrderDTO.ProductSummary(
                product.getProductId(),
                product.getProductName(),
                product.getProductDescription(),
                product.getProductCategory(),
                product.getProductWeight(),
                product.getForm(),
                product.getProductImage(),
                product.getProductPrice()
        );
        return new OrderDTO.ItemResponse(
                item.getOrderItemId(),
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getRating(),
                item.getFeedback(),
                productSummary
        );
    }

    public void delete(String userId, String orderId) {
        Order order = findOwnedActiveOrder(userId, orderId);
        order.setActive(false);
        orderRepository.save(order);
    }

    private Order findOwnedOrder(String userId, String orderId) {
        return orderRepository.findByOrderId(orderId)
                .filter(order -> userId.equals(order.getUserId()))
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
    }

    private Order findOwnedActiveOrder(String userId, String orderId) {
        return orderRepository.findByOrderId(orderId)
                .filter(order -> userId.equals(order.getUserId()) && order.isActive())
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
    }

    private OrderDTO.Response toResponse(Order order) {
        return toResponse(order, null);
    }

    private OrderDTO.Response toResponse(Order order, String paymentStatus) {
        List<OrderDTO.ItemResponse> items = order.getItems() == null ? List.of() : order.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return new OrderDTO.Response(
                order.getOrderId(),
                order.getPaymentMethod(),
                order.getOrderStatus(),
                order.getOrderProgress(),
                order.getPrice(),
                order.getExpectedDeliveryDate(),
                order.getDeliveryDate(),
                items
        );
    }
}