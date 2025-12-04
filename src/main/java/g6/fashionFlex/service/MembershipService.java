package g6.fashionFlex.service;

import g6.fashionFlex.entity.Customer;
import g6.fashionFlex.entity.MembershipHistory;
import g6.fashionFlex.entity.MembershipLevel;
import g6.fashionFlex.entity.Order;
import g6.fashionFlex.repository.CustomerRepository;
import g6.fashionFlex.repository.MembershipHistoryRepository;
import g6.fashionFlex.repository.MembershipLevelRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class MembershipService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MembershipLevelRepository membershipLevelRepository;

    @Autowired
    private MembershipHistoryRepository membershipHistoryRepository;

    /**
     * Process membership rewards when an order is completed
     * - Add loyalty points based on order amount and bonus rate
     * - Update total spent
     * - Check and upgrade membership level if eligible
     */
    @Transactional
    public void processOrderRewards(Order order) {
        if (order.getCustomer() == null) {
            return; // Guest order, no membership rewards
        }

        Customer customer = customerRepository.findById(order.getCustomer().getCustomerID())
                .orElse(null);
        if (customer == null) {
            return;
        }

        // Get current membership level
        MembershipLevel currentLevel = customer.getLevel();
        if (currentLevel == null) {
            // Set default level (Bronze) if not set
            Optional<MembershipLevel> bronzeLevel = membershipLevelRepository.findByLevelName("Bronze");
            if (bronzeLevel.isPresent()) {
                currentLevel = bronzeLevel.get();
                customer.setLevel(currentLevel);
            } else {
                return; // No membership levels configured
            }
        }

        // Calculate loyalty points: orderAmount * bonusRate
        BigDecimal orderAmount = BigDecimal.valueOf(order.getTotalAmount());
        BigDecimal bonusRate = currentLevel.getBonusRate();
        int pointsEarned = orderAmount.multiply(bonusRate)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        // Update loyalty points
        int currentPoints = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
        customer.setLoyaltyPoints(currentPoints + pointsEarned);

        // Update total spent
        BigDecimal currentTotalSpent = customer.getTotalSpent() != null ? customer.getTotalSpent() : BigDecimal.ZERO;
        BigDecimal newTotalSpent = currentTotalSpent.add(orderAmount);
        customer.setTotalSpent(newTotalSpent);

        // Check and upgrade membership level
        upgradeMembershipLevel(customer, currentLevel);

        customerRepository.save(customer);
    }

    /**
     * Check if customer is eligible for a higher membership level and upgrade if so
     */
    @Transactional
    public void upgradeMembershipLevel(Customer customer, MembershipLevel currentLevel) {
        BigDecimal totalSpent = customer.getTotalSpent() != null ? customer.getTotalSpent() : BigDecimal.ZERO;

        // Find all eligible levels (where minSpent <= totalSpent)
        List<MembershipLevel> eligibleLevels = membershipLevelRepository.findEligibleLevels(totalSpent);

        if (eligibleLevels.isEmpty()) {
            return;
        }

        // Get the highest eligible level
        MembershipLevel highestEligibleLevel = eligibleLevels.get(0);

        // Check if customer should be upgraded
        if (currentLevel == null || 
            highestEligibleLevel.getMinSpent().compareTo(currentLevel.getMinSpent()) > 0) {
            
            // Upgrade to new level
            MembershipLevel oldLevel = currentLevel;
            customer.setLevel(highestEligibleLevel);

            // Record in membership history
            MembershipHistory history = new MembershipHistory();
            history.setCustomer(customer);
            history.setOldLevel(oldLevel);
            history.setNewLevel(highestEligibleLevel);
            history.setNote("Auto-upgraded based on total spent: " + totalSpent);

            membershipHistoryRepository.save(history);
        }
    }

    /**
     * Get the next membership level the customer can achieve
     */
    public MembershipLevel getNextLevel(Customer customer) {
        // Get all levels ordered by minSpent
        List<MembershipLevel> allLevels = membershipLevelRepository.findAllOrderByMinSpentAsc();
        
        MembershipLevel currentLevel = customer.getLevel();
        if (currentLevel == null) {
            return allLevels.size() > 1 ? allLevels.get(1) : null;
        }

        // Find next level
        for (MembershipLevel level : allLevels) {
            if (level.getMinSpent().compareTo(currentLevel.getMinSpent()) > 0) {
                return level;
            }
        }

        return null; // Already at highest level
    }

    /**
     * Calculate progress percentage to next level
     */
    public double getProgressToNextLevel(Customer customer) {
        MembershipLevel currentLevel = customer.getLevel();
        MembershipLevel nextLevel = getNextLevel(customer);

        if (currentLevel == null || nextLevel == null) {
            return 100.0; // Already at highest level or no levels configured
        }

        BigDecimal totalSpent = customer.getTotalSpent() != null ? customer.getTotalSpent() : BigDecimal.ZERO;
        BigDecimal currentMinSpent = currentLevel.getMinSpent();
        BigDecimal nextMinSpent = nextLevel.getMinSpent();

        if (nextMinSpent.compareTo(currentMinSpent) <= 0) {
            return 100.0;
        }

        BigDecimal progress = totalSpent.subtract(currentMinSpent);
        BigDecimal range = nextMinSpent.subtract(currentMinSpent);

        double percentage = progress.divide(range, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();

        return Math.max(0, Math.min(100, percentage));
    }

    /**
     * Get discount rate for customer's current membership level
     */
    public BigDecimal getDiscountRate(Customer customer) {
        if (customer == null || customer.getLevel() == null) {
            return BigDecimal.ZERO;
        }
        return customer.getLevel().getDiscountRate();
    }

    /**
     * Calculate discounted price
     */
    public BigDecimal applyDiscount(BigDecimal originalPrice, Customer customer) {
        BigDecimal discountRate = getDiscountRate(customer);
        if (discountRate.compareTo(BigDecimal.ZERO) <= 0) {
            return originalPrice;
        }

        BigDecimal discount = originalPrice.multiply(discountRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return originalPrice.subtract(discount);
    }
}

