package g6.fashionFlex.service;

import g6.fashionFlex.dto.ShippingRateDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shipping service with fake data for demonstration
 * In production, this would integrate with real shipping providers
 */
@Service
public class ShippingService {

    // Fake shipping rates by country
    private static final Map<String, BigDecimal> BASE_RATES = Map.of(
            "US", new BigDecimal("5.00"),
            "CA", new BigDecimal("8.00"),
            "GB", new BigDecimal("10.00"),
            "AU", new BigDecimal("12.00"),
            "DE", new BigDecimal("9.00"),
            "FR", new BigDecimal("9.00"),
            "JP", new BigDecimal("15.00"),
            "VN", new BigDecimal("3.00")
    );

    // State/region multipliers for US
    private static final Map<String, BigDecimal> STATE_MULTIPLIERS = Map.of(
            "AK", new BigDecimal("2.0"), // Alaska
            "HI", new BigDecimal("2.0"), // Hawaii
            "CA", new BigDecimal("1.2"), // California
            "NY", new BigDecimal("1.1"), // New York
            "TX", new BigDecimal("1.0"), // Texas
            "FL", new BigDecimal("1.0")  // Florida
    );

    /**
     * Calculate shipping rates for a destination
     */
    public List<ShippingRateDTO> calculateShippingRates(String country, String state, String postcode) {
        List<ShippingRateDTO> rates = new ArrayList<>();

        BigDecimal baseRate = BASE_RATES.getOrDefault(country.toUpperCase(), new BigDecimal("10.00"));

        // Apply state multiplier for US
        if ("US".equalsIgnoreCase(country) && state != null) {
            BigDecimal multiplier = STATE_MULTIPLIERS.getOrDefault(state.toUpperCase(), BigDecimal.ONE);
            baseRate = baseRate.multiply(multiplier);
        }

        // Standard Shipping (5-7 business days)
        ShippingRateDTO standard = new ShippingRateDTO(
                "Standard Shipping",
                "Delivery in 5-7 business days",
                baseRate,
                "5-7 business days"
        );
        rates.add(standard);

        // Express Shipping (2-3 business days) - 2x standard rate
        ShippingRateDTO express = new ShippingRateDTO(
                "Express Shipping",
                "Delivery in 2-3 business days",
                baseRate.multiply(new BigDecimal("2.0")),
                "2-3 business days"
        );
        rates.add(express);

        // Overnight Shipping (1 business day) - 3x standard rate
        ShippingRateDTO overnight = new ShippingRateDTO(
                "Overnight Shipping",
                "Next business day delivery",
                baseRate.multiply(new BigDecimal("3.0")),
                "1 business day"
        );
        rates.add(overnight);

        // Free shipping for domestic orders (example: Vietnam)
        if ("VN".equalsIgnoreCase(country)) {
            ShippingRateDTO free = new ShippingRateDTO(
                    "Free Shipping",
                    "Free standard shipping within Vietnam (7-10 days)",
                    BigDecimal.ZERO,
                    "7-10 business days"
            );
            rates.add(0, free); // Add at the beginning
        }

        return rates;
    }

    /**
     * Calculate single shipping cost based on method
     */
    public BigDecimal calculateShippingCost(String country, String state, String method) {
        List<ShippingRateDTO> rates = calculateShippingRates(country, state, null);

        return rates.stream()
                .filter(rate -> rate.getMethod().equalsIgnoreCase(method))
                .map(ShippingRateDTO::getCost)
                .findFirst()
                .orElse(new BigDecimal("10.00")); // Default rate
    }

    /**
     * Get estimated delivery date based on shipping method
     */
    public String getEstimatedDelivery(String method) {
        return switch (method.toLowerCase()) {
            case "overnight shipping" -> "1 business day";
            case "express shipping" -> "2-3 business days";
            case "standard shipping" -> "5-7 business days";
            case "free shipping" -> "7-10 business days";
            default -> "5-7 business days";
        };
    }

    /**
     * Validate shipping address
     */
    public boolean validateShippingAddress(String country, String state, String postcode) {
        if (country == null || country.trim().isEmpty()) {
            return false;
        }

        // Basic validation - in production, would use real address validation API
        if ("US".equalsIgnoreCase(country)) {
            // US zip code should be 5 or 9 digits
            return postcode != null && postcode.matches("\\d{5}(-\\d{4})?");
        }

        // For other countries, just check postcode is not empty
        return postcode != null && !postcode.trim().isEmpty();
    }

    /**
     * Check if free shipping is available
     */
    public boolean isFreeShippingAvailable(String country, BigDecimal orderTotal) {
        // Free shipping for Vietnam
        if ("VN".equalsIgnoreCase(country)) {
            return true;
        }

        // Free shipping for orders over $100 in US
        if ("US".equalsIgnoreCase(country) && orderTotal.compareTo(new BigDecimal("100.00")) >= 0) {
            return true;
        }

        return false;
    }

    /**
     * Get supported countries
     */
    public List<String> getSupportedCountries() {
        return new ArrayList<>(BASE_RATES.keySet());
    }

    /**
     * Get tax rate for location (fake data)
     */
    public BigDecimal getTaxRate(String country, String state) {
        if ("US".equalsIgnoreCase(country) && state != null) {
            // Fake US state tax rates
            return switch (state.toUpperCase()) {
                case "CA" -> new BigDecimal("0.0725"); // 7.25%
                case "NY" -> new BigDecimal("0.04");   // 4%
                case "TX" -> new BigDecimal("0.0625"); // 6.25%
                case "FL" -> new BigDecimal("0.06");   // 6%
                default -> new BigDecimal("0.05");     // 5% default
            };
        }

        // Other countries
        return switch (country.toUpperCase()) {
            case "VN" -> new BigDecimal("0.10"); // 10% VAT
            case "GB" -> new BigDecimal("0.20"); // 20% VAT
            case "DE", "FR" -> new BigDecimal("0.19"); // 19% VAT
            case "CA" -> new BigDecimal("0.05"); // 5% GST
            case "AU" -> new BigDecimal("0.10"); // 10% GST
            case "JP" -> new BigDecimal("0.10"); // 10% consumption tax
            default -> BigDecimal.ZERO;
        };
    }

    /**
     * Calculate tax amount
     */
    public BigDecimal calculateTax(BigDecimal subtotal, String country, String state) {
        BigDecimal taxRate = getTaxRate(country, state);
        return subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
    }
}
