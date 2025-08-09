/**
 * Filter Module
 * Provides search and filtering functionality for the scientist dashboard data table.
 * Allows filtering by country name/year search terms and specific year selection.
 * Integrates with pagination system to maintain proper page navigation after filtering.
 */

import { DashboardState } from './state.js';

export const FilterModule = {
    /**
     * Applies search and year filters to the data table rows.
     * Filters are applied in real-time as user types or selects options.
     * Updates application state and refreshes pagination after filtering.
     * 
     * Search functionality:
     * - Case-insensitive search in country names and years
     * - Partial matches are supported
     * 
     * Year filter:
     * - Shows only records from selected year
     * - Empty selection shows all years
     */
    applyFilters() {
        const searchFilter = document.getElementById('searchFilter');
        const yearFilter = document.getElementById('yearFilter');
        
        // Exit early if filter elements are not available
        if (!searchFilter || !yearFilter) return;
        
        const searchTerm = searchFilter.value.toLowerCase();
        const yearValue = yearFilter.value;
        
        // Update application state with current filter values
        DashboardState.setFilterState(searchTerm, yearValue);
        
        const rows = document.querySelectorAll('.data-row');
        
        // Process each data row for filter matching
        rows.forEach(row => {
            const country = row.cells[0].textContent.toLowerCase();
            const year = row.cells[1].textContent;
            
            // Check if row matches search criteria (country or year contains search term)
            const matchesSearch = country.includes(searchTerm) || year.includes(searchTerm);
            
            // Check if row matches year filter (empty filter matches all years)
            const matchesYear = !yearValue || year === yearValue;
            
            // Show row only if it matches both search and year filters
            row.style.display = (matchesSearch && matchesYear) ? '' : 'none';
        });
        
        // Refresh pagination to account for filtered results
        if (window.paginationModule) {
            window.paginationModule.refresh('dataTable');
        }
    },

    /**
     * Clears all active filters and shows all data rows.
     * Resets filter input fields and application state.
     * Refreshes pagination to show all available pages.
     */
    clearFilters() {
        const searchFilter = document.getElementById('searchFilter');
        const yearFilter = document.getElementById('yearFilter');
        
        // Reset filter input values
        if (searchFilter) searchFilter.value = '';
        if (yearFilter) yearFilter.value = '';
        
        // Reset application state to empty filters
        DashboardState.setFilterState('', '');
        
        // Show all data rows
        document.querySelectorAll('.data-row').forEach(row => {
            row.style.display = '';
        });
        
        // Refresh pagination to show all pages
        if (window.paginationModule) {
            window.paginationModule.refresh('dataTable');
        }
    },

    /**
     * Initializes event listeners for filter controls.
     * Sets up real-time filtering on input changes and dropdown selections.
     * Should be called during page initialization to activate filtering functionality.
     */
    initializeEventListeners() {
        const searchFilter = document.getElementById('searchFilter');
        const yearFilter = document.getElementById('yearFilter');
        
        // Set up real-time search filtering on text input
        if (searchFilter) {
            searchFilter.addEventListener('input', this.applyFilters.bind(this));
        }
        
        // Set up year filter on dropdown selection change
        if (yearFilter) {
            yearFilter.addEventListener('change', this.applyFilters.bind(this));
        }
    }
};
