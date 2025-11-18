package g6.fashionFlex.service;

import g6.fashionFlex.dto.ProductDTO;
import g6.fashionFlex.entity.Brand;
import g6.fashionFlex.entity.Category;
import g6.fashionFlex.entity.Product;
import g6.fashionFlex.repository.BrandRepository;
import g6.fashionFlex.repository.CategoryRepository;
import g6.fashionFlex.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class AdminProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private FileUploadService fileUploadService;

    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::convertToDTO);
    }

    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return convertToDTO(product);
    }

    public ProductDTO createProduct(ProductDTO productDTO) throws IOException {
        Product product = convertToEntity(productDTO);

        // Generate SKU if not provided
        if (product.getSku() == null || product.getSku().trim().isEmpty()) {
            product.setSku(generateSKU(product.getName()));
        }

        // Handle main image upload
        if (productDTO.getMainImage() != null && !productDTO.getMainImage().isEmpty()) {
            String imagePath = fileUploadService.uploadFile(productDTO.getMainImage(), "products");
            product.setImageUrl(imagePath);
        }

        // Handle additional images upload
        if (productDTO.getAdditionalImagesFiles() != null && !productDTO.getAdditionalImagesFiles().isEmpty()) {
            var uploadedPaths = fileUploadService.uploadFiles(productDTO.getAdditionalImagesFiles(), "products");
            product.setAdditionalImages(new HashSet<>(uploadedPaths));
        }

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with id: {}", savedProduct.getId());
        return convertToDTO(savedProduct);
    }

    public ProductDTO updateProduct(Long id, ProductDTO productDTO) throws IOException {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setDiscountPrice(productDTO.getDiscountPrice());
        product.setStock(productDTO.getStock());
        product.setSku(productDTO.getSku());
        product.setActive(productDTO.getActive());
        product.setFeatured(productDTO.getFeatured());

        // Parse and update sizes and colors
        productDTO.parseSizesInput();
        productDTO.parseColorsInput();
        product.setAvailableSizes(productDTO.getAvailableSizes());
        product.setAvailableColors(productDTO.getAvailableColors());

        if (productDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(productDTO.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + productDTO.getCategoryId()));
            product.setCategory(category);
        }

        // Update brand
        if (productDTO.getBrandId() != null) {
            Brand brand = brandRepository.findById(productDTO.getBrandId())
                    .orElseThrow(() -> new RuntimeException("Brand not found with id: " + productDTO.getBrandId()));
            product.setBrand(brand);
        } else {
            product.setBrand(null);
        }

        // Handle main image upload if new image provided
        if (productDTO.getMainImage() != null && !productDTO.getMainImage().isEmpty()) {
            // Delete old image if exists
            if (product.getImageUrl() != null) {
                fileUploadService.deleteFile(product.getImageUrl());
            }
            // Upload new image
            String imagePath = fileUploadService.uploadFile(productDTO.getMainImage(), "products");
            product.setImageUrl(imagePath);
        }

        // Handle additional images upload if provided
        if (productDTO.getAdditionalImagesFiles() != null && !productDTO.getAdditionalImagesFiles().isEmpty()) {
            // Delete old additional images
            if (product.getAdditionalImages() != null) {
                product.getAdditionalImages().forEach(fileUploadService::deleteFile);
            }
            // Upload new images
            var uploadedPaths = fileUploadService.uploadFiles(productDTO.getAdditionalImagesFiles(), "products");
            product.setAdditionalImages(new HashSet<>(uploadedPaths));
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully with id: {}", updatedProduct.getId());
        return convertToDTO(updatedProduct);
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        productRepository.delete(product);
    }

    public ProductDTO toggleProductStatus(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        product.setActive(!product.getActive());
        Product updatedProduct = productRepository.save(product);
        return convertToDTO(updatedProduct);
    }

    public Page<ProductDTO> searchProducts(String keyword, Pageable pageable) {
        return productRepository.adminSearchByName(keyword, pageable).map(this::convertToDTO);
    }

    public long countLowStockProducts(int threshold) {
        return productRepository.findAll().stream()
                .filter(p -> p.getStock() <= threshold && p.getStock() > 0)
                .count();
    }

    public long countOutOfStockProducts() {
        return productRepository.findAll().stream()
                .filter(p -> p.getStock() == 0)
                .count();
    }

    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setDiscountPrice(product.getDiscountPrice());
        dto.setStock(product.getStock());
        dto.setSku(product.getSku());
        dto.setImageUrl(product.getImageUrl());
        dto.setAdditionalImages(product.getAdditionalImages());
        dto.setActive(product.getActive());
        dto.setFeatured(product.getFeatured());
        dto.setAvailableSizes(product.getAvailableSizes());
        dto.setAvailableColors(product.getAvailableColors());
        dto.setViewCount(product.getViewCount());
        dto.setSoldCount(product.getSoldCount());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());

        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
        }

        if (product.getBrand() != null) {
            dto.setBrandId(product.getBrand().getId());
            dto.setBrandName(product.getBrand().getName());
        }

        // Set sizes and colors as comma-separated strings for form
        dto.setSizesInput(dto.getSizesAsString());
        dto.setColorsInput(dto.getColorsAsString());

        return dto;
    }

    private Product convertToEntity(ProductDTO dto) {
        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setDiscountPrice(dto.getDiscountPrice());
        product.setStock(dto.getStock());
        product.setSku(dto.getSku());
        product.setImageUrl(dto.getImageUrl());
        product.setAdditionalImages(dto.getAdditionalImages());
        product.setActive(dto.getActive() != null ? dto.getActive() : true);
        product.setFeatured(dto.getFeatured() != null ? dto.getFeatured() : false);

        // Parse sizes and colors from input
        dto.parseSizesInput();
        dto.parseColorsInput();
        product.setAvailableSizes(dto.getAvailableSizes());
        product.setAvailableColors(dto.getAvailableColors());

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + dto.getCategoryId()));
            product.setCategory(category);
        }

        if (dto.getBrandId() != null) {
            Brand brand = brandRepository.findById(dto.getBrandId())
                    .orElseThrow(() -> new RuntimeException("Brand not found with id: " + dto.getBrandId()));
            product.setBrand(brand);
        }

        return product;
    }

    /**
     * Generate SKU from product name
     */
    private String generateSKU(String productName) {
        String base = productName.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (base.length() > 10) {
            base = base.substring(0, 10);
        }
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);
        return base + "-" + timestamp;
    }

    /**
     * Advanced search with multiple filters
     */
    public Page<ProductDTO> searchProductsAdvanced(String keyword, Long categoryId, Long brandId,
                                                    Boolean active, Boolean featured, Pageable pageable) {
        Page<Product> products;

        if (keyword != null && !keyword.trim().isEmpty()) {
            products = productRepository.adminSearchByName(keyword, pageable);
        } else {
            products = productRepository.findAll(pageable);
        }

        // Apply filters
        return products.map(this::convertToDTO)
                .map(dto -> {
                    // Filter by category
                    if (categoryId != null && !categoryId.equals(dto.getCategoryId())) {
                        return null;
                    }
                    // Filter by brand
                    if (brandId != null && !brandId.equals(dto.getBrandId())) {
                        return null;
                    }
                    // Filter by active status
                    if (active != null && !active.equals(dto.getActive())) {
                        return null;
                    }
                    // Filter by featured status
                    if (featured != null && !featured.equals(dto.getFeatured())) {
                        return null;
                    }
                    return dto;
                });
    }

    /**
     * Get product statistics for dashboard
     */
    public ProductStatistics getStatistics() {
        List<Product> allProducts = productRepository.findAll();

        long totalProducts = allProducts.size();
        long activeProducts = allProducts.stream().filter(Product::getActive).count();
        long featuredProducts = allProducts.stream().filter(Product::getFeatured).count();
        long outOfStock = allProducts.stream().filter(p -> p.getStock() == 0).count();
        long lowStock = allProducts.stream().filter(p -> p.getStock() > 0 && p.getStock() <= 10).count();
        long withDiscount = allProducts.stream()
                .filter(p -> p.getDiscountPrice() != null && p.getDiscountPrice().compareTo(p.getPrice()) < 0)
                .count();

        return new ProductStatistics(totalProducts, activeProducts, featuredProducts,
                                     outOfStock, lowStock, withDiscount);
    }

    /**
     * Bulk activate products
     */
    public int bulkActivate(List<Long> productIds) {
        int count = 0;
        for (Long id : productIds) {
            try {
                Product product = productRepository.findById(id).orElse(null);
                if (product != null && !product.getActive()) {
                    product.setActive(true);
                    productRepository.save(product);
                    count++;
                }
            } catch (Exception e) {
                log.error("Error activating product {}: {}", id, e.getMessage());
            }
        }
        log.info("Bulk activated {} products", count);
        return count;
    }

    /**
     * Bulk deactivate products
     */
    public int bulkDeactivate(List<Long> productIds) {
        int count = 0;
        for (Long id : productIds) {
            try {
                Product product = productRepository.findById(id).orElse(null);
                if (product != null && product.getActive()) {
                    product.setActive(false);
                    productRepository.save(product);
                    count++;
                }
            } catch (Exception e) {
                log.error("Error deactivating product {}: {}", id, e.getMessage());
            }
        }
        log.info("Bulk deactivated {} products", count);
        return count;
    }

    /**
     * Bulk delete products
     */
    public int bulkDelete(List<Long> productIds) {
        int count = 0;
        for (Long id : productIds) {
            try {
                productRepository.deleteById(id);
                count++;
            } catch (Exception e) {
                log.error("Error deleting product {}: {}", id, e.getMessage());
            }
        }
        log.info("Bulk deleted {} products", count);
        return count;
    }

    // Product Statistics DTO
    public static class ProductStatistics {
        private final long totalProducts;
        private final long activeProducts;
        private final long featuredProducts;
        private final long outOfStock;
        private final long lowStock;
        private final long withDiscount;

        public ProductStatistics(long totalProducts, long activeProducts, long featuredProducts,
                                long outOfStock, long lowStock, long withDiscount) {
            this.totalProducts = totalProducts;
            this.activeProducts = activeProducts;
            this.featuredProducts = featuredProducts;
            this.outOfStock = outOfStock;
            this.lowStock = lowStock;
            this.withDiscount = withDiscount;
        }

        public long getTotalProducts() { return totalProducts; }
        public long getActiveProducts() { return activeProducts; }
        public long getFeaturedProducts() { return featuredProducts; }
        public long getOutOfStock() { return outOfStock; }
        public long getLowStock() { return lowStock; }
        public long getWithDiscount() { return withDiscount; }
    }
}
