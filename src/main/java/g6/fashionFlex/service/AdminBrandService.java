package g6.fashionFlex.service;

import g6.fashionFlex.dto.BrandDTO;
import g6.fashionFlex.entity.Brand;
import g6.fashionFlex.entity.Product;
import g6.fashionFlex.repository.BrandRepository;
import g6.fashionFlex.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AdminBrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    @Autowired(required = false)
    private FileUploadService fileUploadService;

    /**
     * Get all brands with product count
     */
    public List<BrandDTO> getAllBrands() {
        return brandRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get brand by ID
     */
    public BrandDTO getBrandById(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found with id: " + id));
        return convertToDTO(brand);
    }

    /**
     * Create new brand
     */
    public BrandDTO createBrand(BrandDTO brandDTO, MultipartFile logoFile) throws IOException {
        // Check if brand name already exists
        if (brandRepository.existsByName(brandDTO.getName())) {
            throw new RuntimeException("Brand with name '" + brandDTO.getName() + "' already exists");
        }

        Brand brand = convertToEntity(brandDTO);

        // Handle logo file upload
        if (logoFile != null && !logoFile.isEmpty() && fileUploadService != null) {
            String logoUrl = fileUploadService.uploadFile(logoFile, "brands");
            brand.setLogoUrl(logoUrl);
        }

        Brand savedBrand = brandRepository.save(brand);
        return convertToDTO(savedBrand);
    }

    /**
     * Update existing brand
     */
    public BrandDTO updateBrand(Long id, BrandDTO brandDTO, MultipartFile logoFile) throws IOException {
        Brand existingBrand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found with id: " + id));

        // Check if name is being changed to an existing name
        if (!existingBrand.getName().equals(brandDTO.getName())
            && brandRepository.existsByName(brandDTO.getName())) {
            throw new RuntimeException("Brand with name '" + brandDTO.getName() + "' already exists");
        }

        // Update basic fields
        existingBrand.setName(brandDTO.getName());
        existingBrand.setDescription(brandDTO.getDescription());
        existingBrand.setActive(brandDTO.getActive());

        // Handle logo file upload
        if (logoFile != null && !logoFile.isEmpty() && fileUploadService != null) {
            // Delete old logo if exists
            if (existingBrand.getLogoUrl() != null && !existingBrand.getLogoUrl().isEmpty()) {
                try {
                    fileUploadService.deleteFile(existingBrand.getLogoUrl());
                } catch (Exception e) {
                    // Log error but continue with update
                    System.err.println("Failed to delete old logo: " + e.getMessage());
                }
            }

            // Upload new logo
            String logoUrl = fileUploadService.uploadFile(logoFile, "brands");
            existingBrand.setLogoUrl(logoUrl);
        }

        Brand updatedBrand = brandRepository.save(existingBrand);
        return convertToDTO(updatedBrand);
    }

    /**
     * Delete brand if it has no products
     */
    public void deleteBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found with id: " + id));

        // Check if brand has products
        long productCount = productRepository.countByBrandId(id);
        if (productCount > 0) {
            throw new RuntimeException("Cannot delete brand. It has " + productCount + " associated products.");
        }

        // Delete logo file if exists
        if (brand.getLogoUrl() != null && !brand.getLogoUrl().isEmpty() && fileUploadService != null) {
            try {
                fileUploadService.deleteFile(brand.getLogoUrl());
            } catch (Exception e) {
                // Log error but continue with deletion
                System.err.println("Failed to delete logo: " + e.getMessage());
            }
        }

        brandRepository.delete(brand);
    }

    /**
     * Toggle brand active status
     */
    public BrandDTO toggleBrandStatus(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found with id: " + id));

        brand.setActive(!brand.getActive());
        Brand updatedBrand = brandRepository.save(brand);

        return convertToDTO(updatedBrand);
    }

    /**
     * Convert Brand entity to DTO
     */
    private BrandDTO convertToDTO(Brand brand) {
        BrandDTO dto = new BrandDTO();
        dto.setId(brand.getId());
        dto.setName(brand.getName());
        dto.setDescription(brand.getDescription());
        dto.setLogoUrl(brand.getLogoUrl());
        dto.setActive(brand.getActive());

        // Count products for this brand
        int productCount = (int) productRepository.countByBrandId(brand.getId());
        dto.setProductCount(productCount);

        return dto;
    }

    /**
     * Convert DTO to Brand entity
     */
    private Brand convertToEntity(BrandDTO dto) {
        Brand brand = new Brand();
        if (dto.getId() != null) {
            brand.setId(dto.getId());
        }
        brand.setName(dto.getName());
        brand.setDescription(dto.getDescription());
        brand.setLogoUrl(dto.getLogoUrl());
        brand.setActive(dto.getActive() != null ? dto.getActive() : true);

        return brand;
    }

    /**
     * Get paginated brands
     */
    public Page<BrandDTO> getAllBrands(Pageable pageable) {
        return brandRepository.findAll(pageable).map(this::convertToDTO);
    }

    /**
     * Search brands with filters
     */
    public Page<BrandDTO> searchBrands(String keyword, Boolean active, Pageable pageable) {
        List<Brand> allBrands = brandRepository.findAll();

        // Apply filters
        List<Brand> filteredBrands = allBrands.stream()
                .filter(brand -> {
                    // Keyword filter
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String lowerKeyword = keyword.toLowerCase();
                        boolean matchesName = brand.getName().toLowerCase().contains(lowerKeyword);
                        boolean matchesDesc = brand.getDescription() != null &&
                                             brand.getDescription().toLowerCase().contains(lowerKeyword);
                        if (!matchesName && !matchesDesc) {
                            return false;
                        }
                    }

                    // Active status filter
                    if (active != null && !brand.getActive().equals(active)) {
                        return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());

        // Convert to Page
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredBrands.size());
        List<BrandDTO> pageContent = filteredBrands.subList(start, end).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(
                pageContent, pageable, filteredBrands.size());
    }

    /**
     * Get brand statistics
     */
    public BrandStatistics getStatistics() {
        List<Brand> allBrands = brandRepository.findAll();

        long totalBrands = allBrands.size();
        long activeBrands = allBrands.stream().filter(Brand::getActive).count();
        long inactiveBrands = allBrands.stream().filter(b -> !b.getActive()).count();

        // Brands with products
        long withProducts = allBrands.stream()
                .filter(b -> productRepository.countByBrandId(b.getId()) > 0)
                .count();

        // Brands without logo
        long withoutLogo = allBrands.stream()
                .filter(b -> b.getLogoUrl() == null || b.getLogoUrl().trim().isEmpty())
                .count();

        return new BrandStatistics(totalBrands, activeBrands, inactiveBrands,
                                   withProducts, withoutLogo);
    }

    /**
     * Bulk activate brands
     */
    public int bulkActivate(List<Long> brandIds) {
        int count = 0;
        for (Long id : brandIds) {
            try {
                Brand brand = brandRepository.findById(id).orElse(null);
                if (brand != null && !brand.getActive()) {
                    brand.setActive(true);
                    brandRepository.save(brand);
                    count++;
                }
            } catch (Exception e) {
                log.error("Error activating brand {}: {}", id, e.getMessage());
            }
        }
        log.info("Bulk activated {} brands", count);
        return count;
    }

    /**
     * Bulk deactivate brands
     */
    public int bulkDeactivate(List<Long> brandIds) {
        int count = 0;
        for (Long id : brandIds) {
            try {
                Brand brand = brandRepository.findById(id).orElse(null);
                if (brand != null && brand.getActive()) {
                    brand.setActive(false);
                    brandRepository.save(brand);
                    count++;
                }
            } catch (Exception e) {
                log.error("Error deactivating brand {}: {}", id, e.getMessage());
            }
        }
        log.info("Bulk deactivated {} brands", count);
        return count;
    }

    /**
     * Bulk delete brands (only empty ones)
     */
    public int bulkDelete(List<Long> brandIds) {
        int count = 0;
        for (Long id : brandIds) {
            try {
                long productCount = productRepository.countByBrandId(id);
                if (productCount == 0) {
                    Brand brand = brandRepository.findById(id).orElse(null);
                    if (brand != null) {
                        // Delete logo if exists
                        if (brand.getLogoUrl() != null && !brand.getLogoUrl().isEmpty() && fileUploadService != null) {
                            try {
                                fileUploadService.deleteFile(brand.getLogoUrl());
                            } catch (Exception e) {
                                log.warn("Failed to delete logo for brand {}: {}", id, e.getMessage());
                            }
                        }
                        brandRepository.deleteById(id);
                        count++;
                    }
                } else {
                    log.warn("Cannot delete brand {} with {} products", id, productCount);
                }
            } catch (Exception e) {
                log.error("Error deleting brand {}: {}", id, e.getMessage());
            }
        }
        log.info("Bulk deleted {} brands", count);
        return count;
    }

    // Brand Statistics DTO
    public static class BrandStatistics {
        private final long totalBrands;
        private final long activeBrands;
        private final long inactiveBrands;
        private final long withProducts;
        private final long withoutLogo;

        public BrandStatistics(long totalBrands, long activeBrands, long inactiveBrands,
                              long withProducts, long withoutLogo) {
            this.totalBrands = totalBrands;
            this.activeBrands = activeBrands;
            this.inactiveBrands = inactiveBrands;
            this.withProducts = withProducts;
            this.withoutLogo = withoutLogo;
        }

        public long getTotalBrands() { return totalBrands; }
        public long getActiveBrands() { return activeBrands; }
        public long getInactiveBrands() { return inactiveBrands; }
        public long getWithProducts() { return withProducts; }
        public long getWithoutLogo() { return withoutLogo; }
    }
}
