package g6.fashionFlex.service;

import g6.fashionFlex.entity.Order;
import g6.fashionFlex.entity.Order.OrderStatus;
import g6.fashionFlex.entity.Payment;
import g6.fashionFlex.entity.Payment.PaymentStatus;
import g6.fashionFlex.repository.OrderRepository;
import g6.fashionFlex.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AdminOrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    public Page<Order> searchOrders(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return orderRepository.findAll(pageable);
        }
        return orderRepository.searchOrders(keyword, pageable);
    }

    public Page<Order> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable);
    }

    public Optional<Order> getOrderById(Integer orderID) {
        return orderRepository.findById(orderID);
    }

    @Transactional
    public Order updateOrderStatus(Integer orderID, OrderStatus status) {
        Order order = orderRepository.findById(orderID)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        order.setStatus(status);
        return orderRepository.save(order);
    }

    @Transactional
    public Order updateTrackingNumber(Integer orderID, String trackingNumber) {
        Order order = orderRepository.findById(orderID)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        order.setTrackingNumber(trackingNumber);
        return orderRepository.save(order);
    }

    public List<Payment> getOrderPayments(Integer orderID) {
        return paymentRepository.findByOrderOrderID(orderID);
    }

    @Transactional
    public Payment updatePaymentStatus(Integer paymentID, PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentID)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        payment.setStatus(status);
        return paymentRepository.save(payment);
    }
}

