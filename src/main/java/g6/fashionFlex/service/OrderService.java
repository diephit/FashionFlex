package g6.fashionFlex.service;

import g6.fashionFlex.dto.*;
import g6.fashionFlex.entity.Order;

import java.util.List;

/**
 * Service interface for customer-facing order operations
 * Handles order placement, retrieval, and tracking
 */
public interface OrderService {

    /**
     * Place a new order from cart
     * @param request Order placement request with all necessary data
     * @return OrderConfirmationDTO with order details
     */
    OrderConfirmationDTO placeOrder(PlaceOrderRequest request);

    /**
     * Get all orders for a specific user
     * @param userId User ID
     * @return List of user's orders
     */
    List<OrderDTO> getOrdersByUserId(Long userId);

    /**
     * Get orders by user ID with status filter
     * @param userId User ID
     * @param status Order status
     * @return List of filtered orders
     */
    List<OrderDTO> getOrdersByUserIdAndStatus(Long userId, Order.OrderStatus status);

    /**
     * Get order details by ID (with ownership verification)
     * @param orderId Order ID
     * @param userId User ID (for ownership verification)
     * @return Order details
     */
    OrderDTO getOrderDetails(Long orderId, Long userId);

    /**
     * Track order by order number
     * @param orderNumber Order number
     * @param userId User ID (for ownership verification)
     * @return Order tracking information
     */
    OrderTrackingDTO trackOrder(String orderNumber, Long userId);

    /**
     * Track order by order number and email (for guest tracking)
     * @param orderNumber Order number
     * @param email Email address
     * @return Order tracking information
     */
    OrderTrackingDTO trackOrderByEmail(String orderNumber, String email);

    /**
     * Get order confirmation details
     * @param orderId Order ID
     * @param userId User ID (for ownership verification)
     * @return Order confirmation details
     */
    OrderConfirmationDTO getOrderConfirmation(Long orderId, Long userId);

    /**
     * Cancel order (only allowed for PENDING orders)
     * @param orderId Order ID
     * @param userId User ID (for ownership verification)
     * @param reason Cancellation reason
     * @return Updated order DTO
     */
    OrderDTO cancelOrder(Long orderId, Long userId, String reason);

    /**
     * Get total count of orders for a specific user
     * @param userId User ID
     * @return Total number of orders for the user
     */
    long getUserOrderCount(Long userId);

    /**
     * Get recent orders for a specific user (for dashboard display)
     * @param userId User ID
     * @param limit Maximum number of orders to return
     * @return List of recent orders for the user
     */
    List<OrderDTO> getRecentOrdersByUserId(Long userId, int limit);
}
