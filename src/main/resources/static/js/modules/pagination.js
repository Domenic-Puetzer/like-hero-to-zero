/**
 * Pagination Module
 * Provides client-side pagination functionality for tables and card layouts.
 * Supports multiple containers on the same page with independent pagination states.
 * Handles both table rows and card elements with configurable items per page.
 */

export const PaginationModule = {
    // Maintains separate pagination state for each container
    containerStates: new Map(),

    /**
     * Retrieves or creates pagination state for a specific container.
     * Each container maintains its own pagination settings and current page.
     * @param {string} containerId - The ID of the container element
     * @returns {Object} The pagination state object for the container
     */
    getState(containerId) {
        if (!this.containerStates.has(containerId)) {
            this.containerStates.set(containerId, {
                currentPage: 1,
                itemsPerPage: 10,
                totalItems: 0,
                totalPages: 0,
                isCardMode: false
            });
        }
        return this.containerStates.get(containerId);
    },

    /**
     * Initializes pagination for a table or card container.
     * Sets up the pagination controls and calculates initial state.
     * @param {string} containerId - The ID of the container to paginate
     * @param {number} itemsPerPage - Number of items to show per page (default: 10)
     * @param {boolean} cardMode - Whether to use card mode (1 per page) or table mode
     */
    init(containerId, itemsPerPage = 10, cardMode = false) {
        const state = this.getState(containerId);
        state.itemsPerPage = itemsPerPage;
        state.currentPage = 1;
        state.isCardMode = cardMode;
        
        const container = document.getElementById(containerId);
        if (!container) {
            console.warn(`Pagination: Container with ID "${containerId}" not found`);
            return;
        }
        
        let items;
        if (cardMode) {
            // Card mode: Look for .request-card elements
            items = container.querySelectorAll('.request-card');
            state.itemsPerPage = 1; // Always 1 item per page for cards
        } else {
            // Table mode: Look for table rows
            const selector = containerId === 'dataTable' ? 'tbody tr.data-row' : 'tbody tr';
            items = container.querySelectorAll(selector);
        }
        
        state.totalItems = items.length;
        state.totalPages = Math.ceil(state.totalItems / state.itemsPerPage);
        
        console.log(`Pagination initialized for ${containerId}:`, {
            mode: cardMode ? 'Cards' : 'Table',
            totalItems: state.totalItems,
            itemsPerPage: state.itemsPerPage,
            totalPages: state.totalPages,
            itemsFound: items.length
        });
        
        if (state.totalItems > 0) {
            // Hide all items initially for better performance
            this.hideAllItems(containerId);
            this.createPaginationControls(containerId);
            this.showPage(1, containerId);
        } else {
            console.log(`No items found for pagination in ${containerId}`);
        }
    },

    /**
     * Hides all items initially to prepare for pagination display.
     * Improves performance by hiding items that won't be shown on current page.
     * @param {string} containerId - The ID of the container whose items to hide
     */
    hideAllItems(containerId) {
        const container = document.getElementById(containerId);
        if (!container) return;
        
        const state = this.getState(containerId);
        
        let items;
        if (state.isCardMode) {
            items = container.querySelectorAll('.request-card');
        } else {
            const selector = containerId === 'dataTable' ? 'tbody tr.data-row' : 'tbody tr';
            items = container.querySelectorAll(selector);
        }
        
        items.forEach(item => {
            item.style.display = 'none';
        });
        
        console.log(`Hidden ${items.length} items in ${containerId} for pagination`);
    },

    /**
     * Creates pagination control elements including navigation buttons and page selectors.
     * Builds the complete pagination UI with items-per-page selector for tables,
     * previous/next buttons, and page number buttons.
     * @param {string} containerId - The ID of the container to create controls for
     */
    createPaginationControls(containerId) {
        const container = document.getElementById(containerId);
        if (!container) return;
        
        const state = this.getState(containerId);
        
        // Remove existing pagination to avoid duplicates
        const existingPagination = container.parentNode.querySelector('.pagination-controls');
        if (existingPagination) {
            existingPagination.remove();
        }
        
        // Create main pagination container
        const paginationContainer = document.createElement('div');
        paginationContainer.className = 'pagination-controls flex justify-between items-center mt-4 p-4 bg-gray-50 rounded-lg';
        
        // Information text showing current page status
        const infoText = document.createElement('div');
        infoText.className = 'text-sm text-gray-600';
        infoText.id = `${containerId}-pagination-info`;
        
        // Container for navigation controls
        const controlsContainer = document.createElement('div');
        controlsContainer.className = 'flex items-center space-x-2';
        
        // Items per page selector (only for table mode, not cards)
        if (!state.isCardMode) {
            const itemsPerPageContainer = document.createElement('div');
            itemsPerPageContainer.className = 'flex items-center space-x-2 mr-4';
            
            const itemsLabel = document.createElement('span');
            itemsLabel.className = 'text-sm text-gray-600';
            itemsLabel.textContent = 'Einträge pro Seite:';
            
            const itemsSelect = document.createElement('select');
            itemsSelect.className = 'px-2 py-1 border border-gray-300 rounded text-sm';
            itemsSelect.id = `${containerId}-items-per-page`;
            
            // Add options for different page sizes
            [5, 10, 20, 50].forEach(value => {
                const option = document.createElement('option');
                option.value = value;
                option.textContent = value;
                option.selected = value === state.itemsPerPage;
                itemsSelect.appendChild(option);
            });
            
            // Handle page size changes
            itemsSelect.addEventListener('change', (e) => {
                state.itemsPerPage = parseInt(e.target.value);
                state.totalPages = Math.ceil(state.totalItems / state.itemsPerPage);
                state.currentPage = 1; // Reset to first page
                this.showPage(1, containerId);
                this.updatePaginationControls(containerId);
            });
            
            itemsPerPageContainer.appendChild(itemsLabel);
            itemsPerPageContainer.appendChild(itemsSelect);
            controlsContainer.appendChild(itemsPerPageContainer);
        }
        
        // Previous page button
        const prevButton = document.createElement('button');
        prevButton.className = 'px-3 py-1 bg-gray-300 text-gray-700 rounded text-sm hover:bg-gray-400 transition-colors';
        prevButton.textContent = state.isCardMode ? '« Vorherige' : '« Zurück';
        prevButton.id = `${containerId}-prev-button`;
        prevButton.addEventListener('click', () => this.previousPage(containerId));
        
        // Container for page number buttons
        const pageNumbers = document.createElement('div');
        pageNumbers.className = 'flex space-x-1';
        pageNumbers.id = `${containerId}-page-numbers`;
        
        // Next page button
        const nextButton = document.createElement('button');
        nextButton.className = 'px-3 py-1 bg-gray-300 text-gray-700 rounded text-sm hover:bg-gray-400 transition-colors';
        nextButton.textContent = state.isCardMode ? 'Nächste »' : 'Weiter »';
        nextButton.id = `${containerId}-next-button`;
        nextButton.addEventListener('click', () => this.nextPage(containerId));
        
        // Assemble all controls
        controlsContainer.appendChild(prevButton);
        controlsContainer.appendChild(pageNumbers);
        controlsContainer.appendChild(nextButton);
        
        paginationContainer.appendChild(infoText);
        paginationContainer.appendChild(controlsContainer);
        
        // Insert pagination controls after the container
        container.parentNode.insertBefore(paginationContainer, container.nextSibling);
        
        this.updatePaginationControls(containerId);
    },

    /**
     * Updates the state and appearance of pagination controls.
     * Refreshes info text, button states, and page number display
     * based on current pagination state.
     * @param {string} containerId - The ID of the container to update controls for
     */
    updatePaginationControls(containerId) {
        const state = this.getState(containerId);
        
        // Update information text showing current page status
        const infoText = document.getElementById(`${containerId}-pagination-info`);
        if (infoText) {
            if (state.isCardMode) {
                infoText.textContent = `Anfrage ${state.currentPage} von ${state.totalItems}`;
            } else {
                const start = (state.currentPage - 1) * state.itemsPerPage + 1;
                const end = Math.min(state.currentPage * state.itemsPerPage, state.totalItems);
                infoText.textContent = `Zeige ${start}-${end} von ${state.totalItems} Einträgen`;
            }
        }
        
        // Update previous/next button states
        const prevButton = document.getElementById(`${containerId}-prev-button`);
        const nextButton = document.getElementById(`${containerId}-next-button`);
        
        if (prevButton) {
            prevButton.disabled = state.currentPage === 1;
            prevButton.className = state.currentPage === 1 
                ? 'px-3 py-1 bg-gray-200 text-gray-400 rounded text-sm cursor-not-allowed'
                : 'px-3 py-1 bg-gray-300 text-gray-700 rounded text-sm hover:bg-gray-400 transition-colors cursor-pointer';
        }
        
        if (nextButton) {
            nextButton.disabled = state.currentPage === state.totalPages;
            nextButton.className = state.currentPage === state.totalPages
                ? 'px-3 py-1 bg-gray-200 text-gray-400 rounded text-sm cursor-not-allowed'
                : 'px-3 py-1 bg-gray-300 text-gray-700 rounded text-sm hover:bg-gray-400 transition-colors cursor-pointer';
        }
        
        // Update page number buttons
        this.updatePageNumbers(containerId);
    },

    /**
     * Updates the page number buttons in the pagination controls.
     * Shows a limited number of page buttons around the current page
     * for better usability with large datasets.
     * @param {string} containerId - The ID of the container to update page numbers for
     */
    updatePageNumbers(containerId) {
        const state = this.getState(containerId);
        const pageNumbersContainer = document.getElementById(`${containerId}-page-numbers`);
        if (!pageNumbersContainer) return;
        
        pageNumbersContainer.innerHTML = '';
        
        const maxVisible = 5; // Maximum number of page buttons to show
        let startPage = Math.max(1, state.currentPage - Math.floor(maxVisible / 2));
        let endPage = Math.min(state.totalPages, startPage + maxVisible - 1);
        
        // Adjust start page if we're near the end to always show maxVisible buttons
        if (endPage - startPage < maxVisible - 1) {
            startPage = Math.max(1, endPage - maxVisible + 1);
        }
        
        // Create page number buttons
        for (let i = startPage; i <= endPage; i++) {
            const pageButton = document.createElement('button');
            pageButton.className = i === state.currentPage
                ? 'px-3 py-1 bg-blue-500 text-white rounded text-sm'
                : 'px-3 py-1 bg-gray-200 text-gray-700 rounded text-sm hover:bg-gray-300 transition-colors';
            pageButton.textContent = i;
            pageButton.addEventListener('click', () => this.showPage(i, containerId));
            pageNumbersContainer.appendChild(pageButton);
        }
    },

    /**
     * Displays a specific page of items by showing/hiding appropriate elements.
     * Handles both card mode (1 item per page) and table mode (multiple items per page).
     * @param {number} pageNumber - The page number to display (1-based)
     * @param {string} containerId - The ID of the container whose page to show
     */
    showPage(pageNumber, containerId) {
        const container = document.getElementById(containerId);
        if (!container) return;
        
        const state = this.getState(containerId);
        state.currentPage = pageNumber;
        
        let items;
        if (state.isCardMode) {
            items = container.querySelectorAll('.request-card');
            // Card mode: Show only one card per page
            items.forEach((item, index) => {
                if (index === pageNumber - 1) {
                    item.style.display = 'block';
                } else {
                    item.style.display = 'none';
                }
            });
        } else {
            // Table mode: Show multiple rows per page
            const selector = containerId === 'dataTable' ? 'tbody tr.data-row' : 'tbody tr';
            items = container.querySelectorAll(selector);
            
            const startIndex = (pageNumber - 1) * state.itemsPerPage;
            const endIndex = startIndex + state.itemsPerPage;
            
            // Show/hide items based on page range
            items.forEach((item, index) => {
                item.classList.remove('pagination-visible');
                if (index >= startIndex && index < endIndex) {
                    item.style.display = '';
                    item.classList.add('pagination-visible');
                } else {
                    item.style.display = 'none';
                }
            });
        }
        
        console.log(`📄 Showing page ${pageNumber} of ${state.totalPages} for ${containerId}`);
        
        this.updatePaginationControls(containerId);
    },

    /**
     * Navigates to the previous page if available.
     * @param {string} containerId - The ID of the container to navigate
     */
    previousPage(containerId) {
        const state = this.getState(containerId);
        if (state.currentPage > 1) {
            this.showPage(state.currentPage - 1, containerId);
        }
    },

    /**
     * Navigates to the next page if available.
     * @param {string} containerId - The ID of the container to navigate
     */
    nextPage(containerId) {
        const state = this.getState(containerId);
        if (state.currentPage < state.totalPages) {
            this.showPage(state.currentPage + 1, containerId);
        }
    },

    /**
     * Refreshes pagination after table content changes.
     * Recalculates totals and updates controls to reflect current state.
     * Should be called after filtering or adding/removing items.
     * @param {string} containerId - The ID of the container to refresh
     */
    refresh(containerId) {
        const container = document.getElementById(containerId);
        if (!container) return;
        
        const state = this.getState(containerId);
        
        let allItems, visibleItems;
        if (state.isCardMode) {
            allItems = container.querySelectorAll('.request-card');
            visibleItems = Array.from(allItems).filter(item => item.style.display !== 'none');
        } else {
            const selector = containerId === 'dataTable' ? 'tbody tr.data-row' : 'tbody tr';
            allItems = container.querySelectorAll(selector);
            visibleItems = Array.from(allItems).filter(item => item.style.display !== 'none');
        }
        
        state.totalItems = visibleItems.length;
        state.totalPages = Math.ceil(state.totalItems / state.itemsPerPage);
        
        // Adjust current page if it's beyond available pages
        if (state.currentPage > state.totalPages && state.totalPages > 0) {
            state.currentPage = state.totalPages;
        }
        
        this.updatePaginationControls(containerId);
        this.showPage(state.currentPage, containerId);
    }
};
