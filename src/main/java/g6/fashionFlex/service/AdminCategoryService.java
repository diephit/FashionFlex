package g6.fashionFlex.service;

import g6.fashionFlex.entity.Category;
import g6.fashionFlex.entity.Category.CategoryStatus;
import g6.fashionFlex.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AdminCategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

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
        
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Integer categoryID) {
        Category category = categoryRepository.findById(categoryID)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        // Check if category has subcategories
        List<Category> subCategories = categoryRepository.findByParentCategoryCategoryID(categoryID);
        if (!subCategories.isEmpty()) {
            throw new RuntimeException("Cannot delete category with subcategories");
        }
        
        categoryRepository.delete(category);
    }

    @Transactional
    public Category toggleCategoryStatus(Integer categoryID) {
        Category category = categoryRepository.findById(categoryID)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        if (category.getStatus() == CategoryStatus.active) {
            category.setStatus(CategoryStatus.inactive);
        } else {
            category.setStatus(CategoryStatus.active);
        }
        
        return categoryRepository.save(category);
    }
}

