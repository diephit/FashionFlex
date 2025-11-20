package g6.fashionFlex.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddToWishlistRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;
}
