package de.likeherotozero.repository;

import de.likeherotozero.model.EditRequest;
import de.likeherotozero.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Edit Request Repository
 * Data access layer for managing edit request operations in the peer-review workflow.
 * Provides methods for querying edit requests by various criteria including status,
 * users, and emission data relationships.
 */
public interface EditRequestRepository extends JpaRepository<EditRequest, Long> {

    /**
     * Finds edit requests received by a data owner with a specific status.
     * Used to retrieve requests that need approval/rejection from the data owner.
     * @param dataOwner The user who owns the data being requested for edit
     * @param status The status to filter by (PENDING, APPROVED, REJECTED, EXPIRED)
     * @return List of edit requests ordered by request date (newest first)
     */
    List<EditRequest> findByDataOwnerAndStatusOrderByRequestedAtDesc(User dataOwner, EditRequest.EditRequestStatus status);
    
    /**
     * Finds edit requests submitted by a requester with a specific status.
     * Used to track the status of requests made by a scientist.
     * @param requester The user who submitted the edit requests
     * @param status The status to filter by (PENDING, APPROVED, REJECTED, EXPIRED)
     * @return List of edit requests ordered by request date (newest first)
     */
    List<EditRequest> findByRequesterAndStatusOrderByRequestedAtDesc(User requester, EditRequest.EditRequestStatus status);
    
    /**
     * Finds all edit requests submitted by a requester regardless of status.
     * Used for displaying a complete history of requests made by a scientist.
     * @param requester The user who submitted the edit requests
     * @return List of all edit requests ordered by request date (newest first)
     */
    List<EditRequest> findByRequesterOrderByRequestedAtDesc(User requester);
    
    /**
     * Finds edit requests for a data owner with multiple possible statuses.
     * Useful for finding all open requests (PENDING) or active requests.
     * @param dataOwner The user who owns the data being requested for edit
     * @param statuses List of statuses to include in the search
     * @return List of edit requests ordered by request date (newest first)
     */
    List<EditRequest> findByDataOwnerAndStatusInOrderByRequestedAtDesc(User dataOwner, List<EditRequest.EditRequestStatus> statuses);
    
    /**
     * Finds all edit requests for a specific emission data entry.
     * Used to track the history of edit requests for a particular dataset.
     * @param emissionDataId The ID of the emission data entry
     * @return List of edit requests ordered by request date (newest first)
     */
    List<EditRequest> findByEmissionDataIdOrderByRequestedAtDesc(Long emissionDataId);
    
    /**
     * Counts the number of pending edit requests for a specific user.
     * Used for dashboard statistics to show how many requests await response.
     * @param user The data owner whose pending requests should be counted
     * @return Number of pending edit requests
     */
    @Query("SELECT COUNT(er) FROM EditRequest er WHERE er.dataOwner = :user AND er.status = 'PENDING'")
    Long countPendingRequestsForUser(@Param("user") User user);
    
    /**
     * Checks if there is already a pending edit request for specific data by a requester.
     * Prevents duplicate requests for the same data from the same scientist.
     * @param dataId The ID of the emission data being requested for edit
     * @param requester The user who wants to submit the edit request
     * @return List of pending edit requests (should be empty or contain one element)
     */
    @Query("SELECT er FROM EditRequest er WHERE er.emissionData.id = :dataId AND er.requester = :requester AND er.status = 'PENDING'")
    List<EditRequest> findPendingRequestForData(@Param("dataId") Long dataId, @Param("requester") User requester);
}
