package g6.fashionFlex.service.impl;

import g6.fashionFlex.entity.Product;
import g6.fashionFlex.repository.CategoryRepository;
import g6.fashionFlex.repository.ProductRepository;
import g6.fashionFlex.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public List<Product> findAllActive() {
        return productRepository.findByActiveTrue();
    }

    @Override
    public Page<Product> findAllActive(Pageable pageable) {
        return productRepository.findByActiveTrue(pageable);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    @Override
    public List<Product> getFeaturedProducts() {
        return productRepository.findByFeaturedTrueAndActiveTrueOrderByCreatedAtDesc();
    }

    @Override
    public List<Product> getLatestProducts(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> page = productRepository.findByActiveTrue(pageable);
        return page.getContent();
    }

    @Override
    public List<Product> getBestSellers() {
        List<Product> products = productRepository.findTop10ByActiveTrueOrderBySoldCountDesc();
        return products.size() > 10 ? products.subList(0, 10) : products;
    }

    @Override
    public Page<Product> getProductsByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryIdAndActiveTrue(categoryId, pageable);
    }

    @Override
    public Page<Product> getProductsByCategoryName(String categoryName, Pageable pageable) {
        // Find category by name and get its products
        return categoryRepository.findByName(categoryName)
                .map(category -> productRepository.findByCategoryIdAndActiveTrue(category.getId(), pageable))
                .orElse(Page.empty(pageable));
    }

    @Override
    public Page<Product> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return productRepository.findByPriceBetweenAndActiveTrue(minPrice, maxPrice, pageable);
    }

    @Override
    public Page<Product> searchProducts(String keyword, Pageable pageable) {
        return productRepository.searchByName(keyword, pageable);
    }

    @Override
    public Page<Product> getProductsWithDiscount(Pageable pageable) {
        return productRepository.findProductsWithDiscount(pageable);
    }

    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Override
    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    @Override
    public void incrementViewCount(Long id) {
        productRepository.findById(id).ifPresent(product -> {
            product.setViewCount(product.getViewCount() + 1);
            productRepository.save(product);
        });
    }
}
