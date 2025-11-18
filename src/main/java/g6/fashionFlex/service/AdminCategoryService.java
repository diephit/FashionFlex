package g6.fashionFlex.service;

import g6.fashionFlex.dto.CategoryDTO;
import g6.fashionFlex.entity.Category;
import g6.fashionFlex.repository.CategoryRepository;
import g6.fashionFlex.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class AdminCategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public CategoryDTO getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        return convertToDTO(category);
    }

    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        if (categoryRepository.existsByName(categoryDTO.getName())) {
            throw new RuntimeException("Category with name '" + categoryDTO.getName() + "' already exists");
        }

        Category category = new Category();
        category.setName(categoryDTO.getName());
        category.setDescription(categoryDTO.getDescription());
        category.setActive(categoryDTO.getActive() != null ? categoryDTO.getActive() : true);

        Category savedCategory = categoryRepository.save(category);
        return convertToDTO(savedCategory);
    }

    public CategoryDTO updateCategory(Long id, CategoryDTO categoryDTO) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        // Check if name is being changed and if new name already exists
        if (!category.getName().equals(categoryDTO.getName()) &&
            categoryRepository.existsByName(categoryDTO.getName())) {
            throw new RuntimeException("Category with name '" + categoryDTO.getName() + "' already exists");
        }

        category.setName(categoryDTO.getName());
        category.setDescription(categoryDTO.getDescription());
        category.setActive(categoryDTO.getActive());

        Category updatedCategory = categoryRepository.save(category);
        return convertToDTO(updatedCategory);
    }

    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        long productCount = productRepository.countByCategoryId(id);
        if (productCount > 0) {
            throw new RuntimeException("Cannot delete category with " + productCount + " products. Please reassign or delete the products first.");
        }

        categoryRepository.delete(category);
    }

    public CategoryDTO toggleCategoryStatus(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        category.setActive(!category.getActive());
        Category updatedCategory = categoryRepository.save(category);
        return convertToDTO(updatedCategory);
    }

    private CategoryDTO convertToDTO(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setActive(category.getActive());
        // Use productRepository to count products instead of accessing lazy-loaded collection
        dto.setProductCount((int) productRepository.countByCategoryId(category.getId()));
        dto.setCreatedAt(category.getCreatedAt());
        dto.setUpdatedAt(category.getUpdatedAt());
        return dto;
    }

    /**
     * Get paginated categories
     */
    public Page<CategoryDTO> getAllCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable).map(this::convertToDTO);
    }

    /**
     * Search categories with filters
     */
    public Page<CategoryDTO> searchCategories(String keyword, Boolean active, Pageable pageable) {
        List<Category> allCategories = categoryRepository.findAll();

        // Apply filters
        List<Category> filteredCategories = allCategories.stream()
                .filter(category -> {
                    // Keyword filter
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String lowerKeyword = keyword.toLowerCase();
                        boolean matchesName = category.getName().toLowerCase().contains(lowerKeyword);
                        boolean matchesDesc = category.getDescription() != null &&
                                             category.getDescription().toLowerCase().contains(lowerKeyword);
                        if (!matchesName && !matchesDesc) {
                            return false;
                        }
                    }

                    // Active status filter
                    if (active != null && !category.getActive().equals(active)) {
                        return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());

        // Convert to Page
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredCategories.size());
        List<CategoryDTO> pageContent = filteredCategories.subList(start, end).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(
                pageContent, pageable, filteredCategories.size());
    }

    /**
     * Get category statistics
     */
    public CategoryStatistics getStatistics() {
        List<Category> allCategories = categoryRepository.findAll();

        long totalCategories = allCategories.size();
        long activeCategories = allCategories.stream().filter(Category::getActive).count();
        long inactiveCategories = allCategories.stream().filter(c -> !c.getActive()).count();

        // Categories with products
        long withProducts = allCategories.stream()
                .filter(c -> productRepository.countByCategoryId(c.getId()) > 0)
                .count();

        // Empty categories
        long emptyCategories = allCategories.stream()
                .filter(c -> productRepository.countByCategoryId(c.getId()) == 0)
                .count();

        return new CategoryStatistics(totalCategories, activeCategories, inactiveCategories,
                                     withProducts, emptyCategories);
    }

    /**
     * Bulk activate categories
     */
    public int bulkActivate(List<Long> categoryIds) {
        int count = 0;
        for (Long id : categoryIds) {
            try {
                Category category = categoryRepository.findById(id).orElse(null);
                if (category != null && !category.getActive()) {
                    category.setActive(true);
                    categoryRepository.save(category);
                    count++;
                }
            } catch (Exception e) {
                log.error("Error activating category {}: {}", id, e.getMessage());
            }
        }
        log.info("Bulk activated {} categories", count);
        return count;
    }

    /**
     * Bulk deactivate categories
     */
    public int bulkDeactivate(List<Long> categoryIds) {
        int count = 0;
        for (Long id : categoryIds) {
            try {
                Category category = categoryRepository.findById(id).orElse(null);
                if (category != null && category.getActive()) {
                    category.setActive(false);
                    categoryRepository.save(category);
                    count++;
                }
            } catch (Exception e) {
                log.error("Error deactivating category {}: {}", id, e.getMessage());
            }
        }
        log.info("Bulk deactivated {} categories", count);
        return count;
    }

    /**
     * Bulk delete categories (only empty ones)
     */
    public int bulkDelete(List<Long> categoryIds) {
        int count = 0;
        for (Long id : categoryIds) {
            try {
                long productCount = productRepository.countByCategoryId(id);
                if (productCount == 0) {
                    categoryRepository.deleteById(id);
                    count++;
                } else {
                    log.warn("Cannot delete category {} with {} products", id, productCount);
                }
            } catch (Exception e) {
                log.error("Error deleting category {}: {}", id, e.getMessage());
            }
        }
        log.info("Bulk deleted {} categories", count);
        return count;
    }

    // Category Statistics DTO
    public static class CategoryStatistics {
        private final long totalCategories;
        private final long activeCategories;
        private final long inactiveCategories;
        private final long withProducts;
        private final long emptyCategories;

        public CategoryStatistics(long totalCategories, long activeCategories, long inactiveCategories,
                                 long withProducts, long emptyCategories) {
            this.totalCategories = totalCategories;
            this.activeCategories = activeCategories;
            this.inactiveCategories = inactiveCategories;
            this.withProducts = withProducts;
            this.emptyCategories = emptyCategories;
        }

        public long getTotalCategories() { return totalCategories; }
        public long getActiveCategories() { return activeCategories; }
        public long getInactiveCategories() { return inactiveCategories; }
        public long getWithProducts() { return withProducts; }
        public long getEmptyCategories() { return emptyCategories; }
    }
}
