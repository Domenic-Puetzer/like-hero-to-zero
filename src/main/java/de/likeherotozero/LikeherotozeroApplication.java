package de.likeherotozero;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Main application class for the Like Hero to Zero CO₂ emissions management system.
 * This Spring Boot application provides a platform for scientists to collaborate on
 * CO₂ emissions data collection, validation, and analysis.
 * 
 * Features:
 * - CO₂ emissions data management and visualization
 * - Scientist collaboration and peer-review workflow
 * - External API integration (Our World in Data)
 * - Role-based access control (Scientists and Admins)
 * - Data import/export capabilities
 * 
 * @author Domenic Puetzer
 * @version 1.0
 * @since 2025
 */
@SpringBootApplication
public class LikeherotozeroApplication {

	/**
	 * Main entry point for the Spring Boot application.
	 * Configures environment variables from .env file and starts the application context.
	 * 
	 * Environment variables loaded:
	 * - DB_SOURCE: Database connection URL
	 * - DB_USERNAME: Database username
	 * - DB_PASSWORD: Database password
	 * 
	 * @param args Command line arguments passed to the application
	 */
	public static void main(String[] args) {
		// Load environment variables from .env file (if present)
		// Missing .env file is ignored to support different deployment environments
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

		// Set database configuration as system properties for Spring Boot
		System.setProperty("DB_NAME", dotenv.get("DB_NAME"));
		System.setProperty("DB_USERNAME", dotenv.get("DB_USERNAME"));
		System.setProperty("DB_PASSWORD", dotenv.get("DB_PASSWORD"));

		// Start the Spring Boot application
		SpringApplication.run(LikeherotozeroApplication.class, args);
	}

}
