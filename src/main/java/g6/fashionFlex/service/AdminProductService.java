package g6.fashionFlex.service;

import g6.fashionFlex.dto.ProductDTO;
import g6.fashionFlex.entity.Category;
import g6.fashionFlex.entity.Product;
import g6.fashionFlex.repository.CategoryRepository;
import g6.fashionFlex.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

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

    public ProductDTO createProduct(ProductDTO productDTO) {
        Product product = convertToEntity(productDTO);
        Product savedProduct = productRepository.save(product);
        return convertToDTO(savedProduct);
    }

    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setDiscountPrice(productDTO.getDiscountPrice());
        product.setStock(productDTO.getStock());
        product.setSku(productDTO.getSku());
        product.setImageUrl(productDTO.getImageUrl());
        product.setAdditionalImages(productDTO.getAdditionalImages());
        product.setActive(productDTO.getActive());
        product.setFeatured(productDTO.getFeatured());
        product.setAvailableSizes(productDTO.getAvailableSizes());
        product.setAvailableColors(productDTO.getAvailableColors());

        if (productDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(productDTO.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + productDTO.getCategoryId()));
            product.setCategory(category);
        }

        Product updatedProduct = productRepository.save(product);
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
        product.setAvailableSizes(dto.getAvailableSizes());
        product.setAvailableColors(dto.getAvailableColors());

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + dto.getCategoryId()));
            product.setCategory(category);
        }

        return product;
    }
}
