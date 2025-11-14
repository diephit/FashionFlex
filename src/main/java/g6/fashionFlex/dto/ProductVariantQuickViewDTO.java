package g6.fashionFlex.dto;

import java.math.BigDecimal;

public class ProductVariantQuickViewDTO {
    private Integer variantId;
    private String sku;
    private BigDecimal price;
    private String variantImage;

    public ProductVariantQuickViewDTO() {
    }

    public ProductVariantQuickViewDTO(Integer variantId, String sku, BigDecimal price, String variantImage) {
        this.variantId = variantId;
        this.sku = sku;
        this.price = price;
        this.variantImage = variantImage;
    }

    public Integer getVariantId() {
        return variantId;
    }

    public void setVariantId(Integer variantId) {
        this.variantId = variantId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getVariantImage() {
        return variantImage;
    }

    public void setVariantImage(String variantImage) {
        this.variantImage = variantImage;
    }
}

