package de.likeherotozero.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * User Entity
 * Represents system users with authentication credentials and role-based access control.
 * Used for scientist collaboration in the CO2 emission data management system.
 */
@Entity
@Table(name = "users")
public class User {
    
    /**
     * Unique identifier for the user
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Unique username for authentication (must be unique across the system)
     */
    @Column(name = "username", unique = true, nullable = false)
    private String username;
    
    /**
     * Email address for user identification and communication (must be unique)
     */
    @Column(name = "email", unique = true, nullable = false)
    private String email;
    
    /**
     * Encrypted password for authentication (stored as bcrypt hash)
     */
    @Column(name = "password", nullable = false)
    private String password;
    
    /**
     * User role determining access permissions (SCIENTIST, ADMIN, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role = Role.SCIENTIST;
    
    /**
     * User's first name for display purposes
     */
    @Column(name = "first_name")
    private String firstName;
    
    /**
     * User's last name for display purposes
     */
    @Column(name = "last_name")
    private String lastName;
    
    /**
     * Scientific organization or institution affiliation (optional)
     */
    @Column(name = "organization")
    private String organization;
    
    /**
     * Whether the user account is active and can authenticate
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
    
    /**
     * Timestamp when the user account was created
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    /**
     * Timestamp of the user's last successful login (null if never logged in)
     */
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    /**
     * Default constructor for JPA
     */
    public User() {}
    
    /**
     * Constructor for creating a new user with essential information
     * @param username Unique username for authentication
     * @param email User's email address
     * @param password Encrypted password
     * @param role User's role determining access permissions
     */
    public User(String username, String email, String password, Role role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    
    /**
     * Gets the unique identifier of the user
     * @return The user ID
     */
    public Long getId() {
        return id;
    }
    
    /**
     * Sets the unique identifier of the user
     * @param id The user ID
     */
    public void setId(Long id) {
        this.id = id;
    }
    
    /**
     * Gets the username for authentication
     * @return The username
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Sets the username for authentication
     * @param username The username (must be unique)
     */
    public void setUsername(String username) {
        this.username = username;
    }
    
    /**
     * Gets the user's email address
     * @return The email address
     */
    public String getEmail() {
        return email;
    }
    
    /**
     * Sets the user's email address
     * @param email The email address (must be unique)
     */
    public void setEmail(String email) {
        this.email = email;
    }
    
    /**
     * Gets the encrypted password
     * @return The encrypted password hash
     */
    public String getPassword() {
        return password;
    }
    
    /**
     * Sets the encrypted password
     * @param password The encrypted password hash
     */
    public void setPassword(String password) {
        this.password = password;
    }
    
    /**
     * Gets the user's role
     * @return The user role (SCIENTIST, ADMIN, etc.)
     */
    public Role getRole() {
        return role;
    }
    
    /**
     * Sets the user's role
     * @param role The user role determining access permissions
     */
    public void setRole(Role role) {
        this.role = role;
    }
    
    /**
     * Gets the user's first name
     * @return The first name
     */
    public String getFirstName() {
        return firstName;
    }
    
    /**
     * Sets the user's first name
     * @param firstName The first name
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    /**
     * Gets the user's last name
     * @return The last name
     */
    public String getLastName() {
        return lastName;
    }
    
    /**
     * Sets the user's last name
     * @param lastName The last name
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    /**
     * Gets the user's organization or institution affiliation
     * @return The organization name
     */
    public String getOrganization() {
        return organization;
    }
    
    /**
     * Sets the user's organization or institution affiliation
     * @param organization The organization name
     */
    public void setOrganization(String organization) {
        this.organization = organization;
    }
    
    /**
     * Gets whether the user account is enabled
     * @return True if the account is active and can authenticate
     */
    public Boolean getEnabled() {
        return enabled;
    }
    
    /**
     * Sets whether the user account is enabled
     * @param enabled True to enable the account, false to disable
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Gets the timestamp when the user account was created
     * @return The account creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Sets the timestamp when the user account was created
     * @param createdAt The account creation timestamp
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * Gets the timestamp of the user's last successful login
     * @return The last login timestamp (null if never logged in)
     */
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
    
    /**
     * Sets the timestamp of the user's last successful login
     * @param lastLogin The last login timestamp
     */
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    // Utility methods with documentation
    
    /**
     * Gets the user's full name by combining first and last name
     * @return The full name if both first and last names are available, otherwise the username
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return username;
    }
    
    /**
     * Checks if the user has administrator privileges
     * @return True if the user's role is ADMIN
     */
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
    
    /**
     * Checks if the user can perform scientist operations
     * @return True if the user's role is SCIENTIST or ADMIN
     */
    public boolean isScientist() {
        return role == Role.SCIENTIST || role == Role.ADMIN;
    }
}
