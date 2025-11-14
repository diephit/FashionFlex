package g6.fashionFlex.service;

import g6.fashionFlex.entity.Customer;
import g6.fashionFlex.entity.MembershipHistory;
import g6.fashionFlex.entity.MembershipLevel;
import g6.fashionFlex.repository.CustomerRepository;
import g6.fashionFlex.repository.MembershipHistoryRepository;
import g6.fashionFlex.repository.MembershipLevelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AdminMembershipService {

    @Autowired
    private MembershipLevelRepository membershipLevelRepository;

    @Autowired
    private MembershipHistoryRepository membershipHistoryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    // Membership Level Management
    public List<MembershipLevel> getAllMembershipLevels() {
        return membershipLevelRepository.findAllOrderByMinSpentAsc();
    }

    public Optional<MembershipLevel> getMembershipLevelById(Integer levelID) {
        return membershipLevelRepository.findById(levelID);
    }

    @Transactional
    public MembershipLevel createMembershipLevel(MembershipLevel level) {
        return membershipLevelRepository.save(level);
    }

    @Transactional
    public MembershipLevel updateMembershipLevel(Integer levelID, MembershipLevel levelData) {
        MembershipLevel level = membershipLevelRepository.findById(levelID)
                .orElseThrow(() -> new RuntimeException("Membership level not found"));
        
        level.setLevelName(levelData.getLevelName());
        level.setMinSpent(levelData.getMinSpent());
        level.setDiscountRate(levelData.getDiscountRate());
        level.setBonusRate(levelData.getBonusRate());
        level.setDescription(levelData.getDescription());
        
        return membershipLevelRepository.save(level);
    }

    @Transactional
    public void deleteMembershipLevel(Integer levelID) {
        MembershipLevel level = membershipLevelRepository.findById(levelID)
                .orElseThrow(() -> new RuntimeException("Membership level not found"));
        membershipLevelRepository.delete(level);
    }

    // Membership History
    public List<MembershipHistory> getMembershipHistory(Integer customerID) {
        return membershipHistoryRepository.findByCustomerOrderByChangedAtDesc(customerID);
    }

    public Page<MembershipHistory> getAllMembershipHistory(Pageable pageable) {
        return membershipHistoryRepository.findAll(pageable);
    }

    // Points Management
    @Transactional
    public Customer updateLoyaltyPoints(Integer customerID, Integer points) {
        Customer customer = customerRepository.findById(customerID)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        customer.setLoyaltyPoints(points);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer addLoyaltyPoints(Integer customerID, Integer pointsToAdd) {
        Customer customer = customerRepository.findById(customerID)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() + pointsToAdd);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer deductLoyaltyPoints(Integer customerID, Integer pointsToDeduct) {
        Customer customer = customerRepository.findById(customerID)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        int newPoints = Math.max(0, customer.getLoyaltyPoints() - pointsToDeduct);
        customer.setLoyaltyPoints(newPoints);
        return customerRepository.save(customer);
    }

    // Level Change Tracking
    @Transactional
    public MembershipHistory recordLevelChange(Integer customerID, 
                                              Integer oldLevelID, 
                                              Integer newLevelID, 
                                              String note) {
        Customer customer = customerRepository.findById(customerID)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        MembershipHistory history = new MembershipHistory();
        history.setCustomer(customer);
        
        if (oldLevelID != null) {
            MembershipLevel oldLevel = membershipLevelRepository.findById(oldLevelID)
                    .orElse(null);
            history.setOldLevel(oldLevel);
        }
        
        if (newLevelID != null) {
            MembershipLevel newLevel = membershipLevelRepository.findById(newLevelID)
                    .orElse(null);
            history.setNewLevel(newLevel);
        }
        
        history.setNote(note);
        return membershipHistoryRepository.save(history);
    }
}

