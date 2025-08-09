package de.likeherotozero.controller;

import de.likeherotozero.model.EmissionData;
import de.likeherotozero.service.EmissionDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * Controller for public-facing pages and API endpoints.
 * Handles requests that don't require authentication, including the emissions map
 * and country-specific emission data queries for visualization.
 */
@Controller
public class PublicController {

    private final EmissionDataService emissionDataService;

    public PublicController(EmissionDataService emissionDataService) {
        this.emissionDataService = emissionDataService;
    }

    /**
     * Serves the application home page.
     * @return Template name for the index page
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * Displays the interactive emissions map with optional country filtering.
     * Loads all emission data for map visualization and provides filtered data
     * when a specific country is requested.
     * @param countryName Optional parameter to filter data for a specific country
     * @param model Spring Model to pass data to the view
     * @return Template name for the emissions map page
     */
    @GetMapping("/emissions")
    public String showEmissionsMap(@RequestParam(required = false) String countryName, Model model) {
        System.out.println("=== EMISSIONS MAP REQUEST ===");
        System.out.println("Country parameter: " + countryName);
        
        try {
            // Load all emission data from database (including scientist-uploaded data)
            List<EmissionData> allEmissions = emissionDataService.getAllEmissionsSorted();
            
            // Debug: Count scientist-contributed data
            long scientistDataCount = allEmissions.stream()
                .filter(data -> data.getDataSource() != null && data.getDataSource().contains("Scientist"))
                .count();
            
            System.out.println("Total emissions loaded from database: " + allEmissions.size());
            System.out.println("Scientist data count: " + scientistDataCount);
            
            // Debug: Log sample data for verification
            allEmissions.stream().limit(5).forEach(data -> {
                System.out.println("Sample data: " + data.getCountryName() + " (" + data.getYear() + ") - Source: " + data.getDataSource());
            });
            
            model.addAttribute("allEmissions", allEmissions);
            
            // Handle country-specific filtering if requested
            if (countryName != null && !countryName.trim().isEmpty()) {
                System.out.println("Searching for country: " + countryName);
                List<EmissionData> countryEmissions = emissionDataService.getEmissionsByCountry(countryName);
                model.addAttribute("emissions", countryEmissions);
                model.addAttribute("selectedCountry", countryName);
            } else {
                model.addAttribute("selectedCountry", null);
            }
            
            return "emissions";
            
        } catch (Exception e) {
            System.err.println("ERROR in showEmissionsMap: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * AJAX endpoint that returns all emission data as JSON.
     * Used by the frontend map visualization to dynamically load data.
     * Includes both API-imported and scientist-uploaded data.
     * @return JSON response with all emission data records
     */
    @GetMapping("/api/emissions/all")
    @ResponseBody
    public ResponseEntity<List<EmissionData>> getAllEmissions() {
        try {
            System.out.println("=== ALL EMISSIONS API REQUEST ===");
            
            List<EmissionData> allEmissions = emissionDataService.getAllEmissionsSorted();
            
            System.out.println("Returning " + allEmissions.size() + " emission records (including scientist data)");
            return ResponseEntity.ok(allEmissions);
            
        } catch (Exception e) {
            System.err.println("ERROR in getAllEmissions API: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * AJAX endpoint that returns emission data for a specific country.
     * Used by the frontend to display detailed country information and charts.
     * Returns data sorted by year (newest first) for visualization purposes.
     * @param countryName The name of the country to retrieve data for
     * @return JSON response with emission data for the specified country, or 404 if not found
     */
    @GetMapping("/api/emissions/country/{countryName}")
    @ResponseBody
    public ResponseEntity<List<EmissionData>> getEmissionsByCountry(@PathVariable String countryName) {
        try {
            System.out.println("=== COUNTRY EMISSIONS API REQUEST ===");
            System.out.println("Loading data for country: " + countryName);
            
            List<EmissionData> emissions = emissionDataService.getEmissionsByCountry(countryName);
            
            if (emissions.isEmpty()) {
                System.out.println("No data found for country: " + countryName);
                return ResponseEntity.notFound().build();
            }
            
            System.out.println("Found " + emissions.size() + " records for " + countryName);
            return ResponseEntity.ok(emissions);
            
        } catch (Exception e) {
            System.err.println("ERROR in getEmissionsByCountry: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}