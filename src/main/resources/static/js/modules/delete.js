/**
 * Delete Module
 * Handles deletion of emission data records with confirmation dialogs.
 * Provides safe deletion workflow with user confirmation and error handling.
 */

import { DashboardState } from './state.js';
import { Utils } from './utils.js';

export const DeleteModule = {
    /**
     * Shows the delete confirmation modal with record details.
     * Extracts data from the clicked button and displays confirmation dialog.
     * @param {HTMLElement} button - The delete button containing record data attributes
     */
    showModal(button) {
        const id = button.getAttribute('data-id');
        const country = button.getAttribute('data-country');
        const year = button.getAttribute('data-year');
        
        this.confirmDelete(id, country, year);
    },

    /**
     * Displays a confirmation dialog with specific record details.
     * Sets up the deletion context and shows the confirmation modal.
     * @param {string} id - The ID of the record to delete
     * @param {string} country - The country name for display in confirmation
     * @param {string} year - The year for display in confirmation
     */
    confirmDelete(id, country, year) {
        DashboardState.setDeleteId(id);
        document.getElementById('deleteMessage').textContent = 
            `Möchten Sie die Daten für ${country} (${year}) wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.`;
        document.getElementById('deleteModal').classList.remove('hidden');
        document.getElementById('deleteModal').classList.add('flex');
    },

    /**
     * Closes the delete confirmation modal and resets state.
     * Clears the deletion ID and hides the modal dialog.
     */
    closeModal() {
        DashboardState.setDeleteId(null);
        document.getElementById('deleteModal').classList.add('hidden');
        document.getElementById('deleteModal').classList.remove('flex');
    },

    /**
     * Executes the deletion operation by submitting a form.
     * Creates and submits a POST form with CSRF protection for secure deletion.
     */
    execute() {
        if (DashboardState.deleteId) {
            // Create form for secure deletion with CSRF protection
            const form = document.createElement('form');
            form.method = 'POST';
            form.action = `/scientist/delete/${DashboardState.deleteId}`;
            
            // Add CSRF token for security
            const csrfToken = Utils.getCsrfToken();
            if (csrfToken) {
                const csrfInput = document.createElement('input');
                csrfInput.type = 'hidden';
                csrfInput.name = '_csrf';
                csrfInput.value = csrfToken;
                form.appendChild(csrfInput);
            }
            
            // Submit the deletion form
            document.body.appendChild(form);
            form.submit();
        }
        this.closeModal();
    }
};
