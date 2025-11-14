package g6.fashionFlex.service;

import g6.fashionFlex.entity.Role;
import g6.fashionFlex.entity.User;
import g6.fashionFlex.entity.User.UserStatus;
import g6.fashionFlex.repository.RoleRepository;
import g6.fashionFlex.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AdminUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Optional<User> getUserById(Integer userID) {
        return userRepository.findById(userID);
    }

    @Transactional
    public User toggleUserStatus(Integer userID) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getStatus() == UserStatus.active) {
            user.setStatus(UserStatus.inactive);
        } else {
            user.setStatus(UserStatus.active);
        }
        
        return userRepository.save(user);
    }

    @Transactional
    public User assignRole(Integer userID, Integer roleID) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Role role = roleRepository.findById(roleID)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        
        user.setRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public User removeRole(Integer userID) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setRole(null);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Integer userID) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
}

