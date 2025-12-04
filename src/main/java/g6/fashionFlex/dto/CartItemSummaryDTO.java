package g6.fashionFlex.dto;

import java.math.BigDecimal;

public class CartItemSummaryDTO {
    private Integer cartItemId;
    private Integer variantId;
    private String productName;
    private String sku;
    private String variantLabel;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal lineTotal;
    private String image;

    public CartItemSummaryDTO() {
    }

    public CartItemSummaryDTO(Integer cartItemId, Integer variantId, String productName, String sku,
                              String variantLabel, Integer quantity, BigDecimal price,
                              BigDecimal lineTotal, String image) {
        this.cartItemId = cartItemId;
        this.variantId = variantId;
        this.productName = productName;
        this.sku = sku;
        this.variantLabel = variantLabel;
        this.quantity = quantity;
        this.price = price;
        this.lineTotal = lineTotal;
        this.image = image;
    }

    public Integer getCartItemId() {
        return cartItemId;
    }

    public void setCartItemId(Integer cartItemId) {
        this.cartItemId = cartItemId;
    }

    public Integer getVariantId() {
        return variantId;
    }

    public void setVariantId(Integer variantId) {
        this.variantId = variantId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getVariantLabel() {
        return variantLabel;
    }

    public void setVariantLabel(String variantLabel) {
        this.variantLabel = variantLabel;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}

