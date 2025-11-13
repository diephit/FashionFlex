package g6.fashionFlex.service;

import g6.fashionFlex.dto.AdminStatsDTO;
import g6.fashionFlex.entity.Order;
import g6.fashionFlex.repository.CategoryRepository;
import g6.fashionFlex.repository.OrderRepository;
import g6.fashionFlex.repository.ProductRepository;
import g6.fashionFlex.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;

@Service
@Transactional(readOnly = true)
public class AdminStatsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private g6.fashionFlex.repository.BrandRepository brandRepository;

    public AdminStatsDTO getAdminStatistics() {
        AdminStatsDTO stats = new AdminStatsDTO();

        // Total counts
        stats.setTotalUsers(userRepository.count());
        stats.setTotalProducts(productRepository.count());
        stats.setTotalOrders(orderRepository.count());
        stats.setTotalCategories(categoryRepository.count());
        stats.setTotalBrands(brandRepository.count());

        // Revenue calculations
        BigDecimal totalRevenue = orderRepository.getTotalRevenue();
        stats.setTotalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO);

        // Today's revenue
        LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        BigDecimal todayRevenue = orderRepository.getRevenueByDateRange(startOfToday, endOfToday);
        stats.setTodayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO);

        // This month's revenue
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(LocalTime.MAX);
        BigDecimal monthRevenue = orderRepository.getRevenueByDateRange(startOfMonth, endOfMonth);
        stats.setMonthRevenue(monthRevenue != null ? monthRevenue : BigDecimal.ZERO);

        // Order status counts
        stats.setPendingOrders(orderRepository.countByStatus(Order.OrderStatus.PENDING));
        stats.setProcessingOrders(orderRepository.countByStatus(Order.OrderStatus.PROCESSING));
        stats.setShippedOrders(orderRepository.countByStatus(Order.OrderStatus.SHIPPED));
        stats.setDeliveredOrders(orderRepository.countByStatus(Order.OrderStatus.DELIVERED));
        stats.setCancelledOrders(orderRepository.countByStatus(Order.OrderStatus.CANCELLED));

        // Stock status
        long lowStockCount = productRepository.findAll().stream()
                .filter(p -> p.getStock() <= 10 && p.getStock() > 0)
                .count();
        stats.setLowStockProducts(lowStockCount);

        long outOfStockCount = productRepository.findAll().stream()
                .filter(p -> p.getStock() == 0)
                .count();
        stats.setOutOfStockProducts(outOfStockCount);

        return stats;
    }

    public BigDecimal getRevenueByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal revenue = orderRepository.getRevenueByDateRange(startDate, endDate);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    public long getOrderCountByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.countOrdersByDateRange(startDate, endDate);
    }

    public BigDecimal getTotalRevenue() {
        BigDecimal revenue = orderRepository.getTotalRevenue();
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    public long getTotalUsers() {
        return userRepository.count();
    }

    public long getTotalProducts() {
        return productRepository.count();
    }

    public long getTotalOrders() {
        return orderRepository.count();
    }

    public long getTotalCategories() {
        return categoryRepository.count();
    }

    public long getTotalBrands() {
        return brandRepository.count();
    }
}
