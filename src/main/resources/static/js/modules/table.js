/**
 * Table Module
 * Provides table sorting functionality for the data table in the scientist dashboard.
 * Handles sorting of emission data by country name, year, and CO₂ values with
 * appropriate data type handling for numeric and text columns.
 */

export const TableModule = {
    /**
     * Sorts the data table by the specified column index.
     * Automatically detects data types and applies appropriate sorting logic:
     * - Columns 1 (Year) and 2 (CO₂): Numeric sorting with comma handling
     * - Other columns: Alphabetical sorting using locale-aware comparison
     * 
     * The sorting is performed in-place by reordering the table rows in the DOM.
     * Works with the main data table containing emission records.
     * 
     * @param {number} column - The zero-based column index to sort by
     *                         (0: Country, 1: Year, 2: CO₂ Emissions, etc.)
     */
    sortTable(column) {
        const table = document.getElementById('dataTable');
        if (!table) return;
        
        const tbody = table.querySelector('tbody');
        const rows = Array.from(tbody.querySelectorAll('tr'));
        
        // Sort rows based on column type and content
        rows.sort((a, b) => {
            const aVal = a.cells[column].textContent.trim();
            const bVal = b.cells[column].textContent.trim();
            
            // Numeric sorting for Year (column 1) and CO₂ emissions (column 2)
            if (column === 1 || column === 2) {
                // Remove commas from numbers and parse as float for proper numeric comparison
                return parseFloat(aVal.replace(/,/g, '')) - parseFloat(bVal.replace(/,/g, ''));
            }
            
            // Alphabetical sorting for text columns (country names, etc.)
            // Uses locale-aware comparison for proper international character handling
            return aVal.localeCompare(bVal);
        });
        
        // Rebuild table body with sorted rows
        tbody.innerHTML = '';
        rows.forEach(row => tbody.appendChild(row));
    }
};
