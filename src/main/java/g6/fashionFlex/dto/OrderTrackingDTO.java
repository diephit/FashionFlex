package g6.fashionFlex.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for order tracking information
 * Contains order status timeline and tracking details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrackingDTO {

    private String orderNumber;
    private String currentStatus;
    private String estimatedDelivery;
    private String shippingMethod;
    private String trackingNumber;

    // Timeline events
    private List<TrackingEvent> timeline;

    // Shipping address
    private String shippingAddress;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrackingEvent {
        private String status;
        private String title;
        private String description;
        private LocalDateTime timestamp;
        private boolean completed;
        private boolean active;
    }
}
