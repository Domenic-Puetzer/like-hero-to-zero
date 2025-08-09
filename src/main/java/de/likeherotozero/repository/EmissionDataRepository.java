package de.likeherotozero.repository;

import de.likeherotozero.model.EmissionData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Emission Data Repository
 * Data access layer for managing CO2 emission data operations.
 * Provides methods for querying emission data by various criteria including
 * country, year, user, and data source for the scientific collaboration system.
 */
public interface EmissionDataRepository extends JpaRepository<EmissionData, Long> {

    /**
     * Finds all emission data for a specific country ordered by year (newest first).
     * Used for displaying country-specific emission trends and historical data.
     * @param countryName The name of the country to search for
     * @return List of emission data ordered by year descending
     */
    List<EmissionData> findByCountryNameOrderByYearDesc(String countryName);
    
    /**
     * Finds all emission data ordered by country name alphabetically, then by year (newest first).
     * Used for comprehensive data listings and export functionality.
     * @return List of all emission data ordered by country name ascending, then year descending
     */
    List<EmissionData> findAllByOrderByCountryNameAscYearDesc();
    
    /**
     * Finds emission data for a specific country and year combination.
     * Used for duplicate checking and specific data point retrieval.
     * @param countryName The name of the country
     * @param year The year of the emission data
     * @return Optional containing the emission data if found
     */
    Optional<EmissionData> findByCountryNameAndYear(String countryName, Integer year);

    /**
     * Finds all emission data uploaded by a specific user ordered by source date (newest first).
     * Used for displaying a scientist's personal contributions and data management.
     * @param uploadedBy The username of the scientist who uploaded the data
     * @return List of emission data ordered by source date descending
     */
    List<EmissionData> findByUploadedByOrderBySourceDateDesc(String uploadedBy);
    
    /**
     * Finds the emission data entry with the most recent year.
     * Used for dashboard statistics to show the latest available data year.
     * @return Optional containing the emission data with the highest year value
     */
    Optional<EmissionData> findTopByOrderByYearDesc();
    
    /**
     * Retrieves a distinct list of all country names in the database.
     * Used for autocomplete functionality and country selection dropdowns.
     * @return List of unique country names ordered alphabetically
     */
    @Query("SELECT DISTINCT e.countryName FROM EmissionData e ORDER BY e.countryName")
    List<String> findDistinctCountryNames();
    
    /**
     * Finds the 10 most recently added emission data entries.
     * Used for dashboard "Recent Data" sections with limited display.
     * @return List of the 10 most recent emission data entries by source date
     */
    List<EmissionData> findTop10ByOrderBySourceDateDesc();
    
    /**
     * Finds the 20 most recently added emission data entries.
     * Used for extended "Recent Data" views and pagination.
     * @return List of the 20 most recent emission data entries by source date
     */
    List<EmissionData> findTop20ByOrderBySourceDateDesc();
    
    /**
     * Finds the 10 most recent emission data entries ordered by year then source date.
     * Prioritizes newer years over newer upload dates for chronological relevance.
     * @return List of 10 emission data entries ordered by year desc, then source date desc
     */
    List<EmissionData> findTop10ByOrderByYearDescSourceDateDesc();
    
    /**
     * Finds the 20 most recent emission data entries ordered by year then source date.
     * Extended version for larger data displays with chronological prioritization.
     * @return List of 20 emission data entries ordered by year desc, then source date desc
     */
    List<EmissionData> findTop20ByOrderByYearDescSourceDateDesc();
    
    /**
     * Finds emission data filtered by data source ordered by source date (newest first).
     * Used for analyzing data from specific sources (e.g., World Bank, scientist uploads).
     * @param dataSource The data source to filter by
     * @return List of emission data from the specified source ordered by source date descending
     */
    List<EmissionData> findByDataSourceOrderBySourceDateDesc(String dataSource);
    
    /**
     * Counts the number of distinct countries in the database.
     * Used for dashboard statistics to show global coverage.
     * @return Number of unique countries with emission data
     */
    @Query("SELECT COUNT(DISTINCT e.countryName) FROM EmissionData e")
    Long countDistinctCountries();
    
    /**
     * Finds the maximum year value in the database.
     * Used for dashboard statistics to show the latest data year available.
     * @return The highest year value in the emission data, or null if no data exists
     */
    @Query("SELECT MAX(e.year) FROM EmissionData e")
    Integer findMaxYear();

    /**
     * Checks if there is at least one emission data entry for a given uploader and year.
     * Used to prevent duplicate demo data creation for the same user and year.
     * @param uploadedBy The username of the uploader (e.g. "scientist")
     * @param year The year to check for existing emission data
     * @return true if at least one entry exists, false otherwise
     */
    boolean existsByUploadedByAndYear(String uploadedBy, int year);
}