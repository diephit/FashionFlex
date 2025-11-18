package g6.fashionFlex.service;

import g6.fashionFlex.dto.OrderDTO;
import g6.fashionFlex.dto.OrderItemDTO;
import g6.fashionFlex.entity.Order;
import g6.fashionFlex.entity.OrderItem;
import g6.fashionFlex.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class AdminOrderService {

    @Autowired
    private OrderRepository orderRepository;

    public Page<OrderDTO> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::convertToDTO);
    }

    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        return convertToDTO(order);
    }

    public OrderDTO updateOrderStatus(Long id, Order.OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));

        order.setStatus(status);

        // Update timestamps based on status
        switch (status) {
            case SHIPPED:
                if (order.getShippedAt() == null) {
                    order.setShippedAt(LocalDateTime.now());
                }
                break;
            case DELIVERED:
                if (order.getDeliveredAt() == null) {
                    order.setDeliveredAt(LocalDateTime.now());
                }
                break;
            case CANCELLED:
                if (order.getCancelledAt() == null) {
                    order.setCancelledAt(LocalDateTime.now());
                }
                break;
            default:
                break;
        }

        Order updatedOrder = orderRepository.save(order);
        return convertToDTO(updatedOrder);
    }

    public OrderDTO updatePaymentStatus(Long id, Order.PaymentStatus paymentStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        order.setPaymentStatus(paymentStatus);
        Order updatedOrder = orderRepository.save(order);
        return convertToDTO(updatedOrder);
    }

    public OrderDTO updateTrackingNumber(Long id, String trackingNumber) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        order.setTrackingNumber(trackingNumber);
        Order updatedOrder = orderRepository.save(order);
        return convertToDTO(updatedOrder);
    }

    public Page<OrderDTO> getOrdersByStatus(Order.OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable).map(this::convertToDTO);
    }

    public Page<OrderDTO> searchOrders(String keyword, Pageable pageable) {
        return orderRepository.searchOrders(keyword, pageable).map(this::convertToDTO);
    }

    public List<OrderDTO> getRecentOrders() {
        return orderRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public long countOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.countByStatus(status);
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

    /**
     * Advanced search with multiple filters
     */
    public Page<OrderDTO> searchOrdersAdvanced(String keyword, Order.OrderStatus status,
                                                Order.PaymentStatus paymentStatus,
                                                LocalDateTime startDate, LocalDateTime endDate,
                                                Pageable pageable) {
        List<Order> allOrders = orderRepository.findAll();

        // Apply filters
        List<Order> filteredOrders = allOrders.stream()
                .filter(order -> {
                    // Keyword filter (order number, customer name, email)
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String lowerKeyword = keyword.toLowerCase();
                        boolean matchesOrderNumber = order.getOrderNumber().toLowerCase().contains(lowerKeyword);
                        boolean matchesName = order.getUser().getFullName().toLowerCase().contains(lowerKeyword);
                        boolean matchesEmail = order.getUser().getEmail().toLowerCase().contains(lowerKeyword);
                        if (!matchesOrderNumber && !matchesName && !matchesEmail) {
                            return false;
                        }
                    }

                    // Status filter
                    if (status != null && !order.getStatus().equals(status)) {
                        return false;
                    }

                    // Payment status filter
                    if (paymentStatus != null && !order.getPaymentStatus().equals(paymentStatus)) {
                        return false;
                    }

                    // Date range filter
                    if (startDate != null && order.getCreatedAt().isBefore(startDate)) {
                        return false;
                    }
                    if (endDate != null && order.getCreatedAt().isAfter(endDate)) {
                        return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());

        // Convert to Page (simple implementation)
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredOrders.size());
        List<OrderDTO> pageContent = filteredOrders.subList(start, end).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(
                pageContent, pageable, filteredOrders.size());
    }

    /**
     * Get order statistics for dashboard
     */
    public OrderStatistics getStatistics() {
        List<Order> allOrders = orderRepository.findAll();

        long totalOrders = allOrders.size();
        long pendingOrders = allOrders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.PENDING || o.getStatus() == Order.OrderStatus.CONFIRMED)
                .count();
        long shippedOrders = allOrders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.SHIPPED)
                .count();
        long deliveredOrders = allOrders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.DELIVERED)
                .count();
        long cancelledOrders = allOrders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.CANCELLED)
                .count();

        BigDecimal totalRevenue = allOrders.stream()
                .filter(o -> o.getPaymentStatus() == Order.PaymentStatus.PAID)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrderStatistics(totalOrders, pendingOrders, shippedOrders,
                                   deliveredOrders, cancelledOrders, totalRevenue);
    }

    /**
     * Bulk cancel orders
     */
    public int bulkCancel(List<Long> orderIds, String cancellationReason) {
        int count = 0;
        for (Long id : orderIds) {
            try {
                Order order = orderRepository.findById(id).orElse(null);
                if (order != null && order.getStatus() != Order.OrderStatus.CANCELLED
                        && order.getStatus() != Order.OrderStatus.DELIVERED) {
                    order.setStatus(Order.OrderStatus.CANCELLED);
                    order.setCancelledAt(LocalDateTime.now());
                    order.setCancellationReason(cancellationReason != null ? cancellationReason : "Bulk cancellation");
                    orderRepository.save(order);
                    count++;
                }
            } catch (Exception e) {
                log.error("Error cancelling order {}: {}", id, e.getMessage());
            }
        }
        log.info("Bulk cancelled {} orders", count);
        return count;
    }

    /**
     * Bulk mark as shipped
     */
    public int bulkMarkShipped(List<Long> orderIds) {
        int count = 0;
        for (Long id : orderIds) {
            try {
                Order order = orderRepository.findById(id).orElse(null);
                if (order != null && (order.getStatus() == Order.OrderStatus.PENDING
                        || order.getStatus() == Order.OrderStatus.CONFIRMED
                        || order.getStatus() == Order.OrderStatus.PROCESSING)) {
                    order.setStatus(Order.OrderStatus.SHIPPED);
                    if (order.getShippedAt() == null) {
                        order.setShippedAt(LocalDateTime.now());
                    }
                    orderRepository.save(order);
                    count++;
                }
            } catch (Exception e) {
                log.error("Error marking order {} as shipped: {}", id, e.getMessage());
            }
        }
        log.info("Bulk marked {} orders as shipped", count);
        return count;
    }

    /**
     * Bulk mark as delivered
     */
    public int bulkMarkDelivered(List<Long> orderIds) {
        int count = 0;
        for (Long id : orderIds) {
            try {
                Order order = orderRepository.findById(id).orElse(null);
                if (order != null && order.getStatus() == Order.OrderStatus.SHIPPED) {
                    order.setStatus(Order.OrderStatus.DELIVERED);
                    if (order.getDeliveredAt() == null) {
                        order.setDeliveredAt(LocalDateTime.now());
                    }
                    orderRepository.save(order);
                    count++;
                }
            } catch (Exception e) {
                log.error("Error marking order {} as delivered: {}", id, e.getMessage());
            }
        }
        log.info("Bulk marked {} orders as delivered", count);
        return count;
    }

    // Order Statistics DTO
    public static class OrderStatistics {
        private final long totalOrders;
        private final long pendingOrders;
        private final long shippedOrders;
        private final long deliveredOrders;
        private final long cancelledOrders;
        private final BigDecimal totalRevenue;

        public OrderStatistics(long totalOrders, long pendingOrders, long shippedOrders,
                              long deliveredOrders, long cancelledOrders, BigDecimal totalRevenue) {
            this.totalOrders = totalOrders;
            this.pendingOrders = pendingOrders;
            this.shippedOrders = shippedOrders;
            this.deliveredOrders = deliveredOrders;
            this.cancelledOrders = cancelledOrders;
            this.totalRevenue = totalRevenue;
        }

        public long getTotalOrders() { return totalOrders; }
        public long getPendingOrders() { return pendingOrders; }
        public long getShippedOrders() { return shippedOrders; }
        public long getDeliveredOrders() { return deliveredOrders; }
        public long getCancelledOrders() { return cancelledOrders; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
    }
}
