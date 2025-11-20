package g6.fashionFlex.service.impl;

import g6.fashionFlex.dto.*;
import g6.fashionFlex.entity.*;
import g6.fashionFlex.exception.ResourceNotFoundException;
import g6.fashionFlex.repository.*;
import g6.fashionFlex.service.EmailService;
import g6.fashionFlex.service.OrderService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository; // Keep for fetching cart
    private final EmailService emailService;

    @Override
    public OrderConfirmationDTO placeOrder(PlaceOrderRequest request) {
        log.info("=== PLACE ORDER STARTED === User ID: {}, Time: {}",
                request.getUserId(), java.time.LocalDateTime.now());

        // 1. Get user
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        log.info("User found: {}", user.getEmail());

        // 2. Get cart with eager loading to avoid lazy loading issues
        Cart cart = cartRepository.findById(request.getCartId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        log.info("Cart found with {} items", cart.getItems().size());

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot place order with empty cart");
        }

        // 3. Create order
        log.info("Creating new order for user: {}", user.getEmail());
        Order order = new Order();
        order.setUser(user);

        // Set payment method
        try {
            order.setPaymentMethod(Order.PaymentMethod.valueOf(request.getPaymentMethod()));
            log.info("Payment method set: {}", request.getPaymentMethod());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid payment method: " + request.getPaymentMethod());
        }

        // For COD, payment status is PENDING
        order.setPaymentStatus(Order.PaymentStatus.PENDING);
        order.setStatus(Order.OrderStatus.PENDING);

        // Set shipping information
        order.setShippingName(request.getShippingName());
        order.setShippingPhone(request.getShippingPhone());
        order.setShippingAddress(request.getShippingAddress());
        order.setShippingCity(request.getShippingCity());
        order.setShippingState(request.getShippingState());
        order.setShippingZipCode(request.getShippingZipCode());
        order.setShippingCountry(request.getShippingCountry());

        // Set order totals
        order.setSubtotal(request.getSubtotal());
        order.setShippingCost(request.getShippingCost());
        order.setTax(request.getTax());
        order.setDiscount(request.getDiscount());
        order.setTotalAmount(request.getTotalAmount());

        // Set notes
        order.setNotes(request.getNotes());
        log.info("Order details set - Total: {}", request.getTotalAmount());

        // 4. Create order items from cart items and reduce stock
        List<CartItem> cartItems = new ArrayList<>(cart.getItems());
        log.info("Creating order items from {} cart items", cartItems.size());
        List<Product> productsToUpdate = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found during order creation"));

            log.debug("Processing cart item: product={}, quantity={}, stock={}",
                     product.getName(), cartItem.getQuantity(), product.getStock());

            // Double-check stock
            if (product.getStock() < cartItem.getQuantity()) {
                log.error("Insufficient stock for product: {} (need: {}, available: {})",
                         product.getName(), cartItem.getQuantity(), product.getStock());
                throw new IllegalStateException("While placing your order, " + product.getName() + " became unavailable.");
            }

            // Create order item
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getPrice());
            orderItem.setDiscountPrice(cartItem.getDiscountPrice());
            orderItem.setSize(cartItem.getSize());
            orderItem.setColor(cartItem.getColor());

            order.addOrderItem(orderItem);
            log.debug("Order item added for product: {}", product.getName());

            // 5. Reduce product stock
            int newStock = product.getStock() - cartItem.getQuantity();
            product.setStock(newStock);
            productsToUpdate.add(product);
            log.debug("Product stock will be reduced: {} (new stock: {})", product.getName(), newStock);
        }
        log.info("All order items created successfully");

        // 6. Save all changes in one transaction
        try {
            log.info("Saving order with {} items", order.getOrderItems().size());

            // Save product stock updates
            log.info("Updating stock for {} products", productsToUpdate.size());
            productRepository.saveAll(productsToUpdate);

            // Save order
            order = orderRepository.save(order);
            log.info("Order saved successfully with ID: {} and number: {}", order.getId(), order.getOrderNumber());

        } catch (Exception e) {
            log.error("Failed to save order - Full exception: ", e);
            throw new RuntimeException("Failed to save order: " + e.getMessage(), e);
        }

        // 7. Build confirmation before async operations
        OrderConfirmationDTO confirmation = buildOrderConfirmation(order);
        log.info("Order confirmation built");

        // 8. Send confirmation email asynchronously (outside transaction)
        try {
            log.info("Attempting to send confirmation email");
            sendOrderConfirmationEmail(order, user);
            log.info("Confirmation email sent successfully");
        } catch (Exception e) {
            log.error("Failed to send confirmation email: {}", e.getMessage());
            // Don't fail order if email fails
        }

        log.info("=== PLACE ORDER COMPLETED === Order ID: {}, Order Number: {}",
                order.getId(), order.getOrderNumber());

        // 9. Return order confirmation
        return confirmation;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByUserIdAndStatus(Long userId, Order.OrderStatus status) {
        if (status == null) {
            return getOrdersByUserId(userId);
        }
        return orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderDetails(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Verify ownership
        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to this user");
        }

        return convertToDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderTrackingDTO trackOrder(String orderNumber, Long userId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));

        // Verify ownership
        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to this user");
        }

        return buildOrderTracking(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderTrackingDTO trackOrderByEmail(String orderNumber, String email) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));

        // Verify email matches
        if (!order.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("Email does not match order");
        }

        return buildOrderTracking(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderConfirmationDTO getOrderConfirmation(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Verify ownership
        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to this user");
        }

        return buildOrderConfirmation(order);
    }

    @Override
    public OrderDTO cancelOrder(Long orderId, Long userId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Verify ownership
        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to this user");
        }

        // Only PENDING orders can be cancelled
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be cancelled. Current status: " + order.getStatus());
        }

        // Restore stock
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        // Update order status
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason(reason);

        order = orderRepository.save(order);

        return convertToDTO(order);
    }

    // ==================== Helper Methods ====================

    private OrderConfirmationDTO buildOrderConfirmation(Order order) {
        OrderConfirmationDTO dto = new OrderConfirmationDTO();
        dto.setOrderId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setOrderDate(order.getCreatedAt());

        // Customer info
        dto.setCustomerName(order.getUser().getFullName());
        dto.setCustomerEmail(order.getUser().getEmail());

        // Shipping info
        dto.setShippingAddress(formatAddress(order));
        dto.setShippingPhone(order.getShippingPhone());
        dto.setEstimatedDelivery(calculateEstimatedDelivery(order));

        // Order items
        List<OrderItemDTO> items = order.getOrderItems().stream()
                .map(this::convertOrderItemToDTO)
                .collect(Collectors.toList());
        dto.setOrderItems(items);
        dto.setTotalItems(order.getOrderItems().stream()
                .mapToInt(OrderItem::getQuantity)
                .sum());

        // Payment info
        dto.setPaymentMethod(order.getPaymentMethod().name());
        dto.setPaymentStatus(order.getPaymentStatus().name());

        // Totals
        dto.setSubtotal(order.getSubtotal());
        dto.setShippingCost(order.getShippingCost());
        dto.setTax(order.getTax());
        dto.setDiscount(order.getDiscount());
        dto.setTotalAmount(order.getTotalAmount());

        // Status
        dto.setOrderStatus(order.getStatus().name());

        // Messages
        dto.setSuccessMessage("Your order has been placed successfully!");
        dto.setNextSteps("We'll send you an email confirmation and updates about your order.");

        return dto;
    }

    private OrderTrackingDTO buildOrderTracking(Order order) {
        OrderTrackingDTO dto = new OrderTrackingDTO();
        dto.setOrderNumber(order.getOrderNumber());
        dto.setCurrentStatus(order.getStatus().name());
        dto.setEstimatedDelivery(calculateEstimatedDelivery(order));
        dto.setShippingMethod("Standard Shipping"); // Could be enhanced to store this
        dto.setTrackingNumber(order.getTrackingNumber());
        dto.setShippingAddress(formatAddress(order));

        // Build timeline
        dto.setTimeline(buildTimeline(order));

        return dto;
    }

    private List<OrderTrackingDTO.TrackingEvent> buildTimeline(Order order) {
        List<OrderTrackingDTO.TrackingEvent> timeline = new ArrayList<>();

        // Order Placed
        timeline.add(createEvent(
                "PENDING",
                "Order Placed",
                "Your order has been successfully placed and we've received your payment.",
                order.getCreatedAt(),
                true,
                order.getStatus() == Order.OrderStatus.PENDING
        ));

        // Order Confirmed
        timeline.add(createEvent(
                "CONFIRMED",
                "Order Confirmed",
                "We've confirmed your order details and it's ready for processing.",
                order.getStatus().ordinal() >= Order.OrderStatus.CONFIRMED.ordinal() ? order.getCreatedAt().plusHours(1) : null,
                order.getStatus().ordinal() >= Order.OrderStatus.CONFIRMED.ordinal(),
                order.getStatus() == Order.OrderStatus.CONFIRMED
        ));

        // Processing
        timeline.add(createEvent(
                "PROCESSING",
                "Processing",
                "Your order is currently being prepared for shipment.",
                order.getStatus().ordinal() >= Order.OrderStatus.PROCESSING.ordinal() ? order.getCreatedAt().plusHours(2) : null,
                order.getStatus().ordinal() >= Order.OrderStatus.PROCESSING.ordinal(),
                order.getStatus() == Order.OrderStatus.PROCESSING
        ));

        // Shipped
        timeline.add(createEvent(
                "SHIPPED",
                "Shipped",
                "Your order has been shipped" + (order.getTrackingNumber() != null ? " (Tracking: " + order.getTrackingNumber() + ")" : ""),
                order.getShippedAt(),
                order.getStatus().ordinal() >= Order.OrderStatus.SHIPPED.ordinal(),
                order.getStatus() == Order.OrderStatus.SHIPPED
        ));

        // Delivered
        timeline.add(createEvent(
                "DELIVERED",
                "Delivered",
                "Your order has been delivered successfully.",
                order.getDeliveredAt(),
                order.getStatus() == Order.OrderStatus.DELIVERED,
                false
        ));

        return timeline;
    }

    private OrderTrackingDTO.TrackingEvent createEvent(String status, String title, String description,
                                                        LocalDateTime timestamp, boolean completed, boolean active) {
        OrderTrackingDTO.TrackingEvent event = new OrderTrackingDTO.TrackingEvent();
        event.setStatus(status);
        event.setTitle(title);
        event.setDescription(description);
        event.setTimestamp(timestamp);
        event.setCompleted(completed);
        event.setActive(active);
        return event;
    }

    private String calculateEstimatedDelivery(Order order) {
        // Simple estimation: 5-7 business days from order date
        LocalDateTime estimatedDate = order.getCreatedAt().plusDays(7);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
        return estimatedDate.format(formatter);
    }

    private String formatAddress(Order order) {
        return String.format("%s, %s, %s %s, %s",
                order.getShippingAddress(),
                order.getShippingCity(),
                order.getShippingState(),
                order.getShippingZipCode(),
                order.getShippingCountry()
        );
    }

    private void sendOrderConfirmationEmail(Order order, User user) throws MessagingException {
        String subject = "Order Confirmation - " + order.getOrderNumber();
        String htmlContent = buildOrderConfirmationEmailHtml(order, user);

        // Use existing email service (adapt if needed)
        emailService.sendContactEmail(
                "FashionFlex",
                "noreply@fashionflex.com",
                htmlContent,
                user.getEmail()
        );
    }

    private String buildOrderConfirmationEmailHtml(Order order, User user) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><style>");
        html.append("body{font-family:Arial,sans-serif;line-height:1.6;color:#333;}");
        html.append(".container{max-width:600px;margin:0 auto;padding:20px;background-color:#f9f9f9;}");
        html.append(".header{background-color:#667eea;color:white;padding:20px;text-align:center;}");
        html.append(".content{background-color:white;padding:30px;margin-top:20px;border-radius:5px;}");
        html.append(".order-summary{background-color:#f5f5f5;padding:15px;margin:20px 0;border-radius:5px;}");
        html.append(".footer{text-align:center;margin-top:30px;color:#999;}");
        html.append("</style></head><body><div class='container'>");

        html.append("<div class='header'><h1>Order Confirmation</h1></div>");
        html.append("<div class='content'>");
        html.append("<p>Hi ").append(user.getFullName()).append(",</p>");
        html.append("<p>Thank you for your order! We're getting it ready for shipment.</p>");

        html.append("<div class='order-summary'>");
        html.append("<h3>Order #").append(order.getOrderNumber()).append("</h3>");
        html.append("<p><strong>Order Date:</strong> ").append(order.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))).append("</p>");
        html.append("<p><strong>Total Amount:</strong> $").append(order.getTotalAmount()).append("</p>");
        html.append("<p><strong>Payment Method:</strong> ").append(order.getPaymentMethod()).append("</p>");
        html.append("</div>");

        html.append("<p>We'll send you another email when your order ships.</p>");
        html.append("<p>Thank you for shopping with FashionFlex!</p>");
        html.append("</div>");

        html.append("<div class='footer'><p>&copy; 2024 FashionFlex. All rights reserved.</p></div>");
        html.append("</div></body></html>");

        return html.toString();
    }

    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setUserId(order.getUser().getId());
        dto.setUserEmail(order.getUser().getEmail());
        dto.setUserName(order.getUser().getFullName());

        List<OrderItemDTO> orderItemDTOs = order.getOrderItems().stream()
                .map(this::convertOrderItemToDTO)
                .collect(Collectors.toList());
        dto.setOrderItems(orderItemDTOs);

        dto.setSubtotal(order.getSubtotal());
        dto.setShippingCost(order.getShippingCost());
        dto.setTax(order.getTax());
        dto.setDiscount(order.getDiscount());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus().name());
        dto.setPaymentMethod(order.getPaymentMethod().name());
        dto.setPaymentStatus(order.getPaymentStatus().name());
        dto.setShippingName(order.getShippingName());
        dto.setShippingPhone(order.getShippingPhone());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setShippingCity(order.getShippingCity());
        dto.setShippingState(order.getShippingState());
        dto.setShippingZipCode(order.getShippingZipCode());
        dto.setShippingCountry(order.getShippingCountry());
        dto.setNotes(order.getNotes());
        dto.setTrackingNumber(order.getTrackingNumber());
        dto.setShippedAt(order.getShippedAt());
        dto.setDeliveredAt(order.getDeliveredAt());
        dto.setCancelledAt(order.getCancelledAt());
        dto.setCancellationReason(order.getCancellationReason());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        return dto;
    }

    @Override
    public long getUserOrderCount(Long userId) {
        return orderRepository.countByUserId(userId);
    }

    private OrderItemDTO convertOrderItemToDTO(OrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(item.getId());
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProduct().getName());
        dto.setProductImageUrl(item.getProduct().getImageUrl());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());
        dto.setDiscountPrice(item.getDiscountPrice());
        dto.setSize(item.getSize());
        dto.setColor(item.getColor());
        dto.setSubtotal(item.getSubtotal());
        return dto;
    }
}
