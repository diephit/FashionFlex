package g6.fashionFlex.service;

import g6.fashionFlex.entity.Order;
import g6.fashionFlex.entity.Order.OrderStatus;
import g6.fashionFlex.entity.Payment;
import g6.fashionFlex.repository.OrderRepository;
import g6.fashionFlex.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    public Page<Order> getOrdersByDateRange(LocalDateTime startDate,
                                            LocalDateTime endDate,
                                            Pageable pageable) {
        List<Order> orders = orderRepository.findByOrderDateBetween(startDate, endDate);
        return new PageImpl<>(orders, pageable, orders.size());
    }

    public Page<Order> getOrdersByStatusAndDateRange(OrderStatus status,
                                                     LocalDateTime startDate,
                                                     LocalDateTime endDate,
                                                     Pageable pageable) {
        List<Order> orders = orderRepository.findByStatusAndOrderDateBetween(status, startDate, endDate);
        return new PageImpl<>(orders, pageable, orders.size());
    }

    public Optional<Order> getOrderById(Integer orderID) {
        return orderRepository.findById(orderID);
    }

    public List<Payment> getOrderPayments(Integer orderID) {
        return paymentRepository.findByOrderOrderID(orderID);
    }

    public long countAllOrders() {
        return orderRepository.count();
    }

    public long countOrdersByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }

    public long countPendingOrders() {
        return countOrdersByStatus(OrderStatus.pending);
    }

    public long countPaidOrders() {
        return countOrdersByStatus(OrderStatus.paid);
    }

    public long countCompletedOrders() {
        return countOrdersByStatus(OrderStatus.completed);
    }

    public long countCanceledOrders() {
        return countOrdersByStatus(OrderStatus.canceled);
    }
}