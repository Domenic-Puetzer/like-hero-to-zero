package de.likeherotozero.service;

import de.likeherotozero.model.User;
import de.likeherotozero.model.Role;
import de.likeherotozero.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service class for user management operations
 * Implements UserDetailsService for Spring Security integration
 */
@Service
@Transactional
public class UserService implements UserDetailsService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    /**
     * Load user by username for Spring Security authentication
     * @param username the username to load
     * @return UserDetails for authentication
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .authorities(user.getRole().getAuthority())
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(false)
            .build();
    }
    
    /**
     * Register a new user
     * @param username the username
     * @param email the email address
     * @param password the plain text password (will be encoded)
     * @param firstName the first name
     * @param lastName the last name
     * @param role the user role
     * @return the created user
     * @throws IllegalArgumentException if username or email already exists
     */
    public User registerUser(String username, String email, String password, 
                           String firstName, String lastName, Role role) {
        
        // Validate username uniqueness
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        
        // Validate email uniqueness
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }
        
        // Create new user
        User user = new User();
        user.setUsername(username.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }
    
    /**
     * Find user by username
     * @param username the username to search for
     * @return Optional containing the user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username);
    }
    
    /**
     * Find user by email
     * @param email the email to search for
     * @return Optional containing the user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }
    
    /**
     * Get all users
     * @return list of all users ordered by creation date
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAllOrderByCreatedAtDesc();
    }
    
    /**
     * Get users by role
     * @param role the role to filter by
     * @return list of users with the specified role
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }
    
    /**
     * Check if username is available
     * @param username the username to check
     * @return true if username is available
     */
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsernameIgnoreCase(username);
    }
    
    /**
     * Check if email is available
     * @param email the email to check
     * @return true if email is available
     */
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmailIgnoreCase(email);
    }
    
    /**
     * Update user password
     * @param username the username
     * @param newPassword the new plain text password
     * @return true if password was updated successfully
     */
    public boolean updatePassword(String username, String newPassword) {
        Optional<User> userOpt = userRepository.findByUsernameIgnoreCase(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return true;
        }
        return false;
    }
    
    /**
     * Delete user by ID
     * @param userId the user ID
     * @return true if user was deleted successfully
     */
    public boolean deleteUser(Long userId) {
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
            return true;
        }
        return false;
    }
    
    /**
     * Get user statistics
     * @return array with [total users, scientists, admins]
     */
    @Transactional(readOnly = true)
    public long[] getUserStatistics() {
        long totalUsers = userRepository.count();
        long scientists = userRepository.countByRole(Role.SCIENTIST);
        long admins = userRepository.countByRole(Role.ADMIN);
        
        return new long[]{totalUsers, scientists, admins};
    }
}
