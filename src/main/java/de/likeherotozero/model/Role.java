package de.likeherotozero.model;

/**
 * User role enumeration for role-based access control
 * Defines the different access levels in the system
 */
public enum Role {
    /**
     * Regular scientist role - can upload and manage own data
     */
    SCIENTIST("ROLE_SCIENTIST", "Scientist"),
    
    /**
     * Administrator role - full system access
     */
    ADMIN("ROLE_ADMIN", "Administrator"),
    
    /**
     * Guest role - read-only access (future use)
     */
    GUEST("ROLE_GUEST", "Guest");
    
    private final String authority;
    private final String displayName;
    
    Role(String authority, String displayName) {
        this.authority = authority;
        this.displayName = displayName;
    }
    
    /**
     * Get Spring Security authority string
     * @return authority string (e.g., "ROLE_SCIENTIST")
     */
    public String getAuthority() {
        return authority;
    }
    
    /**
     * Get user-friendly display name
     * @return display name (e.g., "Scientist")
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if this role has admin privileges
     * @return true if role is ADMIN
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }
    
    /**
     * Check if this role can manage data
     * @return true if role is SCIENTIST or ADMIN
     */
    public boolean canManageData() {
        return this == SCIENTIST || this == ADMIN;
    }
}
