/**
 * Dashboard State Module
 * Centralized state management for the scientist dashboard application.
 * Manages modal states, loading indicators, filter states, and user interactions.
 * Provides a single source of truth for application state across different modules.
 */

export const DashboardState = {
    // Modal and interaction states - track which records are being manipulated
    deleteId: null,          // ID of record currently being deleted
    currentEditId: null,     // ID of record currently being edited
    
    // Application lifecycle states - track loading and refresh status
    isLoading: false,        // Global loading state for UI feedback
    lastRefresh: null,       // Timestamp of last data refresh for cache management
    
    // User interface filter states - maintain filter selections across operations
    currentSearch: '',       // Current search term in the filter input
    currentYearFilter: '',   // Currently selected year filter value
    
    /**
     * Sets the ID of the record currently marked for deletion.
     * Used by delete confirmation modals to track which record to remove.
     * @param {string|number|null} id - The ID of the record to delete, or null to clear
     */
    setDeleteId(id) {
        this.deleteId = id;
    },
    
    /**
     * Sets the ID of the record currently being edited.
     * Used by edit modals and forms to track which record is being modified.
     * @param {string|number|null} id - The ID of the record being edited, or null to clear
     */
    setCurrentEditId(id) {
        this.currentEditId = id;
    },
    
    /**
     * Updates the global loading state to control UI feedback.
     * Shows/hides loading indicators and disables interactive elements during operations.
     * @param {boolean} loading - True to show loading state, false to hide
     */
    setLoading(loading) {
        this.isLoading = loading;
    },
    
    /**
     * Updates the timestamp of the last data refresh.
     * Used for cache management and determining when to reload data from server.
     * Automatically sets the timestamp to the current date and time.
     */
    updateLastRefresh() {
        this.lastRefresh = new Date();
    },
    
    /**
     * Updates the current filter state with search and year values.
     * Maintains filter selections when navigating between pages or performing operations.
     * @param {string} search - The current search term
     * @param {string} year - The currently selected year filter
     */
    setFilterState(search, year) {
        this.currentSearch = search;
        this.currentYearFilter = year;
    },
    
    /**
     * Resets all state variables to their initial values.
     * Used during logout, navigation, or when clearing all user selections.
     * Ensures clean state when starting fresh operations.
     */
    reset() {
        this.deleteId = null;
        this.currentEditId = null;
        this.isLoading = false;
        this.currentSearch = '';
        this.currentYearFilter = '';
    }
};
