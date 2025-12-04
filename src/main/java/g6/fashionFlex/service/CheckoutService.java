package g6.fashionFlex.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import g6.fashionFlex.entity.*;
import g6.fashionFlex.entity.Cart.CartStatus;
import g6.fashionFlex.entity.Order.OrderStatus;
import g6.fashionFlex.entity.Payment.PaymentMethod;
import g6.fashionFlex.entity.Payment.PaymentStatus;
import g6.fashionFlex.repository.CartItemRepository;
import g6.fashionFlex.repository.CartRepository;
import g6.fashionFlex.repository.CustomerRepository;
import g6.fashionFlex.repository.OrderRepository;
import g6.fashionFlex.repository.PaymentRepository;
import g6.fashionFlex.repository.ProductRepository;
import g6.fashionFlex.repository.ProductVariantRepository;
import g6.fashionFlex.repository.StockRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CheckoutService {

    private static final double USD_TO_VND_RATE = 25000.0;
    private static final BigDecimal SHIPPING_FEE_USD = BigDecimal.ONE;

    @Autowired
    private CartService cartService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CustomerRepository customerRepository;


    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private StockRepository stockRepository;

    public CheckoutTotals calculateTotals(Cart cart) {
        Customer customer = cart != null ? cart.getCustomer() : null;
        return calculateTotals(cart, customer);
    }

    public CheckoutTotals calculateTotals(Cart cart, Customer discountCustomer) {
        List<CartItem> items = cartService.getItems(cart);
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            ProductVariant variant = item.getVariant();
            BigDecimal price = variant.getPrice() != null ? variant.getPrice() : BigDecimal.ZERO;
            subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        BigDecimal discountRate = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;

        Customer customerForDiscount = discountCustomer;
        if (customerForDiscount == null && cart != null) {
            customerForDiscount = cart.getCustomer();
        }

        if (customerForDiscount != null) {
            discountRate = membershipService.getDiscountRate(customerForDiscount);
            if (discountRate == null) {
                discountRate = BigDecimal.ZERO;
            }
            if (discountRate.compareTo(BigDecimal.ZERO) > 0) {
                discountAmount = subtotal.multiply(discountRate)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                if (discountAmount.compareTo(subtotal) > 0) {
                    discountAmount = subtotal;
                }
            }
        }

        BigDecimal discountedSubtotal = subtotal.subtract(discountAmount);
        if (discountedSubtotal.compareTo(BigDecimal.ZERO) < 0) {
            discountedSubtotal = BigDecimal.ZERO;
        }

        BigDecimal shipping = items.isEmpty() ? BigDecimal.ZERO : SHIPPING_FEE_USD;
        BigDecimal totalUSD = discountedSubtotal.add(shipping);

        long totalVND = totalUSD.multiply(BigDecimal.valueOf(USD_TO_VND_RATE))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
        if (totalVND < 5000 && !items.isEmpty()) {
            totalVND = 5000;
        }

        return new CheckoutTotals(
                subtotal,
                discountedSubtotal,
                discountRate,
                discountAmount,
                shipping,
                totalUSD,
                totalVND
        );
    }

    @Transactional
    public CheckoutResult createOrderFromSessionCart(String sessionId,
                                                     String email,
                                                     String address,
                                                     User authenticatedUser) {
        Cart cart = cartService.getOrCreateCartBySession(sessionId);
        List<CartItem> items = cartService.getItems(cart);
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        // Kiểm tra tồn kho trước khi tạo order
        for (CartItem cartItem : items) {
            if (cartItem == null || cartItem.getVariant() == null || cartItem.getQuantity() == null) {
                continue;
            }
            ProductVariant variant = productVariantRepository.findById(cartItem.getVariant().getVariantID())
                    .orElse(cartItem.getVariant());
            if (variant == null || variant.getProduct() == null) {
                throw new IllegalStateException("Product variant not available");
            }
            int available = variant.getProduct().getStockQuantity() != null
                    ? Math.max(variant.getProduct().getStockQuantity(), 0)
                    : Integer.MAX_VALUE;
            if (available <= 0 || cartItem.getQuantity() > available) {
                throw new IllegalStateException("Not enough stock for product: " +
                        (variant.getProduct().getName() != null ? variant.getProduct().getName() : "Unknown"));
            }
        }

        Customer discountCustomer = cart.getCustomer();
        if (discountCustomer == null && authenticatedUser != null) {
            discountCustomer = customerRepository.findByUser(authenticatedUser).orElse(null);
            if (discountCustomer != null && cart.getCustomer() == null) {
                cart.setCustomer(discountCustomer);
            }
        }

        if (email == null) {
            email = "";
        }
        String checkoutEmail = email.trim();
        if (checkoutEmail.isBlank()) {
            throw new IllegalArgumentException("Email is required to place an order.");
        }

        if (discountCustomer != null && cart.getCustomer() == null) {
            cart.setCustomer(discountCustomer);
            cartRepository.save(cart);
        }

        CheckoutTotals totals = calculateTotals(cart, discountCustomer);

        Order order = new Order();
        
        // Đảm bảo order luôn có customer và user được set
        Customer finalCustomer = null;
        User finalUser = null;
        
        if (cart.getCustomer() != null) {
            finalCustomer = cart.getCustomer();
            if (finalCustomer.getUser() != null) {
                finalUser = finalCustomer.getUser();
            }
        } else if (authenticatedUser != null) {
            finalUser = authenticatedUser;
            // Tìm hoặc tạo Customer cho authenticated user
            if (discountCustomer != null) {
                finalCustomer = discountCustomer;
            } else {
                Optional<Customer> existingCustomer = customerRepository.findByUser(authenticatedUser);
                if (existingCustomer.isPresent()) {
                    finalCustomer = existingCustomer.get();
                } else {
                    // Create new Customer for OAuth users
                    Customer newCustomer = new Customer();
                    newCustomer.setUser(authenticatedUser);
                    newCustomer.setLoyaltyPoints(0);
                    newCustomer.setTotalSpent(BigDecimal.ZERO);
                    finalCustomer = customerRepository.save(newCustomer);
                }
            }
        }
        
        // Set customer và user cho order - đảm bảo cả hai đều được set
        if (finalCustomer != null) {
            order.setCustomer(finalCustomer);
            // Đảm bảo user được set từ customer hoặc authenticatedUser
            if (finalCustomer.getUser() != null) {
                order.setUser(finalCustomer.getUser());
            } else if (finalUser != null) {
                order.setUser(finalUser);
            }
        } else if (finalUser != null) {
            order.setUser(finalUser);
            // Nếu không có customer, tạo mới
            Customer newCustomer = new Customer();
            newCustomer.setUser(finalUser);
            newCustomer.setLoyaltyPoints(0);
            newCustomer.setTotalSpent(BigDecimal.ZERO);
            finalCustomer = customerRepository.save(newCustomer);
            order.setCustomer(finalCustomer);
        }
        
        // Get shipping address from manual address string
        String shippingAddress = null;
        String checkoutName = null;
        if (address != null && !address.isBlank()) {
            shippingAddress = address.trim();
        }
        
        order.setStatus(OrderStatus.pending);
        order.setTotalAmount(totals.getTotalUSD().floatValue());

        if (shippingAddress != null && !shippingAddress.isBlank()) {
            order.setShippingAddress(shippingAddress);
        }

        order.setContactEmail(checkoutEmail);

        // Set customer name - ưu tiên từ user, nếu không có thì dùng email
        if (checkoutName == null || checkoutName.isBlank()) {
            if (order.getCustomer() != null && order.getCustomer().getUser() != null) {
                User customerUser = order.getCustomer().getUser();
                checkoutName = customerUser.getName();
                // Nếu name null hoặc empty, dùng email làm fallback
                if ((checkoutName == null || checkoutName.isBlank()) && customerUser.getEmail() != null) {
                    checkoutName = customerUser.getEmail().split("@")[0]; // Lấy phần trước @
                }
            } else if (authenticatedUser != null) {
                checkoutName = authenticatedUser.getName();
                // Nếu name null hoặc empty, dùng email làm fallback
                if ((checkoutName == null || checkoutName.isBlank()) && authenticatedUser.getEmail() != null) {
                    checkoutName = authenticatedUser.getEmail().split("@")[0]; // Lấy phần trước @
                }
            }
        }
        // Nếu vẫn không có name, dùng email
        if ((checkoutName == null || checkoutName.isBlank()) && checkoutEmail != null && !checkoutEmail.isBlank()) {
            checkoutName = checkoutEmail.split("@")[0];
        }
        // Cuối cùng nếu vẫn không có, dùng "Customer"
        if (checkoutName == null || checkoutName.isBlank()) {
            checkoutName = "Customer";
        }
        order.setCustomerName(checkoutName);

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : items) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setVariant(cartItem.getVariant());
            orderItem.setQuantity(cartItem.getQuantity());
            BigDecimal price = cartItem.getVariant().getPrice() != null ? cartItem.getVariant().getPrice() : BigDecimal.ZERO;
            orderItem.setPrice(price);
            orderItem.setTotalPrice(price.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            // Lưu size đã chọn (nếu có) từ cartItem sang orderItem
            if (cartItem.getSelectedSize() != null && !cartItem.getSelectedSize().isBlank()) {
                orderItem.setSelectedSize(cartItem.getSelectedSize());
            }
            orderItems.add(orderItem);
        }
        order.setOrderItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setMethod(PaymentMethod.VNPay);
        payment.setAmount(BigDecimal.valueOf(totals.getTotalVND()));
        payment.setStatus(PaymentStatus.pending);
        
        // Store email and shipping address in payment details as JSON
        if (email != null || shippingAddress != null) {
            try {
                Map<String, String> details = new HashMap<>();
                if (email != null && !email.isBlank()) {
                    details.put("email", email);
                }
                if (shippingAddress != null && !shippingAddress.isBlank()) {
                    details.put("shippingAddress", shippingAddress);
                }
                payment.setPaymentDetails(objectMapper.writeValueAsString(details));
            } catch (JsonProcessingException e) {
                // Ignore if JSON serialization fails
            }
        }
        
        paymentRepository.save(payment);

        cart.setStatus(CartStatus.ordered);
        cartRepository.save(cart);
        cartItemRepository.deleteAll(items);

        return new CheckoutResult(savedOrder, payment, totals);
    }

    @Transactional
    public void handlePaymentResult(Integer orderId,
                                    boolean success,
                                    String transactionId,
                                    long amountVnd,
                                    Map<String, String> rawParams) {
        if (orderId == null) {
            return;
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        Optional<Payment> latestPaymentOpt = paymentRepository.findTopByOrderOrderIDOrderByPaymentDateDesc(orderId);
        Payment payment = latestPaymentOpt.orElseGet(() -> {
            Payment p = new Payment();
            p.setOrder(order);
            p.setMethod(PaymentMethod.VNPay);
            return p;
        });

        payment.setTransactionCode(transactionId);
        payment.setAmount(BigDecimal.valueOf(amountVnd));
        payment.setStatus(success ? PaymentStatus.success : PaymentStatus.failed);
        if (rawParams != null && !rawParams.isEmpty()) {
            try {
                payment.setPaymentDetails(objectMapper.writeValueAsString(rawParams));
            } catch (JsonProcessingException ignored) {
            }
        }
        paymentRepository.save(payment);

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(success ? OrderStatus.paid : OrderStatus.canceled);
        orderRepository.save(order);

        boolean shouldDeductInventory = success && (previousStatus == null
                || (previousStatus != OrderStatus.paid
                && previousStatus != OrderStatus.shipped
                && previousStatus != OrderStatus.completed));

        if (shouldDeductInventory) {
            deductInventoryForOrder(order);
        }
        
        // Process membership rewards when payment is successful
        if (success && order.getCustomer() != null) {
            try {
                membershipService.processOrderRewards(order);
            } catch (Exception e) {
                // Log error but don't fail the payment processing
                System.err.println("Error processing membership rewards: " + e.getMessage());
            }
        }
    }

    private void deductInventoryForOrder(Order order) {
        if (order == null || order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return;
        }

        Map<Integer, Integer> productDeductionMap = new HashMap<>();

        order.getOrderItems().forEach(item -> {
            if (item == null || item.getVariant() == null || item.getQuantity() == null) {
                return;
            }

            ProductVariant variant = productVariantRepository.findById(item.getVariant().getVariantID())
                    .orElse(item.getVariant());

            if (variant != null && variant.getProduct() != null && variant.getProduct().getProductID() != null) {
                Integer productId = variant.getProduct().getProductID();
                productDeductionMap.merge(productId, item.getQuantity(), Integer::sum);
            }

            recordVariantExport(variant, item.getQuantity(), order.getOrderID());
        });

        productDeductionMap.forEach((productId, deductionQty) ->
                productRepository.findById(productId).ifPresent(product -> {
                    int current = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
                    int updated = current - deductionQty;
                    if (updated < 0) {
                        updated = 0;
                    }
                    product.setStockQuantity(updated);
                    productRepository.save(product);
                }));
    }

    private void recordVariantExport(ProductVariant variant, int quantity, Integer orderId) {
        if (variant == null || quantity <= 0 || stockRepository == null || variant.getVariantID() == null) {
            return;
        }

        int currentQuantity = 0;
        List<Stock> history = stockRepository.findByVariantOrderByUpdatedAtDesc(variant.getVariantID());
        if (history != null && !history.isEmpty() && history.get(0).getCurrentQuantity() != null) {
            currentQuantity = history.get(0).getCurrentQuantity();
        } else if (variant.getProduct() != null && variant.getProduct().getStockQuantity() != null) {
            currentQuantity = variant.getProduct().getStockQuantity();
        }

        int updatedQuantity = Math.max(0, currentQuantity - quantity);

        Stock exportRecord = new Stock();
        exportRecord.setVariant(variant);
        exportRecord.setChangeType(Stock.ChangeType.export);
        exportRecord.setChangeAmount(quantity);
        exportRecord.setCurrentQuantity(updatedQuantity);
        exportRecord.setNote(orderId != null ? "Auto deduction after order #" + orderId : "Auto deduction");
        stockRepository.save(exportRecord);
    }

    public Integer extractOrderId(String orderInfo) {
        if (orderInfo == null || orderInfo.isBlank()) {
            return null;
        }
        if (orderInfo.startsWith("ORDER-")) {
            orderInfo = orderInfo.substring(6);
        }
        try {
            return Integer.parseInt(orderInfo.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static class CheckoutTotals {
        private final BigDecimal subtotalUSD;
        private final BigDecimal discountedSubtotalUSD;
        private final BigDecimal discountRate;
        private final BigDecimal discountAmountUSD;
        private final BigDecimal shippingUSD;
        private final BigDecimal totalUSD;
        private final long totalVND;

        public CheckoutTotals(BigDecimal subtotalUSD,
                              BigDecimal discountedSubtotalUSD,
                              BigDecimal discountRate,
                              BigDecimal discountAmountUSD,
                              BigDecimal shippingUSD,
                              BigDecimal totalUSD,
                              long totalVND) {
            this.subtotalUSD = subtotalUSD;
            this.discountedSubtotalUSD = discountedSubtotalUSD;
            this.discountRate = discountRate;
            this.discountAmountUSD = discountAmountUSD;
            this.shippingUSD = shippingUSD;
            this.totalUSD = totalUSD;
            this.totalVND = totalVND;
        }

        public BigDecimal getSubtotalUSD() {
            return subtotalUSD;
        }

        public BigDecimal getDiscountedSubtotalUSD() {
            return discountedSubtotalUSD;
        }

        public BigDecimal getDiscountRate() {
            return discountRate;
        }

        public BigDecimal getDiscountAmountUSD() {
            return discountAmountUSD;
        }

        public BigDecimal getShippingUSD() {
            return shippingUSD;
        }

        public BigDecimal getTotalUSD() {
            return totalUSD;
        }

        public long getTotalVND() {
            return totalVND;
        }

        public boolean hasDiscount() {
            return discountAmountUSD != null && discountAmountUSD.compareTo(BigDecimal.ZERO) > 0;
        }
    }

    public static class CheckoutResult {
        private final Order order;
        private final Payment payment;
        private final CheckoutTotals totals;

        public CheckoutResult(Order order, Payment payment, CheckoutTotals totals) {
            this.order = order;
            this.payment = payment;
            this.totals = totals;
        }

        public Order getOrder() {
            return order;
        }

        public Payment getPayment() {
            return payment;
        }

        public CheckoutTotals getTotals() {
            return totals;
        }
    }
}


