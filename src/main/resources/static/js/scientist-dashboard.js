/**
 * Scientist Dashboard - Main Module Loader
 * Imports and orchestrates all dashboard modules for the scientist dashboard.
 * Handles initialization and exposes selected functions globally for UI event handlers.
 */
import { Utils } from './modules/utils.js';
import { DashboardState } from './modules/state.js';
import { DeleteModule } from './modules/delete.js';
import { DirectEditModule } from './modules/direct-edit.js';
import { EditRequestModule } from './modules/edit-request.js';
import { ApprovalModule } from './modules/approval.js';
import { TableModule } from './modules/table.js';
import { FilterModule } from './modules/filter.js';
import { PaginationInitModule } from './modules/pagination-init.js';

/**
 * DashboardInit handles the initialization of all dashboard modules after the DOM is ready.
 * @namespace DashboardInit
 */
const DashboardInit = {
    /**
     * Initializes all modules and sets up event listeners after DOM content is loaded.
     * This ensures all dashboard features are ready for user interaction.
     */
    init() {
        document.addEventListener('DOMContentLoaded', () => {
            console.log('DOM Content Loaded - Starting initialization...');
            
            // Initialize filter module event listeners
            FilterModule.initializeEventListeners();
            
            // Robust pagination initialization for tables and cards
            PaginationInitModule.initializeAll();
            
            // Update dashboard state timestamp
            DashboardState.updateLastRefresh();
            
            console.log('Scientist Dashboard ES6 modules loaded successfully!');
            console.log('Loaded modules:', {
                Utils: true,
                DashboardState: true,
                DeleteModule: true,
                DirectEditModule: true,
                EditRequestModule: true,
                ApprovalModule: true,
                TableModule: true,
                FilterModule: true,
                PaginationModule: true,
                PaginationInitModule: true
            });
            console.log('Dashboard State initialized:', DashboardState);
        });
    }
};

/**
 * Expose selected module functions to the global scope for use in HTML event handlers.
 * This allows UI elements to trigger modal dialogs, table sorting, filtering, and data refresh.
 */
window.showDeleteModal = DeleteModule.showModal.bind(DeleteModule);
window.closeDeleteModal = DeleteModule.closeModal.bind(DeleteModule);
window.executeDelete = DeleteModule.execute.bind(DeleteModule);

window.showDirectEditModal = DirectEditModule.showModal.bind(DirectEditModule);
window.closeDirectEditModal = DirectEditModule.closeModal.bind(DirectEditModule);
window.submitDirectEdit = DirectEditModule.submit.bind(DirectEditModule);

window.showEditRequestModal = EditRequestModule.showModal.bind(EditRequestModule);
window.closeEditRequestModal = EditRequestModule.closeModal.bind(EditRequestModule);
window.submitEditRequest = EditRequestModule.submit.bind(EditRequestModule);

window.showApproveModal = ApprovalModule.showApproveModal.bind(ApprovalModule);
window.closeApproveModal = ApprovalModule.closeApproveModal.bind(ApprovalModule);
window.showRejectModal = ApprovalModule.showRejectModal.bind(ApprovalModule);
window.closeRejectModal = ApprovalModule.closeRejectModal.bind(ApprovalModule);

window.sortTable = TableModule.sortTable.bind(TableModule);
window.applyFilters = FilterModule.applyFilters.bind(FilterModule);
window.clearFilters = FilterModule.clearFilters.bind(FilterModule);
window.refreshData = Utils.refreshData.bind(Utils);
window.reinitPagination = PaginationInitModule.forceReinit.bind(PaginationInitModule);

// Initialize the dashboard
DashboardInit.init();