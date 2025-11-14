package g6.fashionFlex.service;

import g6.fashionFlex.entity.Order;
import g6.fashionFlex.entity.Order.OrderStatus;
import g6.fashionFlex.entity.Payment;
import g6.fashionFlex.entity.Payment.PaymentStatus;
import g6.fashionFlex.repository.CategoryRepository;
import g6.fashionFlex.repository.OrderRepository;
import g6.fashionFlex.repository.PaymentRepository;
import g6.fashionFlex.repository.ProductRepository;
import g6.fashionFlex.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminStatsService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Total counts
        stats.put("totalUsers", userRepository.count());
        stats.put("totalProducts", productRepository.count());
        stats.put("totalOrders", orderRepository.count());
        stats.put("totalCategories", categoryRepository.count());
        
        // Orders by status
        Map<String, Long> ordersByStatus = new HashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            ordersByStatus.put(status.name(), 
                orderRepository.findByStatus(status, 
                    org.springframework.data.domain.Pageable.unpaged()).getTotalElements());
        }
        stats.put("ordersByStatus", ordersByStatus);
        
        // Total revenue (from successful payments)
        List<Payment> successfulPayments = paymentRepository.findByStatus(PaymentStatus.success);
        BigDecimal totalRevenue = successfulPayments.stream()
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalRevenue", totalRevenue);
        
        return stats;
    }

    public Map<String, Object> getStatsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> stats = new HashMap<>();
        
        // Orders in date range
        List<Order> ordersInRange = orderRepository.findByOrderDateBetween(startDate, endDate);
        stats.put("ordersCount", ordersInRange.size());
        
        // Revenue in date range
        List<Payment> paymentsInRange = paymentRepository.findByPaymentDateBetween(startDate, endDate);
        BigDecimal revenue = paymentsInRange.stream()
            .filter(p -> p.getStatus() == PaymentStatus.success)
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("revenue", revenue);
        
        // Orders by status in range
        Map<String, Long> ordersByStatus = new HashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            long count = ordersInRange.stream()
                .filter(o -> o.getStatus() == status)
                .count();
            ordersByStatus.put(status.name(), count);
        }
        stats.put("ordersByStatus", ordersByStatus);
        
        return stats;
    }

    public Map<String, BigDecimal> getRevenueByDay(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, BigDecimal> revenueByDay = new HashMap<>();
        
        List<Payment> payments = paymentRepository.findByStatusAndPaymentDateBetween(
            PaymentStatus.success, startDate, endDate);
        
        payments.forEach(payment -> {
            String dateKey = payment.getPaymentDate().toLocalDate().toString();
            revenueByDay.merge(dateKey, payment.getAmount(), BigDecimal::add);
        });
        
        return revenueByDay;
    }

    public Map<String, BigDecimal> getRevenueByMonth(int year) {
        Map<String, BigDecimal> revenueByMonth = new HashMap<>();
        
        LocalDateTime startDate = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(year, 12, 31, 23, 59, 59);
        
        List<Payment> payments = paymentRepository.findByStatusAndPaymentDateBetween(
            PaymentStatus.success, startDate, endDate);
        
        payments.forEach(payment -> {
            String monthKey = String.format("%d-%02d", 
                payment.getPaymentDate().getYear(),
                payment.getPaymentDate().getMonthValue());
            revenueByMonth.merge(monthKey, payment.getAmount(), BigDecimal::add);
        });
        
        return revenueByMonth;
    }
}

