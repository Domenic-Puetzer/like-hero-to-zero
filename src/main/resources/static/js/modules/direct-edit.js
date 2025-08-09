/**
 * Direct Edit Module
 * Handles direct editing of scientist's own emission data records.
 * Allows scientists to modify their uploaded data without requiring approval
 * from other scientists, streamlining the data correction process.
 */

import { Utils } from './utils.js';
import { DashboardState } from './state.js';

export const DirectEditModule = {
    /**
     * Shows the direct edit modal with pre-filled form data.
     * Extracts data attributes from the clicked button and populates the edit form.
     * @param {HTMLElement} button - The edit button that was clicked, containing data attributes
     */
    showModal(button) {
        const id = button.getAttribute('data-id');
        const country = button.getAttribute('data-country');
        const year = button.getAttribute('data-year');
        const co2 = button.getAttribute('data-co2');
        const source = button.getAttribute('data-source');
        
        // Update application state with current edit ID
        DashboardState.setCurrentEditId(id);
        
        // Show modal dialog
        document.getElementById('directEditModal').classList.remove('hidden');
        document.getElementById('directEditModal').classList.add('flex');
        
        // Pre-fill form fields with existing data
        document.getElementById('directEditDataId').value = id;
        document.getElementById('directEditCountry').value = country;
        document.getElementById('directEditYear').value = year;
        document.getElementById('directEditCo2').value = co2;
        document.getElementById('directEditSource').value = source;
        
        // Set informative message for user context
        document.getElementById('directEditMessage').innerHTML = 
            `Sie bearbeiten den Datensatz für <strong>${country} (${year})</strong>.`;
    },

    /**
     * Closes the direct edit modal and resets form state.
     * Clears the current edit ID and resets the form to initial state.
     */
    closeModal() {
        DashboardState.setCurrentEditId(null);
        document.getElementById('directEditModal').classList.add('hidden');
        document.getElementById('directEditModal').classList.remove('flex');
        document.getElementById('directEditForm').reset();
    },

    /**
     * Submits the direct edit form via AJAX and handles the response.
     * Shows loading state during submission and provides user feedback.
     * Automatically refreshes the page on successful update.
     */
    submit() {
        const form = document.getElementById('directEditForm');
        const formData = new FormData(form);
        const dataId = DashboardState.currentEditId;
        
        // Set loading state to prevent multiple submissions
        DashboardState.setLoading(true);
        
        // Show loading state on submit button
        const submitButton = form.querySelector('button[onclick="submitDirectEdit()"]');
        const originalText = submitButton.textContent;
        submitButton.textContent = 'Speichert...';
        submitButton.disabled = true;
        
        // Submit form data via AJAX with page reload on success
        fetch(`/scientist/edit/${dataId}`, {
            method: 'POST',
            body: formData
        })
        .then(response => response.text())
        .then(result => {
            DashboardState.setLoading(false);
            submitButton.textContent = originalText;
            submitButton.disabled = false;
            
            if (result === 'success') {
                Utils.showToast('Datensatz wurde erfolgreich aktualisiert!', 'success');
                this.closeModal();
                DashboardState.updateLastRefresh();
                
                // Reload page to show updated data (similar to edit request approval)
                setTimeout(() => {
                    location.reload();
                }, 1000);
            } else {
                Utils.showToast('Fehler: ' + result, 'error');
            }
        })
        .catch(error => {
            DashboardState.setLoading(false);
            submitButton.textContent = originalText;
            submitButton.disabled = false;
            Utils.showToast('Netzwerkfehler: ' + error.message, 'error');
        });
    }
};
