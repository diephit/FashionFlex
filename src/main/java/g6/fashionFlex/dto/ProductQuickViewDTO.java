package g6.fashionFlex.dto;

import java.math.BigDecimal;
import java.util.List;

public class ProductQuickViewDTO {
    private Integer productId;
    private String name;
    private String description;
    private String mainImage;
    private List<ProductVariantQuickViewDTO> variants;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String priceLabel;

    public ProductQuickViewDTO() {
    }

    public ProductQuickViewDTO(Integer productId, String name, String description, String mainImage,
                               List<ProductVariantQuickViewDTO> variants, BigDecimal minPrice,
                               BigDecimal maxPrice, String priceLabel) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.mainImage = mainImage;
        this.variants = variants;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.priceLabel = priceLabel;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMainImage() {
        return mainImage;
    }

    public void setMainImage(String mainImage) {
        this.mainImage = mainImage;
    }

    public List<ProductVariantQuickViewDTO> getVariants() {
        return variants;
    }

    public void setVariants(List<ProductVariantQuickViewDTO> variants) {
        this.variants = variants;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String getPriceLabel() {
        return priceLabel;
    }

    public void setPriceLabel(String priceLabel) {
        this.priceLabel = priceLabel;
    }
}

