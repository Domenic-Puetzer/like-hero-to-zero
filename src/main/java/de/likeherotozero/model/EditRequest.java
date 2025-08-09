package de.likeherotozero.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Edit Request Entity
 * Represents a peer-review request where one scientist asks to modify another scientist's data.
 * Supports collaborative data editing workflow with approval/rejection mechanism.
 */
@Entity
@Table(name = "edit_requests")
public class EditRequest {
    
    /**
     * Unique identifier for the edit request
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The emission data entry that is being requested for modification
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emission_data_id", nullable = false)
    private EmissionData emissionData;
    
    /**
     * The scientist who is requesting to edit the data
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;
    
    /**
     * The scientist who owns the data and needs to approve/reject the request
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_owner_id", nullable = false)
    private User dataOwner;
    
    /**
     * Timestamp when the edit request was created
     */
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;
    
    /**
     * Timestamp when the data owner responded to the request (null if still pending)
     */
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
    
    /**
     * Current status of the edit request (PENDING, APPROVED, REJECTED, EXPIRED)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EditRequestStatus status = EditRequestStatus.PENDING;
    
    /**
     * Message from the requester explaining why the edit is needed
     */
    @Column(name = "request_message", columnDefinition = "TEXT")
    private String requestMessage;
    
    /**
     * Response message from the data owner (approval/rejection reason)
     */
    @Column(name = "response_message", columnDefinition = "TEXT")
    private String responseMessage;
    
    /**
     * Proposed new country name for the emission data
     */
    @Column(name = "proposed_country_name")
    private String proposedCountryName;
    
    /**
     * Proposed new year for the emission data
     */
    @Column(name = "proposed_year")
    private Integer proposedYear;
    
    /**
     * Proposed new CO2 emission value in kilotons
     */
    @Column(name = "proposed_co2_emission_kt")
    private Double proposedCo2EmissionKt;
    
    /**
     * Proposed new data source attribution
     */
    @Column(name = "proposed_data_source")
    private String proposedDataSource;

    /**
     * Status enumeration for edit request workflow
     */
    public enum EditRequestStatus {
        PENDING,    // Awaiting response from data owner
        APPROVED,   // Request approved and changes applied
        REJECTED,   // Request rejected by data owner
        EXPIRED     // Request expired without response
    }

    /**
     * Default constructor for JPA
     */
    public EditRequest() {}

    /**
     * Constructor for creating a new edit request
     * @param emissionData The emission data to be edited
     * @param requester The scientist requesting the edit
     * @param dataOwner The scientist who owns the data
     * @param requestMessage Explanation for the requested edit
     */
    public EditRequest(EmissionData emissionData, User requester, User dataOwner, String requestMessage) {
        this.emissionData = emissionData;
        this.requester = requester;
        this.dataOwner = dataOwner;
        this.requestMessage = requestMessage;
        this.requestedAt = LocalDateTime.now();
        this.status = EditRequestStatus.PENDING;
    }

    // Getters and Setters with documentation

    /**
     * Gets the unique identifier of the edit request
     * @return The edit request ID
     */
    public Long getId() { return id; }
    
    /**
     * Sets the unique identifier of the edit request
     * @param id The edit request ID
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Gets the emission data that this edit request targets
     * @return The emission data entity
     */
    public EmissionData getEmissionData() { return emissionData; }
    
    /**
     * Sets the emission data that this edit request targets
     * @param emissionData The emission data entity
     */
    public void setEmissionData(EmissionData emissionData) { this.emissionData = emissionData; }

    /**
     * Gets the scientist who requested the edit
     * @return The requester user entity
     */
    public User getRequester() { return requester; }
    
    /**
     * Sets the scientist who requested the edit
     * @param requester The requester user entity
     */
    public void setRequester(User requester) { this.requester = requester; }

    /**
     * Gets the scientist who owns the data being requested for edit
     * @return The data owner user entity
     */
    public User getDataOwner() { return dataOwner; }
    
    /**
     * Sets the scientist who owns the data being requested for edit
     * @param dataOwner The data owner user entity
     */
    public void setDataOwner(User dataOwner) { this.dataOwner = dataOwner; }

    /**
     * Gets the timestamp when the edit request was created
     * @return The request creation timestamp
     */
    public LocalDateTime getRequestedAt() { return requestedAt; }
    
    /**
     * Sets the timestamp when the edit request was created
     * @param requestedAt The request creation timestamp
     */
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    /**
     * Gets the timestamp when the data owner responded to the request
     * @return The response timestamp (null if still pending)
     */
    public LocalDateTime getRespondedAt() { return respondedAt; }
    
    /**
     * Sets the timestamp when the data owner responded to the request
     * @param respondedAt The response timestamp
     */
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }

    /**
     * Gets the current status of the edit request
     * @return The request status (PENDING, APPROVED, REJECTED, EXPIRED)
     */
    public EditRequestStatus getStatus() { return status; }
    
    /**
     * Sets the current status of the edit request
     * @param status The request status
     */
    public void setStatus(EditRequestStatus status) { this.status = status; }

    /**
     * Gets the message explaining why the edit is requested
     * @return The request message from the requester
     */
    public String getRequestMessage() { return requestMessage; }
    
    /**
     * Sets the message explaining why the edit is requested
     * @param requestMessage The request message from the requester
     */
    public void setRequestMessage(String requestMessage) { this.requestMessage = requestMessage; }

    /**
     * Gets the response message from the data owner
     * @return The response message (approval/rejection reason)
     */
    public String getResponseMessage() { return responseMessage; }
    
    /**
     * Sets the response message from the data owner
     * @param responseMessage The response message
     */
    public void setResponseMessage(String responseMessage) { this.responseMessage = responseMessage; }

    /**
     * Gets the proposed new country name for the emission data
     * @return The proposed country name
     */
    public String getProposedCountryName() { return proposedCountryName; }
    
    /**
     * Sets the proposed new country name for the emission data
     * @param proposedCountryName The proposed country name
     */
    public void setProposedCountryName(String proposedCountryName) { this.proposedCountryName = proposedCountryName; }

    /**
     * Gets the proposed new year for the emission data
     * @return The proposed year
     */
    public Integer getProposedYear() { return proposedYear; }
    
    /**
     * Sets the proposed new year for the emission data
     * @param proposedYear The proposed year
     */
    public void setProposedYear(Integer proposedYear) { this.proposedYear = proposedYear; }

    /**
     * Gets the proposed new CO2 emission value in kilotons
     * @return The proposed CO2 emission value
     */
    public Double getProposedCo2EmissionKt() { return proposedCo2EmissionKt; }
    
    /**
     * Sets the proposed new CO2 emission value in kilotons
     * @param proposedCo2EmissionKt The proposed CO2 emission value
     */
    public void setProposedCo2EmissionKt(Double proposedCo2EmissionKt) { this.proposedCo2EmissionKt = proposedCo2EmissionKt; }

    /**
     * Gets the proposed new data source attribution
     * @return The proposed data source
     */
    public String getProposedDataSource() { return proposedDataSource; }
    
    /**
     * Sets the proposed new data source attribution
     * @param proposedDataSource The proposed data source
     */
    public void setProposedDataSource(String proposedDataSource) { this.proposedDataSource = proposedDataSource; }
}
