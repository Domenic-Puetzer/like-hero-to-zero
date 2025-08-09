package de.likeherotozero.config;

import de.likeherotozero.model.EmissionData;
import de.likeherotozero.model.Role;
import de.likeherotozero.repository.EmissionDataRepository;
import de.likeherotozero.service.EmissionDataService;
import de.likeherotozero.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Data Loader Component
 * Initializes the application with demo users and emission data on startup.
 * Attempts to load data from external API first, falls back to sample data if needed.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private final EmissionDataRepository repository;
    private final EmissionDataService emissionDataService;
    private final UserService userService;

    public DataLoader(EmissionDataRepository repository, 
                     EmissionDataService emissionDataService,
                     UserService userService) {
        this.repository = repository;
        this.emissionDataService = emissionDataService;
        this.userService = userService;
    }

    @Override
    public void run(String... args) throws Exception {
        // Load demo users first for testing purposes
        loadDemoUsers();

        // Load demo emission data created by demo users
        loadDemoDataByUser();
        
        // Load emission data if database is empty
        if (repository.count() == 0) {
            System.out.println("No data found - attempting World Bank import...");
            
            try {
                // First attempt: Load data from external World Bank API
                int imported = emissionDataService.importWorldBankData();
                if (imported > 0) {
                    System.out.println(imported + " World Bank records loaded on startup!");
                    return; // Success - no fallback data needed
                }
            } catch (Exception e) {
                System.out.println("World Bank import failed: " + e.getMessage());
                System.out.println("Loading fallback data...");
            }
            
            // Fallback: Load sample data only if World Bank import fails
            loadFallbackData();
        } else {
            System.out.println("Data already exists (" + repository.count() + " entries).");
        }
    }

    /**
     * Creates demo user accounts for testing and development purposes.
     * Creates a scientist user and an admin user with predefined credentials.
     */
    private void loadDemoUsers() {
        try {
            // Create demo scientist user for testing
            if (!userService.findByUsername("scientist").isPresent()) {
                userService.registerUser(
                    "scientist", 
                    "scientist@example.com", 
                    "password", 
                    "Demo", 
                    "Scientist", 
                    Role.SCIENTIST
                );
                System.out.println("Demo Scientist User created (Username: scientist, Password: password)");
            }
            
            // Create demo admin user for administration
            if (!userService.findByUsername("admin").isPresent()) {
                userService.registerUser(
                    "admin", 
                    "admin@example.com", 
                    "admin123", 
                    "Demo", 
                    "Administrator", 
                    Role.ADMIN
                );
                System.out.println("Demo Admin User created (Username: admin, Password: admin123)");
            }
        } catch (Exception e) {
            System.out.println("Error creating demo users: " + e.getMessage());
        }
    }

    /**
     * Loads example emission data assigned to a specific user (e.g. scientist).
     * Adds three emission entries for 2024, all attributed to the scientist user.
     */
    private void loadDemoDataByUser() {
        boolean exists = repository.existsByUploadedByAndYear("scientist", 2024);
        if (exists) {
            System.out.println("Demo emissions for scientist user in 2024 already exist. Skipping creation.");
            return;
        }

        userService.findByUsername("scientist").ifPresent(scientistUser -> {
            EmissionData emission1 = new EmissionData("Austria", "AUT", 2024, 67000.00);
            emission1.setUploadedBy("scientist");
            emission1.setDataSource("Demo Data");
            repository.save(emission1);

            EmissionData emission2 = new EmissionData("Switzerland", "CHE", 2024, 41000.00);
            emission2.setUploadedBy("scientist");
            emission2.setDataSource("Demo Data");
            repository.save(emission2);

            EmissionData emission3 = new EmissionData("Netherlands", "NLD", 2024, 95000.00);
            emission3.setUploadedBy("scientist");
            emission3.setDataSource("Demo Data");
            repository.save(emission3);

            System.out.println("3 example emissions for 2024 created by scientist user (Austria, Switzerland, Netherlands)");
        });
    }

    /**
     * Loads basic fallback data when external API is unavailable.
     * Provides minimal sample data for testing and demonstration purposes.
     */
    private void loadFallbackData() {
        repository.save(new EmissionData("Germany", "DEU", 2020, 644661.58));
        repository.save(new EmissionData("Germany", "DEU", 2019, 709714.22));
        repository.save(new EmissionData("United States", "USA", 2020, 4713065.11));
        repository.save(new EmissionData("China", "CHN", 2020, 10668417.46));
        repository.save(new EmissionData("France", "FRA", 2020, 298407.44));
        
        System.out.println("Fallback data loaded (5 test entries)");
    }
}