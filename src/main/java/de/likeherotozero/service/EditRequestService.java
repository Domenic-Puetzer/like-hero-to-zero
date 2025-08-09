package de.likeherotozero.service;

import de.likeherotozero.model.EditRequest;
import de.likeherotozero.model.EmissionData;
import de.likeherotozero.model.User;
import de.likeherotozero.repository.EditRequestRepository;
import de.likeherotozero.repository.EmissionDataRepository;
import de.likeherotozero.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Edit Request Service
 * Business logic layer for managing the peer-review workflow in the scientific collaboration system.
 * Handles creation, approval, rejection, and querying of edit requests between scientists.
 * Ensures data integrity and proper authorization for collaborative data editing.
 */
@Service
@Transactional
public class EditRequestService {

    private final EditRequestRepository editRequestRepository;
    private final EmissionDataRepository emissionDataRepository;
    private final UserRepository userRepository;

    /**
     * Constructor injection for required repositories.
     * @param editRequestRepository Repository for edit request data access
     * @param emissionDataRepository Repository for emission data access
     * @param userRepository Repository for user data access
     */
    public EditRequestService(EditRequestRepository editRequestRepository, 
                            EmissionDataRepository emissionDataRepository,
                            UserRepository userRepository) {
        this.editRequestRepository = editRequestRepository;
        this.emissionDataRepository = emissionDataRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new edit request for peer review.
     * Allows one scientist to request changes to another scientist's data.
     * Validates that no duplicate pending requests exist for the same data.
     * @param emissionDataId The ID of the emission data to be edited
     * @param requesterUsername Username of the scientist requesting the edit
     * @param requestMessage Explanation of why the edit is needed
     * @param proposedCountryName Proposed new country name
     * @param proposedYear Proposed new year
     * @param proposedCo2EmissionKt Proposed new CO2 emission value in kilotons
     * @param proposedDataSource Proposed new data source attribution
     * @return The created edit request
     * @throws IllegalArgumentException if data or users are not found
     * @throws IllegalStateException if a pending request already exists for this data
     */
    public EditRequest createEditRequest(Long emissionDataId, String requesterUsername, 
                                       String requestMessage, String proposedCountryName, 
                                       Integer proposedYear, Double proposedCo2EmissionKt, 
                                       String proposedDataSource) {
        
        // Load data and users
        EmissionData emissionData = emissionDataRepository.findById(emissionDataId)
            .orElseThrow(() -> new IllegalArgumentException("Datensatz nicht gefunden"));
        
        User requester = userRepository.findByUsernameIgnoreCase(requesterUsername)
            .orElseThrow(() -> new IllegalArgumentException("Antragsteller nicht gefunden"));
        
        User dataOwner = userRepository.findByUsernameIgnoreCase(emissionData.getUploadedBy())
            .orElseThrow(() -> new IllegalArgumentException("Datenbesitzer nicht gefunden"));
        
        // Check if a pending request already exists
        List<EditRequest> existingRequests = editRequestRepository
            .findPendingRequestForData(emissionDataId, requester);
        
        if (!existingRequests.isEmpty()) {
            throw new IllegalStateException("Es existiert bereits eine offene Bearbeitungsanfrage für diesen Datensatz");
        }
        
        // Create new request
        EditRequest editRequest = new EditRequest(emissionData, requester, dataOwner, requestMessage);
        editRequest.setProposedCountryName(proposedCountryName);
        editRequest.setProposedYear(proposedYear);
        editRequest.setProposedCo2EmissionKt(proposedCo2EmissionKt);
        editRequest.setProposedDataSource(proposedDataSource);
        
        return editRequestRepository.save(editRequest);
    }

    /**
     * Approves an edit request and applies the proposed changes to the emission data.
     * Updates the request status, records the response, and modifies the actual data.
     * Only pending requests can be approved.
     * @param requestId The ID of the edit request to approve
     * @param responseMessage Response message explaining the approval
     * @throws IllegalArgumentException if the request is not found
     * @throws IllegalStateException if the request is not in pending status
     */
    public void approveEditRequest(Long requestId, String responseMessage) {
        EditRequest request = editRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Anfrage nicht gefunden"));
        
        if (request.getStatus() != EditRequest.EditRequestStatus.PENDING) {
            throw new IllegalStateException("Anfrage wurde bereits bearbeitet");
        }
        
        // Approve the request
        request.setStatus(EditRequest.EditRequestStatus.APPROVED);
        request.setResponseMessage(responseMessage);
        request.setRespondedAt(LocalDateTime.now());
        
        // Apply the proposed changes to the actual data
        EmissionData data = request.getEmissionData();
        if (request.getProposedCountryName() != null) {
            data.setCountryName(request.getProposedCountryName());
        }
        if (request.getProposedYear() != null) {
            data.setYear(request.getProposedYear());
        }
        if (request.getProposedCo2EmissionKt() != null) {
            data.setCo2EmissionKt(request.getProposedCo2EmissionKt());
        }
        if (request.getProposedDataSource() != null) {
            data.setDataSource(request.getProposedDataSource());
        }
        
        emissionDataRepository.save(data);
        editRequestRepository.save(request);
    }

    /**
     * Rejects an edit request without applying any changes.
     * Updates the request status and records the rejection reason.
     * Only pending requests can be rejected.
     * @param requestId The ID of the edit request to reject
     * @param responseMessage Response message explaining the rejection
     * @throws IllegalArgumentException if the request is not found
     * @throws IllegalStateException if the request is not in pending status
     */
    public void rejectEditRequest(Long requestId, String responseMessage) {
        EditRequest request = editRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Anfrage nicht gefunden"));
        
        if (request.getStatus() != EditRequest.EditRequestStatus.PENDING) {
            throw new IllegalStateException("Anfrage wurde bereits bearbeitet");
        }
        
        request.setStatus(EditRequest.EditRequestStatus.REJECTED);
        request.setResponseMessage(responseMessage);
        request.setRespondedAt(LocalDateTime.now());
        
        editRequestRepository.save(request);
    }

    /**
     * Retrieves pending edit requests for a user as the data owner.
     * Used for displaying requests that need approval or rejection from the user.
     * @param username Username of the data owner
     * @return List of pending edit requests ordered by request date (newest first)
     * @throws IllegalArgumentException if the user is not found
     */
    public List<EditRequest> getPendingRequestsForUser(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new IllegalArgumentException("Benutzer nicht gefunden"));
        
        return editRequestRepository.findByDataOwnerAndStatusOrderByRequestedAtDesc(
            user, EditRequest.EditRequestStatus.PENDING);
    }

    /**
     * Retrieves pending edit requests submitted by a user.
     * Used for tracking the status of requests made by a scientist.
     * @param username Username of the requester
     * @return List of pending edit requests ordered by request date (newest first)
     * @throws IllegalArgumentException if the user is not found
     */
    public List<EditRequest> getRequestsByUser(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new IllegalArgumentException("Benutzer nicht gefunden"));
        
        return editRequestRepository.findByRequesterAndStatusOrderByRequestedAtDesc(
            user, EditRequest.EditRequestStatus.PENDING);
    }

    /**
     * Retrieves all edit requests submitted by a user regardless of status.
     * Used for displaying a complete history of requests made by a scientist.
     * @param username Username of the requester
     * @return List of all edit requests ordered by request date (newest first)
     * @throws IllegalArgumentException if the user is not found
     */
    public List<EditRequest> getRequestsByRequester(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new IllegalArgumentException("Benutzer nicht gefunden"));
        
        return editRequestRepository.findByRequesterOrderByRequestedAtDesc(user);
    }

    /**
     * Counts the number of pending edit requests for a user as the data owner.
     * Used for dashboard statistics and notification badges.
     * @param username Username of the data owner
     * @return Number of pending edit requests
     * @throws IllegalArgumentException if the user is not found
     */
    public Long countPendingRequestsForUser(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new IllegalArgumentException("Benutzer nicht gefunden"));
        
        return editRequestRepository.countPendingRequestsForUser(user);
    }

    /**
     * Retrieves a specific edit request by its ID.
     * Used for detailed view and processing of individual requests.
     * @param requestId The ID of the edit request
     * @return Optional containing the edit request if found
     */
    public Optional<EditRequest> getEditRequest(Long requestId) {
        return editRequestRepository.findById(requestId);
    }
}
