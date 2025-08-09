package de.likeherotozero.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

/**
 * Handles post-login redirection based on user roles.
 * Scientists are redirected to the dashboard, all other users to the emissions page.
 * Implements Spring Security's AuthenticationSuccessHandler.
 */
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    /**
     * Called by Spring Security after a successful login.
     * Checks the user's roles and redirects accordingly.
     *
     * @param request       the HttpServletRequest
     * @param response      the HttpServletResponse
     * @param authentication the Authentication object containing user details
     * @throws IOException      if an input or output error occurs
     * @throws ServletException if a servlet error occurs
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // Extract all roles of the authenticated user
        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

        // Redirect scientists to their dashboard, others to the emissions page
        if (roles.contains("SCIENTIST") || roles.contains("SCIENTIST")) {
            response.sendRedirect("/scientist/dashboard");
        } else {
            response.sendRedirect("/emissions");
        }
    }
}