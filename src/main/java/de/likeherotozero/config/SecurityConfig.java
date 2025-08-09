package de.likeherotozero.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security Configuration
 * Defines security rules, authentication, and authorization for the application.
 * Configures access controls for different user roles and public endpoints.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configures the security filter chain for HTTP requests.
     * Defines which endpoints are public, which require authentication,
     * and what roles are needed for protected resources.
     * @param http HttpSecurity configuration object
     * @return Configured SecurityFilterChain
     * @throws Exception if security configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - accessible without authentication
                .requestMatchers("/", "/emissions", "/api/emissions/**").permitAll()
                .requestMatchers("/login", "/register").permitAll()
                .requestMatchers("/css/**", "/js/**", "/img/**", "/json/**").permitAll()
                // Role-based access control
                .requestMatchers("/scientist/**").hasRole("SCIENTIST")  // Scientist dashboard and features
                .requestMatchers("/admin/**").hasRole("ADMIN")          // Admin panel access 
                .anyRequest().authenticated()                           // All other requests require authentication
            )
            .formLogin(form -> form
                .loginPage("/login")                                    // Custom login page
                .successHandler(new LoginSuccessHandler())       // Redirect after successful login
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")                                  // Redirect to home after logout
                .permitAll()
            );
        return http.build();
    }

    /**
     * Provides password encoder bean for secure password hashing.
     * Uses BCrypt algorithm for strong password encryption.
     * @return BCryptPasswordEncoder instance for password hashing
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}