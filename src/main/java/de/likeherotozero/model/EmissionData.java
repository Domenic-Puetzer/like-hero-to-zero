package de.likeherotozero.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Emission Data Entity
 * Represents CO2 emission data for a specific country and year.
 * Contains emission values, metadata, and source attribution for scientific collaboration.
 */
@Entity
@Table(name = "emission_data")
public class EmissionData {
    
    /**
     * Unique identifier for the emission data entry
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Full name of the country (e.g., "Germany", "United States")
     */
    @Column(name = "country_name")
    private String countryName;
    
    /**
     * ISO 3166-1 alpha-3 country code (e.g., "DEU", "USA")
     */
    @Column(name = "country_code")
    private String countryCode;
    
    /**
     * Year for which the emission data is recorded
     */
    @Column(name = "year")
    private Integer year;
    
    /**
     * CO2 emission value in kilotons (kt)
     */
    @Column(name = "co2_emission_kt")
    private Double co2EmissionKt;
    
    /**
     * Date when the data was sourced or last updated
     */
    @Column(name = "source_date")
    private LocalDate sourceDate;
    
    /**
     * Attribution of the data source (e.g., "World Bank Open Data", "Scientist Upload")
     */
    @Column(name = "data_source")
    private String dataSource;
    
    /**
     * Username of the scientist who uploaded or last modified this data
     */
    @Column(name = "uploaded_by")
    private String uploadedBy;
    
    /**
     * Default constructor for JPA
     */
    public EmissionData() {}
    
    /**
     * Constructor for creating emission data with basic information
     * @param countryName Full name of the country
     * @param countryCode ISO 3166-1 alpha-3 country code
     * @param year Year for the emission data
     * @param co2EmissionKt CO2 emission value in kilotons
     */
    public EmissionData(String countryName, String countryCode, Integer year, Double co2EmissionKt) {
        this.countryName = countryName;
        this.countryCode = countryCode;
        this.year = year;
        this.co2EmissionKt = co2EmissionKt;
        this.sourceDate = LocalDate.now();
    }

    // Getters and Setters with documentation

    /**
     * Gets the unique identifier of the emission data
     * @return The emission data ID
     */
    public Long getId() { 
        return id; 
    }
    
    /**
     * Sets the unique identifier of the emission data
     * @param id The emission data ID
     */
    public void setId(Long id) { 
        this.id = id; 
    }
    
    /**
     * Gets the full name of the country
     * @return The country name
     */
    public String getCountryName() {
        return countryName;
    }
    
    /**
     * Sets the full name of the country
     * @param countryName The country name
     */
    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }
    
    /**
     * Gets the ISO 3166-1 alpha-3 country code
     * @return The country code
     */
    public String getCountryCode() {
        return countryCode;
    }
    
    /**
     * Sets the ISO 3166-1 alpha-3 country code
     * @param countryCode The country code
     */
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
    
    /**
     * Gets the year for which the emission data is recorded
     * @return The year
     */
    public Integer getYear() {
        return year;
    }
    
    /**
     * Sets the year for which the emission data is recorded
     * @param year The year
     */
    public void setYear(Integer year) {
        this.year = year;
    }
    
    /**
     * Gets the CO2 emission value in kilotons
     * @return The CO2 emission value in kt
     */
    public Double getCo2EmissionKt() {
        return co2EmissionKt;
    }
    
    /**
     * Sets the CO2 emission value in kilotons
     * @param co2EmissionKt The CO2 emission value in kt
     */
    public void setCo2EmissionKt(Double co2EmissionKt) {
        this.co2EmissionKt = co2EmissionKt;
    }
    
    /**
     * Gets the date when the data was sourced or last updated
     * @return The source date
     */
    public LocalDate getSourceDate() {
        return sourceDate;
    }
    
    /**
     * Sets the date when the data was sourced or last updated
     * @param sourceDate The source date
     */
    public void setSourceDate(LocalDate sourceDate) {
        this.sourceDate = sourceDate;
    }
    
    /**
     * Gets the attribution of the data source
     * @return The data source description
     */
    public String getDataSource() {
        return dataSource;
    }
    
    /**
     * Sets the attribution of the data source
     * @param dataSource The data source description
     */
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * Gets the username of the scientist who uploaded or last modified this data
     * @return The username of the uploader
     */
    public String getUploadedBy() {
        return uploadedBy;
    }
    
    /**
     * Sets the username of the scientist who uploaded or last modified this data
     * @param uploadedBy The username of the uploader
     */
    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
}