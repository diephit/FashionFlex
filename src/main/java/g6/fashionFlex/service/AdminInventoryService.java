package g6.fashionFlex.service;

import g6.fashionFlex.entity.Admin;
import g6.fashionFlex.entity.ProductVariant;
import g6.fashionFlex.entity.Stock;
import g6.fashionFlex.entity.Stock.ChangeType;
import g6.fashionFlex.repository.AdminRepository;
import g6.fashionFlex.repository.ProductVariantRepository;
import g6.fashionFlex.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminInventoryService {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private AdminRepository adminRepository;

    public List<Stock> getStockHistory(Integer variantID) {
        return stockRepository.findByVariantOrderByUpdatedAtDesc(variantID);
    }

    public Page<Stock> getAllStockHistory(Pageable pageable) {
        return stockRepository.findAll(pageable);
    }

    public Stock getLatestStock(Integer variantID) {
        return stockRepository.findLatestByVariant(variantID);
    }

    public Integer getCurrentStockQuantity(Integer variantID) {
        Stock latestStock = stockRepository.findLatestByVariant(variantID);
        return latestStock != null ? latestStock.getCurrentQuantity() : 0;
    }

    @Transactional
    public Stock importStock(Integer variantID, Integer quantity, Integer adminID, String note) {
        ProductVariant variant = variantRepository.findById(variantID)
                .orElseThrow(() -> new RuntimeException("Variant not found"));
        
        Admin admin = adminRepository.findById(adminID)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        Integer currentQuantity = getCurrentStockQuantity(variantID);
        Integer newQuantity = currentQuantity + quantity;
        
        Stock stock = new Stock();
        stock.setVariant(variant);
        stock.setChangeAmount(quantity);
        stock.setCurrentQuantity(newQuantity);
        stock.setChangeType(ChangeType.import_);
        stock.setAdmin(admin);
        stock.setNote(note);
        
        return stockRepository.save(stock);
    }

    @Transactional
    public Stock exportStock(Integer variantID, Integer quantity, Integer adminID, String note) {
        ProductVariant variant = variantRepository.findById(variantID)
                .orElseThrow(() -> new RuntimeException("Variant not found"));
        
        Admin admin = adminRepository.findById(adminID)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        Integer currentQuantity = getCurrentStockQuantity(variantID);
        
        if (currentQuantity < quantity) {
            throw new RuntimeException("Insufficient stock. Current: " + currentQuantity + ", Requested: " + quantity);
        }
        
        Integer newQuantity = currentQuantity - quantity;
        
        Stock stock = new Stock();
        stock.setVariant(variant);
        stock.setChangeAmount(quantity);
        stock.setCurrentQuantity(newQuantity);
        stock.setChangeType(ChangeType.export);
        stock.setAdmin(admin);
        stock.setNote(note);
        
        return stockRepository.save(stock);
    }

    @Transactional
    public Stock adjustStock(Integer variantID, Integer newQuantity, Integer adminID, String note) {
        ProductVariant variant = variantRepository.findById(variantID)
                .orElseThrow(() -> new RuntimeException("Variant not found"));
        
        Admin admin = adminRepository.findById(adminID)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        Integer currentQuantity = getCurrentStockQuantity(variantID);
        Integer changeAmount = newQuantity - currentQuantity;
        
        Stock stock = new Stock();
        stock.setVariant(variant);
        stock.setChangeAmount(Math.abs(changeAmount));
        stock.setCurrentQuantity(newQuantity);
        stock.setChangeType(ChangeType.adjust);
        stock.setAdmin(admin);
        stock.setNote(note);
        
        return stockRepository.save(stock);
    }
}

