package de.likeherotozero.controller;

import de.likeherotozero.model.Role;
import de.likeherotozero.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Authentication Controller
 * Handles user authentication, registration, and login/logout functionality.
 * Provides endpoints for user registration form display and processing.
 */
@Controller
public class AuthController {

    private final UserService userService;

    /**
     * Constructor injection for UserService dependency.
     * @param userService Service for user management operations
     */
    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Displays the login page.
     * @return Login template view name
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Displays the user registration form.
     * Provides available user roles for selection.
     * @param model Model object to pass data to the view
     * @return Registration template view name
     */
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("roles", Role.values());
        return "register";
    }

    /**
     * Processes user registration form submission.
     * Validates input data, creates new user account, and handles success/error scenarios.
     * @param username Desired username for the new account
     * @param email User's email address
     * @param password User's chosen password
     * @param confirmPassword Password confirmation for validation
     * @param firstName User's first name
     * @param lastName User's last name
     * @param role Selected user role (SCIENTIST, ADMIN, etc.)
     * @param redirectAttributes Attributes for flash messages on redirect
     * @return Redirect to appropriate page based on registration result
     */
    @PostMapping("/register")
    public String register(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam Role role,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Validate password confirmation
            if (!password.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Passwörter stimmen nicht überein");
                return "redirect:/register";
            }
            
            // Validate password strength
            if (password.length() < 6) {
                redirectAttributes.addFlashAttribute("error", "Passwort muss mindestens 6 Zeichen lang sein");
                return "redirect:/register";
            }
            
            // Register user
            userService.registerUser(username, email, password, firstName, lastName, role);
            
            redirectAttributes.addFlashAttribute("success", 
                "Registrierung erfolgreich! Sie können sich jetzt mit Ihren Zugangsdaten anmelden.");
            return "redirect:/login";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Registrierung fehlgeschlagen. Bitte versuchen Sie es erneut.");
            return "redirect:/register";
        }
    }
}