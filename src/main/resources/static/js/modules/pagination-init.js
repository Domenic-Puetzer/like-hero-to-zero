/**
 * Pagination Initialization Module
 * Handles robust initialization of pagination for tables and cards with multiple fallback strategies.
 * Provides automatic detection of content loading and graceful handling of dynamic content.
 */

import { PaginationModule } from './pagination.js';

export const PaginationInitModule = {
    /**
     * Robustly initializes pagination for all tables and card containers.
     * Attempts immediate initialization and falls back to multiple strategies if content is not yet loaded.
     * Supports data tables, system tables, and card-based layouts with different pagination requirements.
     */
    initializeAll() {
        const tryInitPagination = () => {
            console.log('Attempting pagination initialization...');
            let successCount = 0;
            
            // Initialize dataTable pagination (scientist dashboard data)
            const dataTable = document.getElementById('dataTable');
            if (dataTable) {
                const rows = dataTable.querySelectorAll('tbody tr.data-row');
                console.log(`Found ${rows.length} data rows in dataTable`);
                
                if (rows.length > 0) {
                    console.log('Initializing pagination for dataTable');
                    PaginationModule.init('dataTable', 10);
                    window.paginationModule = PaginationModule;
                    successCount++;
                } else {
                    console.log('Waiting for dataTable rows to load...');
                }
            }
            
            // Initialize systemDataTable pagination (admin system data)
            const systemDataTable = document.getElementById('systemDataTable');
            if (systemDataTable) {
                const rows = systemDataTable.querySelectorAll('tbody tr');
                console.log(`Found ${rows.length} rows in systemDataTable`);
                
                if (rows.length > 0) {
                    console.log('Initializing pagination for systemDataTable');
                    PaginationModule.init('systemDataTable', 10);
                    successCount++;
                } else {
                    console.log('Waiting for systemDataTable rows to load...');
                }
            }
            
            // Initialize myRequestsContainer card pagination (edit requests, 1 card per page) 
            const myRequestsContainer = document.getElementById('myRequestsContainer');
            if (myRequestsContainer) {
                const cards = myRequestsContainer.querySelectorAll('.request-card');
                console.log(`Found ${cards.length} cards in myRequestsContainer`);
                
                if (cards.length > 0) {
                    console.log('Initializing card pagination for myRequestsContainer');
                    PaginationModule.init('myRequestsContainer', 1, true); // true = cardMode
                    successCount++;
                } else {
                    console.log('Waiting for myRequestsContainer cards to load...');
                }
            }
            
            return successCount > 0; // At least one table/container successfully initialized
        };
        
        // Try immediate initialization first
        if (tryInitPagination()) {
            console.log('Pagination initialized immediately');
            return;
        }
        
        // Use fallback strategies if immediate initialization fails
        this.setupFallbackStrategies(tryInitPagination);
    },
    
    /**
     * Sets up fallback strategies for pagination initialization when content loads dynamically.
     * Uses MutationObserver to detect when table rows or cards are added to the DOM.
     * Provides graceful handling of AJAX-loaded content and slow server responses.
     * 
     * @param {Function} tryInitFunction - Function to attempt pagination initialization
     */
    setupFallbackStrategies(tryInitFunction) {
        // Observer for DOM changes to detect dynamically loaded content
        const observer = new MutationObserver((mutations) => {
            let shouldTryInit = false;
            mutations.forEach((mutation) => {
                if (mutation.type === 'childList' && mutation.addedNodes.length > 0) {
                    // Check if relevant table elements were added
                    const addedElements = Array.from(mutation.addedNodes);
                    const hasTableElements = addedElements.some(node => 
                        node.nodeType === Node.ELEMENT_NODE && 
                        (node.tagName === 'TR' || node.querySelector('tr'))
                    );
                    
                    if (hasTableElements) {
                        shouldTryInit = true;
                    }
                }
            });
            
            if (shouldTryInit && tryInitFunction()) {
                console.log('Pagination initialized via MutationObserver');
                observer.disconnect();
            }
        });
        
        // Start observer - monitor only relevant containers for performance
        const tablesContainer = document.querySelector('main') || document.body;
        observer.observe(tablesContainer, {
            childList: true,
            subtree: true
        });
        
        // Set up time-delayed fallback attempts
        this.setupTimeoutFallbacks(tryInitFunction, observer);
    },
    
    /**
     * Sets up timeout-based fallback attempts for pagination initialization.
     * Provides multiple retry attempts at increasing intervals to handle slow-loading content.
     * Automatically disconnects observer after final attempt to prevent memory leaks.
     * 
     * @param {Function} tryInitFunction - Function to attempt pagination initialization
     * @param {MutationObserver} observer - DOM observer to disconnect after successful init
     */
    setupTimeoutFallbacks(tryInitFunction, observer) {
        const timeouts = [200, 500, 1000];
        
        timeouts.forEach((delay, index) => {
            setTimeout(() => {
                if (tryInitFunction()) {
                    console.log(`Pagination initialized via setTimeout (${delay}ms)`);
                    observer.disconnect();
                } else if (index === timeouts.length - 1) {
                    // Final attempt - stop observer to prevent memory leaks
                    console.log('Pagination initialization completed with timeouts');
                    observer.disconnect();
                }
            }, delay);
        });
    },
    
    /**
     * Forces re-initialization of pagination for manual refresh scenarios.
     * Removes existing pagination controls and re-runs the initialization process.
     * Useful when table content is dynamically updated or when manual refresh is needed.
     */
    forceReinit() {
        console.log('Force re-initializing pagination...');
        
        // Remove existing pagination controls to prevent duplicates
        document.querySelectorAll('.pagination-controls').forEach(control => {
            control.remove();
        });
        
        // Re-run the full initialization process
        this.initializeAll();
    }
};
