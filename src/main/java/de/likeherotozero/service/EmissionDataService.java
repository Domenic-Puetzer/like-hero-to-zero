package de.likeherotozero.service;

import de.likeherotozero.model.EmissionData;
import de.likeherotozero.model.EditRequest;
import de.likeherotozero.repository.EmissionDataRepository;
import de.likeherotozero.repository.EditRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Service class for managing CO₂ emission data operations.
 * Handles CRUD operations, caching, API integration, and data validation.
 * Provides both database and external API data access with intelligent caching.
 */
@Service
public class EmissionDataService {

    private final EmissionDataRepository repository;
    private final EditRequestRepository editRequestRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // Manual cache implementation for performance optimization
    private List<EmissionData> cachedAllData = null;
    private long lastCacheTime = 0;
    private static final long CACHE_DURATION = 30 * 60 * 1000; // 30 minutes cache duration

    public EmissionDataService(EmissionDataRepository repository, EditRequestRepository editRequestRepository) {
        this.repository = repository;
        this.editRequestRepository = editRequestRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    // ========================================
    // CORE DATA RETRIEVAL METHODS
    // ========================================

    /**
     * Retrieves all emission data with intelligent caching strategy.
     * First attempts to load from external API, falls back to database if API is unavailable.
     * Cache expires after 30 minutes to ensure data freshness.
     * @return List of all emission data records
     */
    public List<EmissionData> getAllEmissions() {
        long currentTime = System.currentTimeMillis();
        
        // Check if cache is valid and not expired
        if (cachedAllData != null && (currentTime - lastCacheTime) < CACHE_DURATION) {
            System.out.println("CACHE HIT - Returning " + cachedAllData.size() + " records from cache");
            return cachedAllData;
        }
        
        System.out.println("CACHE MISS - Loading fresh data...");
        
        // Primary data source: External API
        List<EmissionData> apiData = loadAllFromAPI();
        if (!apiData.isEmpty()) {
            cachedAllData = apiData;
            lastCacheTime = currentTime;
            System.out.println(apiData.size() + " records loaded from API and cached");
            return apiData;
        }
        
        // Fallback: Database when API is unavailable
        System.out.println("API unavailable - using database fallback");
        List<EmissionData> dbData = repository.findAllByOrderByCountryNameAscYearDesc();
        cachedAllData = dbData;
        lastCacheTime = currentTime;
        System.out.println(dbData.size() + " records loaded from database and cached");
        return dbData;
    }

    /**
     * Filters emission data by country name from database.
     * Includes all data sources (scientist uploads, API imports, etc.)
     * @param countryName The name of the country to filter by (case-insensitive)
     * @return List of emission data for the specified country, sorted by year (newest first)
     */
    public List<EmissionData> getEmissionsByCountry(String countryName) {
        System.out.println("Filtering country data from database: " + countryName);
        
        // Load directly from database to capture all scientist-uploaded data
        List<EmissionData> allEmissions = getAllEmissionsSorted();
        
        if (allEmissions.isEmpty()) {
            System.out.println("No data available!");
            return new ArrayList<>();
        }
        
        // Filter by country name (case-insensitive) and sort by year
        List<EmissionData> result = allEmissions.stream()
            .filter(emission -> emission.getCountryName().equalsIgnoreCase(countryName))
            .sorted((a, b) -> Integer.compare(b.getYear(), a.getYear()))
            .collect(Collectors.toList());
            
        System.out.println("Found " + result.size() + " records for " + countryName + " (including scientist data)");
        return result;
    }

    // ========================================
    // DASHBOARD & USER-SPECIFIC METHODS
    // ========================================

    /**
     * Retrieves emission data uploaded by a specific user.
     * Used for scientist dashboard to show personal contributions.
     * @param username The username of the data uploader
     * @return List of emission data uploaded by the user, sorted by upload date (newest first)
     */
    public List<EmissionData> getDataByUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return repository.findByUploadedByOrderBySourceDateDesc(username);
    }

    /**
     * Retrieves the most recent emission data for dashboard display.
     * Optimized with repository methods for better performance.
     * @param limit Maximum number of records to return
     * @return List of recent emission data, sorted by year (newest first) and upload date
     */
    public List<EmissionData> getRecentEmissions(int limit) {
        try {
            // Use optimized repository methods when available
            if (limit <= 10) {
                return repository.findTop10ByOrderByYearDescSourceDateDesc();
            } else if (limit <= 20) {
                return repository.findTop20ByOrderByYearDescSourceDateDesc();
            } else {
                // Fallback for larger limits with custom sorting
                return repository.findAll().stream()
                    .filter(data -> data.getYear() != null)
                    .sorted((a, b) -> {
                        // Primary sort: by year (newest first)
                        int yearComparison = Integer.compare(b.getYear(), a.getYear());
                        if (yearComparison != 0) {
                            return yearComparison;
                        }
                        // Secondary sort: by upload date for same year
                        if (a.getSourceDate() != null && b.getSourceDate() != null) {
                            return b.getSourceDate().compareTo(a.getSourceDate());
                        }
                        return 0;
                    })
                    .limit(limit)
                    .collect(Collectors.toList());
            }
            
        } catch (Exception e) {
            System.err.println("Error retrieving recent emissions: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Retrieves all emission data sorted by year and upload date for dashboard tables.
     * Provides consistent ordering for display purposes.
     * @return List of all emission data sorted by year (newest first), then by upload date
     */
    public List<EmissionData> getAllEmissionsSorted() {
        try {
            return repository.findAll().stream()
                .filter(data -> data.getYear() != null)
                .filter(data -> isValidCountry(data.getCountryName()))
                .sorted((a, b) -> {
                    // Primary sort: by year (newest first)
                    int yearComparison = Integer.compare(b.getYear(), a.getYear());
                    if (yearComparison != 0) {
                        return yearComparison;
                    }
                    // Secondary sort: by upload date for same year
                    if (a.getSourceDate() != null && b.getSourceDate() != null) {
                        return b.getSourceDate().compareTo(a.getSourceDate());
                    }
                    return 0;
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            System.err.println("Error in getAllEmissionsSorted: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Counts the number of distinct countries in the database.
     * Used for dashboard statistics and overview displays.
     * @return Number of unique countries with emission data
     */
    public long getCountryCount() {
        try {
            Long count = repository.countDistinctCountries();
            return count != null ? count : 0L;
        } catch (Exception e) {
            // Fallback: Count from API data
            return getAllEmissions().stream()
                .map(EmissionData::getCountryName)
                .distinct()
                .count();
        }
    }

    /**
     * Finds the most recent year with available emission data.
     * Used for setting default values and data validation.
     * @return The latest year with emission data, defaults to 2023 if none found
     */
    public Integer getLatestYear() {
        try {
            System.out.println("Searching for latest year in database: " + repository.findMaxYear());
            return repository.findMaxYear();
        } catch (Exception e) {
            // Fallback: Determine from API data
            System.err.println("Error loading latest year: " + e.getMessage());
            return getAllEmissions().stream()
                .mapToInt(EmissionData::getYear)
                .max()
                .orElse(2023);
        }
    }

    /**
     * Retrieves all unique country names for dropdowns and filters.
     * Provides alphabetically sorted list of countries with emission data.
     * @return Sorted list of all country names in the database
     */
    public List<String> getAllCountries() {
        try {
            List<String> dbCountries = repository.findDistinctCountryNames();
            if (!dbCountries.isEmpty()) {
                return dbCountries;
            }
        } catch (Exception e) {
            System.err.println("Error loading countries from database: " + e.getMessage());
        }
        
        // Fallback: Extract from API data
        return getAllEmissions().stream()
            .map(EmissionData::getCountryName)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    // ========================================
    // CRUD OPERATIONS FOR DASHBOARD
    // ========================================

    /**
     * Saves emission data to the database and invalidates cache.
     * Used for new data uploads and updates that require full cache refresh.
     * @param data The emission data to save
     * @return The saved emission data with generated ID
     */
    public EmissionData saveEmission(EmissionData data) {
        EmissionData saved = repository.save(data);
        clearCache(); // Invalidate cache to ensure consistency
        return saved;
    }

    /**
     * Saves emission data without clearing cache for performance-optimized updates.
     * Intelligently updates the specific record in cache instead of clearing all.
     * Use for rapid updates when cache consistency is critical.
     * @param data The emission data to save
     * @return The saved emission data with generated ID
     */
    public EmissionData saveEmissionFast(EmissionData data) {
        EmissionData saved = repository.save(data);
        
        // Intelligent cache update: Replace only the specific record in cache
        if (cachedAllData != null && !cachedAllData.isEmpty() && saved.getId() != null) {
            // Find existing record in cache
            for (int i = 0; i < cachedAllData.size(); i++) {
                EmissionData existing = cachedAllData.get(i);
                if (existing.getId() != null && existing.getId().equals(saved.getId())) {
                    // Replace old record with updated one
                    cachedAllData.set(i, saved);
                    System.out.println("Cache updated for ID: " + saved.getId() + " - " + 
                                     saved.getCountryName() + " (" + saved.getYear() + ")");
                    break;
                }
            }
        }
        
        return saved;
    }

    /**
     * Checks if emission data already exists for a specific country and year.
     * Used to prevent duplicate entries during data import.
     * @param countryName Name of the country
     * @param year The year to check
     * @return true if data exists, false otherwise
     */
    public boolean existsByCountryAndYear(String countryName, Integer year) {
        return repository.findByCountryNameAndYear(countryName, year).isPresent();
    }

    /**
     * Finds emission data by ID with error handling.
     * @param id The ID of the emission data record
     * @return The emission data record
     * @throws RuntimeException if record is not found
     */
    public EmissionData findById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Record not found: " + id));
    }

    /**
     * Deletes an emission data record by ID.
     * Also removes all related edit requests to maintain referential integrity.
     * @param id The ID of the emission data to delete
     */
    public void deleteById(Long id) {
        // First delete all related edit requests to avoid foreign key constraints
        List<EditRequest> relatedRequests = editRequestRepository.findByEmissionDataIdOrderByRequestedAtDesc(id);
        if (!relatedRequests.isEmpty()) {
            editRequestRepository.deleteAll(relatedRequests);
        }
        
        // Then delete the emission data record
        repository.deleteById(id);
        clearCache(); // Invalidate cache after deletion
    }

    /**
     * Filters emission data by data source for analysis and reporting.
     * @param source The data source to filter by (e.g., "API", "Scientist Upload")
     * @return List of emission data from the specified source, sorted by date
     */
    public List<EmissionData> getDataBySource(String source) {
        return repository.findByDataSourceOrderBySourceDateDesc(source);
    }

    // ========================================
    // EXTERNAL API INTEGRATION
    // ========================================

    /**
     * Loads all emission data from external API without using cache.
     * Primary data source for fresh emission data from Our World in Data.
     * @return List of emission data from external API, empty if API is unavailable
     */
    private List<EmissionData> loadAllFromAPI() {
        try {
            String apiUrl = "https://nyc3.digitaloceanspaces.com/owid-public/data/co2/owid-co2-data.json";
            System.out.println("Making API request to: " + apiUrl);
            
            String response = restTemplate.getForObject(apiUrl, String.class);
            
            if (response != null && response.length() > 1000) {
                System.out.println("API response received: " + response.length() + " characters");
                return parseOWIDDataAll(response);
            } else {
                System.out.println("API response too small or null");
            }
            
        } catch (Exception e) {
            System.err.println("API Error: " + e.getMessage());
        }
        
        return new ArrayList<>();
    }

    /**
     * Parses OWID JSON response for all countries and years.
     * Filters and transforms the raw API data into EmissionData objects.
     * Only processes valid countries with 3-letter ISO codes from years 1900-2025.
     * @param jsonResponse Raw JSON response from OWID API
     * @return List of parsed emission data records
     */
    private List<EmissionData> parseOWIDDataAll(String jsonResponse) {
        List<EmissionData> result = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            System.out.println("Parsing all OWID countries and years...");
            
            AtomicInteger processedCountries = new AtomicInteger(0);
            
            root.fieldNames().forEachRemaining(countryName -> {
                try {
                    JsonNode countryData = root.get(countryName);
                    
                    if (countryData.has("iso_code") && countryData.has("data")) {
                        String countryCode = countryData.get("iso_code").asText();
                        JsonNode dataArray = countryData.get("data");
                        
                        // Only process countries with valid 3-letter ISO codes
                        if (isValidCountry(countryName) && countryCode.length() == 3) {
                            
                            for (JsonNode yearData : dataArray) {
                                if (yearData.has("year") && yearData.has("co2")) {
                                    int year = yearData.get("year").asInt();
                                    
                                    // Filter for reasonable year range
                                    if (year >= 1900 && year <= 2025) {
                                        JsonNode co2Node = yearData.get("co2");
                                        
                                        // Only include records with valid positive CO2 values
                                        if (!co2Node.isNull() && co2Node.asDouble() > 0) {
                                            EmissionData emission = new EmissionData();
                                            emission.setCountryName(countryName);
                                            emission.setCountryCode(countryCode);
                                            emission.setYear(year);
                                            emission.setCo2EmissionKt(co2Node.asDouble() * 1000); // Convert to kilotons
                                            emission.setSourceDate(LocalDate.now());
                                            emission.setDataSource("Our World in Data API (Live)");
                                            emission.setUploadedBy("API_IMPORT"); // Mark as API data
                                            
                                            result.add(emission);
                                            
                                            // Progress logging every 500 records
                                            if (result.size() % 500 == 0) {
                                                System.out.println(result.size() + " records loaded...");
                                            }
                                        }
                                    }
                                }
                            }
                            
                            int currentCount = processedCountries.incrementAndGet();
                            if (currentCount % 20 == 0) {
                                System.out.println(currentCount + " countries processed...");
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error processing country " + countryName + ": " + e.getMessage());
                }
            });
            
            System.out.println(result.size() + " records loaded from API (all countries & years)");
            
        } catch (Exception e) {
            System.err.println("Parse error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }

    /**
     * Validates if a country name represents a real country vs. a region or group.
     * Filters out continental aggregates, income groups, and other non-country entities.
     * @param countryName Name to validate
     * @return true if it represents a real country, false for regions/groups
     */
    private boolean isValidCountry(String countryName) {
        // Exclude regions, economic groups, and other non-country entities
        List<String> excludeRegions = List.of(
            "World", "High income", "Low income", "Middle income", 
            "Upper middle income", "Lower middle income",
            "Europe & Central Asia", "East Asia & Pacific",
            "Latin America & Caribbean", "Sub-Saharan Africa",
            "North America", "South Asia", "Arab World",
            "European Union", "OECD", "Asia", "Europe", "Africa",
            "International transport", "Global", "Antarctica",
            "Europe (excl. EU-27)", "Europe (excl. EU-28)",
            "Asia (excl. China & India)", "Non-OECD (IIASA)",
            "OECD (IIASA)", "Bunkers", "Statistical differences",
            "International aviation", "International shipping",
            "G20", "G7"
        );
        
        // Exclude dependent territories and special administrative regions
        List<String> excludeTerritories = List.of(
            "Anguilla", "Aruba", "Bermuda", "British Virgin Islands",
            "Bonaire Sint Eustatius and Saba", "Christmas Island",
            "Cook Islands", "Curacao", "Faroe Islands", "French Polynesia",
            "Greenland", "Hong Kong", "Macao", "Montserrat",
            "New Caledonia", "Niue", "Saint Helena", "Saint Pierre and Miquelon",
            "Turks and Caicos Islands", "Wallis and Futuna"
        );
        
        // Combined validation check
        return !excludeRegions.contains(countryName) && 
            !excludeTerritories.contains(countryName) &&
            !countryName.contains("(") &&
            !countryName.toLowerCase().contains("income") &&
            !countryName.toLowerCase().contains("bunker") &&
            !countryName.toLowerCase().contains("excl") &&
            !countryName.toLowerCase().contains("transport") &&
            countryName.length() > 2;
    }

    // ========================================
    // DATABASE IMPORT OPERATIONS
    // ========================================

    /**
     * Imports emission data from external API into the database.
     * Clears cache after import to ensure data consistency.
     * @return Number of records successfully imported
     */
    public int importWorldBankData() {
        try {
            String apiUrl = "https://nyc3.digitaloceanspaces.com/owid-public/data/co2/owid-co2-data.json";
            String response = restTemplate.getForObject(apiUrl, String.class);
            
            if (response != null && response.length() > 1000) {
                int imported = saveOWIDDataToDatabase(response);
                clearCache();
                System.out.println("Cache cleared after database import");
                return imported;
            }
            return 0;
            
        } catch (Exception e) {
            System.err.println("Import error: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Saves OWID API data directly to database with duplicate checking.
     * Processes all valid countries and years, skipping existing records.
     * @param jsonResponse Raw JSON response from OWID API
     * @return Number of records successfully saved to database
     */
    private int saveOWIDDataToDatabase(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            AtomicInteger importedCount = new AtomicInteger(0);
            
            System.out.println("Saving OWID CO₂ data to database...");
            
            root.fieldNames().forEachRemaining(countryName -> {
                try {
                    JsonNode countryData = root.get(countryName);
                    
                    if (countryData.has("iso_code") && countryData.has("data")) {
                        String countryCode = countryData.get("iso_code").asText();
                        JsonNode dataArray = countryData.get("data");
                        
                        if (isValidCountry(countryName) && countryCode.length() == 3) {
                            for (JsonNode yearData : dataArray) {
                                if (yearData.has("year") && yearData.has("co2")) {
                                    int year = yearData.get("year").asInt();
                                    
                                    // Extended year range for historical data
                                    if (year >= 1900 && year <= 2025) {
                                        JsonNode co2Node = yearData.get("co2");
                                        
                                        if (!co2Node.isNull() && co2Node.asDouble() > 0) {
                                            // Skip if data already exists to avoid duplicates
                                            if (!dataExists(countryName, year)) {
                                                EmissionData emission = new EmissionData();
                                                emission.setCountryName(countryName);
                                                emission.setCountryCode(countryCode);
                                                emission.setYear(year);
                                                emission.setCo2EmissionKt(co2Node.asDouble() * 1000); // Convert to kilotons
                                                emission.setSourceDate(LocalDate.now());
                                                emission.setDataSource("Our World in Data CO₂ Database");
                                                emission.setUploadedBy("DB_IMPORT"); // Mark as database import
                                                
                                                repository.save(emission);
                                                int current = importedCount.incrementAndGet();
                                                
                                                // Progress logging every 100 records
                                                if (current % 100 == 0) {
                                                    System.out.println(current + " records saved to database...");
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Database error for country " + countryName + ": " + e.getMessage());
                }
            });
            
            int totalImported = importedCount.get();
            System.out.println(totalImported + " new CO₂ records imported to database!");
            return totalImported;
            
        } catch (Exception e) {
            System.err.println("Error saving to database: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Database import failed", e);
        }
    }

    /**
     * Checks if emission data already exists in database for given country and year.
     * Used to prevent duplicate entries during import operations.
     * @param countryName Name of the country
     * @param year The year to check
     * @return true if data exists, false otherwise
     */
    private boolean dataExists(String countryName, int year) {
        return repository.findByCountryNameAndYear(countryName, year).isPresent();
    }

    // ========================================
    // CACHE MANAGEMENT
    // ========================================

    /**
     * Manually clears the emission data cache.
     * Should be called after data modifications to ensure consistency.
     */
    public void clearCache() {
        cachedAllData = null;
        lastCacheTime = 0;
        System.out.println("Cache manually cleared - next request will reload");
    }

    /**
     * Returns current cache status for monitoring and debugging.
     * @return String describing cache state, size, and remaining time
     */
    public String getCacheStatus() {
        if (cachedAllData == null) {
            return "Cache is empty";
        }
        
        long age = System.currentTimeMillis() - lastCacheTime;
        long remainingTime = CACHE_DURATION - age;
        
        return String.format("Cache: %d records, Age: %d seconds, Remaining: %d seconds",
            cachedAllData.size(), age / 1000, remainingTime / 1000);
    }

    // ========================================
    // LEGACY & UTILITY METHODS
    // ========================================

    /**
     * Legacy method for backward compatibility.
     * @deprecated Use getAllCountries() instead
     * @return List of all country names
     */
    public List<String> getAllCountryNames() {
        return getAllCountries();
    }

    /**
     * Debug method for development and troubleshooting.
     * Logs current database statistics and sample data.
     */
    public void debugDatabase() {
        long count = repository.count();
        System.out.println("Number of records in database: " + count);
        
        if (count > 0) {
            List<EmissionData> first5 = repository.findAll().stream().limit(5).toList();
            for (EmissionData data : first5) {
                System.out.println(data.getCountryName() + " (" + data.getYear() + "): " + 
                    data.getCo2EmissionKt() + " kt [" + data.getUploadedBy() + "]");
            }
        }
        
        System.out.println(getCacheStatus());
        System.out.println("Countries: " + getCountryCount());
        System.out.println("Latest Year: " + getLatestYear());
    }

    /**
     * Provides comprehensive service statistics for admin dashboard monitoring.
     * Returns formatted statistics including cache status, database metrics, and data coverage.
     * Used for system monitoring and performance analysis.
     * @return Formatted string containing service statistics and metrics
     */
    public String getServiceStatistics() {
        return String.format(
            "Service Stats: Cache=%s, DB-Records=%d, Countries=%d, Latest Year=%d",
            getCacheStatus(),
            repository.count(),
            getCountryCount(),
            getLatestYear()
        );
    }
}