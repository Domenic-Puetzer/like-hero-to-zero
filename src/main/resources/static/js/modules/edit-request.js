/**
 * Edit Request Module
 * Handles creation of edit requests for data owned by other scientists.
 * Allows scientists to propose changes to emission data they didn't upload,
 * following a collaborative peer-review workflow.
 */

import { Utils } from './utils.js';

export const EditRequestModule = {
    /**
     * Shows the edit request modal with pre-filled data from the selected record.
     * Extracts data from the clicked button and populates the request form.
     * @param {HTMLElement} button - The edit request button containing data attributes
     */
    showModal(button) {
        const id = button.getAttribute('data-id');
        const country = button.getAttribute('data-country');
        const year = button.getAttribute('data-year');
        const co2 = button.getAttribute('data-co2');
        const source = button.getAttribute('data-source');
        const uploader = button.getAttribute('data-uploader');
        
        // Display the modal dialog
        document.getElementById('editRequestModal').classList.remove('hidden');
        document.getElementById('editRequestModal').classList.add('flex');
        
        // Pre-fill form with current data values
        document.getElementById('editRequestDataId').value = id;
        document.getElementById('editRequestCountry').value = country;
        document.getElementById('editRequestYear').value = year;
        document.getElementById('editRequestCo2').value = co2;
        document.getElementById('editRequestSource').value = source;
        
        // Set informative message with context
        document.getElementById('editRequestMessage').innerHTML = 
            `Sie möchten den Datensatz von <strong>${uploader}</strong> für <strong>${country} (${year})</strong> bearbeiten.`;
    },

    /**
     * Closes the edit request modal and resets the form.
     * Hides the modal and clears all form inputs.
     */
    closeModal() {
        document.getElementById('editRequestModal').classList.add('hidden');
        document.getElementById('editRequestModal').classList.remove('flex');
        document.getElementById('editRequestForm').reset();
    },

    /**
     * Submits the edit request form with CSRF protection and handles the response.
     * Validates the form, sends the request, and provides user feedback.
     */
    submit() {
        const form = document.getElementById('editRequestForm');
        const formData = new FormData(form);
        
        // Add CSRF token for security
        const csrfToken = Utils.getCsrfToken();
        if (csrfToken) {
            formData.append('_csrf', csrfToken);
        }
        
        // Submit the edit request via AJAX
        fetch('/scientist/request-edit', {
            method: 'POST',
            body: formData
        })
        .then(response => response.text())
        .then(result => {
            if (result === 'success') {
                Utils.showToast('Bearbeitungsanfrage wurde gesendet!', 'success');
                this.closeModal();
                // Reload page to show updated pending requests
                setTimeout(() => {
                    location.reload();
                }, 1500);
            } else {
                Utils.showToast('Fehler: ' + result, 'error');
            }
        })
        .catch(error => {
            Utils.showToast('Fehler beim Senden der Anfrage', 'error');
            console.error('Error:', error);
        });
    }
};
