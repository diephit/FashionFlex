package g6.fashionFlex.service;

import g6.fashionFlex.dto.OrderDTO;
import g6.fashionFlex.dto.OrderItemDTO;
import g6.fashionFlex.entity.Order;
import g6.fashionFlex.entity.OrderItem;
import g6.fashionFlex.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
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
}
