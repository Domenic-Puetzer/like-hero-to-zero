/**
 * Utility Module
 * Common helper functions and utilities used across the scientist dashboard.
 * Provides reusable functionality for notifications, CSRF handling, and data refresh.
 */

export const Utils = {
    /**
     * Displays a toast notification to the user.
     * Creates a temporary notification that automatically disappears after 3 seconds.
     * @param {string} message - The message to display to the user
     * @param {string} type - The type of notification ('success' or 'error')
     */
    showToast(message, type) {
        const toast = document.createElement('div');
        toast.className = `fixed top-4 right-4 px-6 py-3 rounded-lg shadow-lg z-50 transition-opacity duration-300 ${
            type === 'success' ? 'bg-green-500 text-white' : 'bg-red-500 text-white'
        }`;
        toast.textContent = message;
        
        document.body.appendChild(toast);
        
        // Auto-remove toast after 3 seconds with fade-out animation
        setTimeout(() => {
            toast.style.opacity = '0';
            setTimeout(() => {
                document.body.removeChild(toast);
            }, 300);
        }, 3000);
    },

    /**
     * Retrieves the CSRF token for secure form submissions.
     * Checks both meta tags and form inputs for the token.
     * @returns {string|null} The CSRF token value or null if not found
     */
    getCsrfToken() {
        // First try to get from meta tag
        const metaToken = document.querySelector('meta[name="_csrf"]');
        if (metaToken) {
            return metaToken.getAttribute('content');
        }
        // Fallback: try to get from hidden form input
        const inputToken = document.querySelector('input[name="_csrf"]');
        if (inputToken) {
            return inputToken.value;
        }
        return null;
    },

    /**
     * Refreshes the current page data by reloading the page.
     * Used after successful data operations to ensure UI shows latest state.
     */
    refreshData() {
        location.reload();
    }
};
