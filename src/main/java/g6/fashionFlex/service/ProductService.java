package g6.fashionFlex.service;

import g6.fashionFlex.dto.ProductQuickViewDTO;
import g6.fashionFlex.dto.ProductVariantQuickViewDTO;
import g6.fashionFlex.entity.Product;
import g6.fashionFlex.entity.ProductVariant;
import g6.fashionFlex.exception.ResourceNotFoundException;
import g6.fashionFlex.repository.ProductRepository;
import g6.fashionFlex.repository.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    public ProductService(ProductRepository productRepository, ProductVariantRepository variantRepository) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
    }

    @Transactional(readOnly = true)
    public ProductQuickViewDTO getQuickViewByProductId(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        List<ProductVariant> activeVariants = variantRepository.findByProductProductIDAndStatus(
                productId,
                ProductVariant.VariantStatus.active
        );

        List<ProductVariantQuickViewDTO> variantDTOs = activeVariants.stream()
                .map(variant -> new ProductVariantQuickViewDTO(
                        variant.getVariantID(),
                        variant.getSku(),
                        variant.getPrice(),
                        variant.getVariantImage()
                ))
                .toList();

        BigDecimal[] minMaxPrice = calculatePriceRange(activeVariants);
        String priceLabel = buildPriceLabel(minMaxPrice[0], minMaxPrice[1]);

        ProductQuickViewDTO dto = new ProductQuickViewDTO(
                product.getProductID(),
                product.getName(),
                product.getDescription(),
                product.getMainImage(),
                variantDTOs,
                minMaxPrice[0],
                minMaxPrice[1],
                priceLabel
        );
        return dto;
    }

    private BigDecimal[] calculatePriceRange(List<ProductVariant> variants) {
        Optional<BigDecimal> min = variants.stream()
                .map(ProductVariant::getPrice)
                .filter(price -> price != null)
                .min(BigDecimal::compareTo);

        Optional<BigDecimal> max = variants.stream()
                .map(ProductVariant::getPrice)
                .filter(price -> price != null)
                .max(BigDecimal::compareTo);

        return new BigDecimal[]{
                min.orElse(null),
                max.orElse(null)
        };
    }

    private String buildPriceLabel(BigDecimal min, BigDecimal max) {
        if (min == null || max == null) {
            return "Contact for price";
        }
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
        if (min.compareTo(max) == 0) {
            return formatter.format(min);
        }
        return formatter.format(min) + " - " + formatter.format(max);
    }
}

