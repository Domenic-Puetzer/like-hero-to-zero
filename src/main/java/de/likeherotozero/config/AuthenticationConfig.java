package de.likeherotozero.config;

import de.likeherotozero.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Authentication Configuration
 * Configures Spring Security authentication providers and managers.
 * Separated from SecurityConfig to prevent circular dependency issues
 * between authentication components and security configuration.
 */
@Configuration
public class AuthenticationConfig {
    
    /**
     * Creates and configures the AuthenticationManager bean.
     * The AuthenticationManager is responsible for processing authentication requests
     * and coordinating with various authentication providers.
     * @param config The authentication configuration provided by Spring Security
     * @return Configured AuthenticationManager instance
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    /**
     * Configures global authentication settings for the application.
     * Sets up the custom UserDetailsService and password encoder for user authentication.
     * This method is automatically called by Spring during application startup.
     * @param auth The AuthenticationManagerBuilder to configure
     * @param userService Custom UserDetailsService implementation for loading user data
     * @param passwordEncoder Password encoder for secure password verification
     * @throws Exception if authentication configuration fails
     */
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth, 
                              UserService userService, 
                              PasswordEncoder passwordEncoder) throws Exception {
        auth.userDetailsService(userService).passwordEncoder(passwordEncoder);
    }
}
