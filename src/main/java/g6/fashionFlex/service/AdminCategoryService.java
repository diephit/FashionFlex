package g6.fashionFlex.service;

import g6.fashionFlex.entity.Category;
import g6.fashionFlex.entity.Category.CategoryStatus;
import g6.fashionFlex.entity.Product;
import g6.fashionFlex.entity.Product.ProductStatus;
import g6.fashionFlex.repository.CategoryRepository;
import g6.fashionFlex.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminCategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public List<Category> getRootCategories() {
        return categoryRepository.findByParentCategoryIsNull();
    }

    public List<Category> getSubCategories(Integer parentCategoryID) {
        return categoryRepository.findByParentCategoryCategoryID(parentCategoryID);
    }

    public List<Category> searchCategories(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return categoryRepository.findAll();
        }
        return categoryRepository.searchCategories(keyword);
    }

    public Optional<Category> getCategoryById(Integer categoryID) {
        return categoryRepository.findById(categoryID);
    }

    public long countAllCategories() {
        return categoryRepository.count();
    }

    public long countByStatus(CategoryStatus status) {
        return categoryRepository.countByStatus(status);
    }

    public long countTotalProducts() {
        return productRepository.count();
    }

    @Transactional
    public Category createCategory(Category category) {
        if (category.getStatus() == null) {
            category.setStatus(CategoryStatus.active);
        }
        return categoryRepository.save(category);
    }

    @Transactional
    public Category updateCategory(Integer categoryID, Category categoryData) {
        Category category = categoryRepository.findById(categoryID)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        category.setName(categoryData.getName());
        category.setParentCategory(categoryData.getParentCategory());
        boolean statusChanged = categoryData.getStatus() != null && categoryData.getStatus() != category.getStatus();
        if (categoryData.getStatus() != null) {
            category.setStatus(categoryData.getStatus());
        }
        
        if (statusChanged) {
            propagateStatusToTree(category, category.getStatus());
            return category;
        }
        
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Integer categoryID) {
        Category root = categoryRepository.findById(categoryID)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Thu thập toàn bộ cây category (cha + con, cháu, ...)
        List<Category> tree = collectCategoryTree(root);

        // Gỡ category khỏi product trước để tránh lỗi ràng buộc FK
        List<Integer> categoryIds = tree.stream()
                .map(Category::getCategoryID)
                .collect(Collectors.toList());

        if (!categoryIds.isEmpty()) {
            List<Product> products = productRepository.findByCategoryCategoryIDIn(categoryIds);
            for (Product product : products) {
                product.setCategory(null);
            }
            if (!products.isEmpty()) {
                productRepository.saveAll(products);
            }
        }

        // Xóa toàn bộ category cây từ lá lên (collectCategoryTree đã gom đủ)
        categoryRepository.deleteAll(tree);
    }

    @Transactional
    public Category toggleCategoryStatus(Integer categoryID) {
        Category category = categoryRepository.findById(categoryID)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        CategoryStatus newStatus = category.getStatus() == CategoryStatus.active
                ? CategoryStatus.inactive
                : CategoryStatus.active;
        category.setStatus(newStatus);
        propagateStatusToTree(category, newStatus);
        return category;
    }

    private void propagateStatusToTree(Category root, CategoryStatus status) {
        List<Category> tree = collectCategoryTree(root);
        for (Category cat : tree) {
            cat.setStatus(status);
        }
        categoryRepository.saveAll(tree);

        List<Integer> categoryIds = tree.stream()
                .map(Category::getCategoryID)
                .collect(Collectors.toList());

        if (!categoryIds.isEmpty()) {
            List<Product> products = productRepository.findByCategoryCategoryIDIn(categoryIds);
            ProductStatus newProductStatus = status == CategoryStatus.active ? ProductStatus.active : ProductStatus.inactive;
            boolean updated = false;
            for (Product product : products) {
                if (product.getStatus() != newProductStatus) {
                    product.setStatus(newProductStatus);
                    updated = true;
                }
            }
            if (updated) {
                productRepository.saveAll(products);
            }
        }
    }

    private List<Category> collectCategoryTree(Category root) {
        List<Category> collected = new ArrayList<>();
        Deque<Category> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            Category current = queue.poll();
            collected.add(current);
            List<Category> children = categoryRepository.findByParentCategoryCategoryID(current.getCategoryID());
            queue.addAll(children);
        }
        return collected;
    }
}

