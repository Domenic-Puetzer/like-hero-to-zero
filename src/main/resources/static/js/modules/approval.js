/**
 * Approval Module
 * Handles approval and rejection of edit requests from other scientists.
 * Manages the peer-review workflow for collaborative data editing.
 */

export const ApprovalModule = {
    /**
     * Shows the approval confirmation modal for an edit request.
     * Displays request details and sets up the approval form.
     * @param {HTMLElement} button - The approve button containing request data attributes
     */
    showApproveModal(button) {
        const requestId = button.getAttribute('data-request-id');
        const requester = button.getAttribute('data-requester');
        const country = button.getAttribute('data-country');
        const year = button.getAttribute('data-year');
        
        // Set hidden field value for form submission
        document.getElementById('approveRequestId').value = requestId;
        
        // Set confirmation message with request details
        document.getElementById('approveMessage').innerHTML = 
            `Möchten Sie die Bearbeitungsanfrage von <strong>${requester}</strong> für <strong>${country} (${year})</strong> genehmigen?`;
        
        // Set form action URL
        document.getElementById('approveForm').action = `/scientist/approve-edit/${requestId}`;
        
        // Show approval modal
        document.getElementById('approveModal').classList.remove('hidden');
        document.getElementById('approveModal').classList.add('flex');
    },

    /**
     * Closes the approval confirmation modal.
     * Hides the modal dialog without taking any action.
     */
    closeApproveModal() {
        document.getElementById('approveModal').classList.add('hidden');
        document.getElementById('approveModal').classList.remove('flex');
    },

    /**
     * Shows the rejection confirmation modal for an edit request.
     * Displays request details and sets up the rejection form.
     * @param {HTMLElement} button - The reject button containing request data attributes
     */
    showRejectModal(button) {
        const requestId = button.getAttribute('data-request-id');
        const requester = button.getAttribute('data-requester');
        const country = button.getAttribute('data-country');
        const year = button.getAttribute('data-year');
        
        // Set hidden field value for form submission
        document.getElementById('rejectRequestId').value = requestId;
        
        // Set confirmation message with request details
        document.getElementById('rejectMessage').innerHTML = 
            `Möchten Sie die Bearbeitungsanfrage von <strong>${requester}</strong> für <strong>${country} (${year})</strong> ablehnen?`;
        
        // Set form action URL
        document.getElementById('rejectForm').action = `/scientist/reject-edit/${requestId}`;
        
        // Show rejection modal
        document.getElementById('rejectModal').classList.remove('hidden');
        document.getElementById('rejectModal').classList.add('flex');
    },

    /**
     * Closes the rejection confirmation modal.
     * Hides the modal dialog without taking any action.
     */
    closeRejectModal() {
        document.getElementById('rejectModal').classList.add('hidden');
        document.getElementById('rejectModal').classList.remove('flex');
    }
};
