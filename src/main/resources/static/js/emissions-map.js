/**
 * Emissions Map Module
 * Provides interactive world map visualization of CO₂ emissions data using Leaflet.js.
 * Loads and displays country-level CO₂ emissions on a map with dynamic marker coloring
 * and popups based on emission values. Integrates with backend API and scientist uploads.
 */

let emissionsData = [];
let selectedCountry = null;
let map;
let markersLayer;
let countryCoords = {};

/**
 * Loads country coordinates from a JSON file and stores them in a lookup object.
 * Converts the JSON data format to a simple country-to-coordinates mapping
 * for efficient marker placement on the map.
 * @returns {Promise<Object>} A mapping of country names to [lat, lng] arrays
 */
async function loadCountryCoordinates() {
    try {
        const response = await fetch('/json/country-coords.json');
        const data = await response.json();
        console.log(data);
        
        // Convert JSON to a simple format: { country: [lat, lng] }
        Object.entries(data.countries).forEach(([country, info]) => {
            countryCoords[country] = [info.lat, info.lng];
        });
        
        console.log('Loaded coordinates for', Object.keys(countryCoords).length, 'countries');
        return countryCoords;
    } catch (error) {
        console.error('Error loading coordinates:', error);
        return {};
    }
}

/**
 * Initializes the Leaflet map and adds the base tile layer and marker layer group.
 * Sets up the world map view with OpenStreetMap tiles and creates an empty
 * marker layer for displaying emissions data.
 */
function initMap() {
    map = L.map('worldMap', {
        worldCopyJump: false,
    }).setView([20, 0], 2);
    
    // Basis-Kartenlayer
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 18,
        attribution: '© OpenStreetMap contributors',
    }).addTo(map);
    
    // Layer für Marker
    markersLayer = L.layerGroup().addTo(map);
}

/**
 * Returns a color hex code based on the CO₂ emission value.
 * Used for dynamic marker coloring on the map with a graduated color scale
 * from gray (no data) to dark purple (highest emissions).
 * @param {number} co2Value - The CO₂ emission value in kilotons
 * @returns {string} The corresponding color hex code
 */
function getColor(co2Value) {
    if (!co2Value || co2Value === 0) return '#cccccc';
    if (co2Value < 100000) return '#2166ac';
    if (co2Value < 500000) return '#5aae61';
    if (co2Value < 2000000) return '#a50026';
    if (co2Value < 5000000) return '#d73027';
    return '#40004b';
}

/**
 * Loads emission data for a specific country from the backend API and displays it in the detail panel.
 * Also zooms to the selected country on the map and highlights its marker.
 * Shows loading spinner during data fetch and handles errors gracefully.
 * @param {string} countryName - The name of the country to load data for
 */
async function loadCountryData(countryName) {
    try {
    console.log('Loading data for:', countryName);
        
        const loadingSpinner = document.getElementById('loading-spinner');
        if (loadingSpinner) {
            loadingSpinner.style.display = 'flex';
        }
        
        const response = await fetch(`/api/emissions/country/${encodeURIComponent(countryName)}`);
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: Länderdaten nicht gefunden`);
        }
        
        const data = await response.json();
        console.log('Received data:', data);
        
        displayCountryPanel(countryName, data);
        
        // Zoom to the selected country's position
        const coords = countryCoords[countryName];
        if (coords) {
            map.setView(coords, 5);
        }
        
        highlightCountryMarker(countryName);
        selectedCountry = countryName;
        
    } catch (error) {
        console.error('Error loading country data:', error);
        
        // Fallback: Show panel with "No data" message
        displayCountryPanel(countryName, []);
        
    } finally {
        const loadingSpinner = document.getElementById('loading-spinner');
        if (loadingSpinner) {
            loadingSpinner.style.display = 'none';
        }
    }
}

/**
 * Displays the country detail panel with emission data for the selected country.
 * Populates the data table with emissions sorted by year, adjusts the map layout,
 * and shows appropriate sections based on data availability.
 * @param {string} countryName - The name of the country
 * @param {Array<Object>} emissions - Array of emission data objects for the country
 */
function displayCountryPanel(countryName, emissions) {
    const panel = document.getElementById('country-panel');
    const mapContainer = document.getElementById('map-container');
    const countryNameElement = document.getElementById('country-name');
    const dataSection = document.getElementById('data-available-section');
    const noDataSection = document.getElementById('no-data-section');
    const tableBody = document.getElementById('emissions-table-body');
    const exportBtn = document.getElementById('export-data-btn');
    
    console.log('Displaying panel for:', countryName, 'with', emissions?.length || 0, 'records');
    
    // Adjust layout: shrink map from 12 to 8 columns for detail panel
    mapContainer.classList.remove('col-span-12');
    mapContainer.classList.add('lg:col-span-8');
    
    // Set country name in detail panel
    countryNameElement.textContent = countryName;
    
    // Clear previous table rows
    tableBody.innerHTML = '';
    
    if (emissions && emissions.length > 0) {
        // Data available: show table and hide 'no data' message
        dataSection.style.display = 'block';
        noDataSection.style.display = 'none';
        
        // Fill table, sorted by year (most recent first)
        emissions.sort((a, b) => b.year - a.year);
        
        emissions.forEach(emission => {
            const row = document.createElement('tr');
            row.className = 'hover:bg-blue-50 transition-colors';
            
            const yearCell = document.createElement('td');
            yearCell.className = 'px-4 py-2 font-medium text-blue-900';
            yearCell.textContent = emission.year;
            
            const co2Cell = document.createElement('td');
            co2Cell.className = 'px-4 py-2';
            co2Cell.textContent = emission.co2EmissionKt.toLocaleString('de-DE');
            
            // New column for data source
            const sourceCell = document.createElement('td');
            sourceCell.className = 'px-4 py-2 text-xs';
            
            // Check if data is from scientist upload, API, or database
            if (emission.dataSource && emission.dataSource.includes('Scientist Upload')) {
                sourceCell.innerHTML = '<span class="bg-green-100 text-green-800 px-2 py-1 rounded-full">🔬 Scientist</span>';
            } else if (emission.dataSource && emission.dataSource.includes('API')) {
                sourceCell.innerHTML = '<span class="bg-blue-100 text-blue-800 px-2 py-1 rounded-full">🌐 API</span>';
            } else {
                sourceCell.innerHTML = '<span class="bg-gray-100 text-gray-800 px-2 py-1 rounded-full">📊 Datenbank</span>';
            }
            
            row.appendChild(yearCell);
            row.appendChild(co2Cell);
            row.appendChild(sourceCell);
            tableBody.appendChild(row);
        });
        
        // Update export button event handler
        exportBtn.onclick = () => downloadCountryData(countryName);
        
    } else {
        console.log('No data for', countryName);
        
        // No data: show 'no data' message and hide table
        dataSection.style.display = 'none';
        noDataSection.style.display = 'block';
    }
    
    // Show detail panel
    panel.style.display = 'block';
}

/**
 * Closes the country detail panel and resets the map layout and marker highlights.
 * Restores the map to full width and resets the view to world overview.
 */
function closeCountryPanel() {
    const panel = document.getElementById('country-panel');
    const mapContainer = document.getElementById('map-container');
    
    // Hide detail panel
    panel.style.display = 'none';
    
    // Expand map to full width
    mapContainer.classList.remove('lg:col-span-8');
    mapContainer.classList.add('col-span-12');
    
    // Reset map view and marker highlights
    resetMarkerHighlight();
    selectedCountry = null;
    if (map) {
        map.setView([20, 0], 2);
    }
}

/**
 * Sets up event listeners for UI controls, including back buttons and search input.
 * Attaches click handlers for panel navigation and Enter key handler for search.
 */
function setupEventListeners() {
    // Back buttons for closing detail panel
    const backBtn = document.getElementById('back-to-map-btn');
    const backNoDataBtn = document.getElementById('back-to-map-no-data-btn');
    
    if (backBtn) {
        backBtn.addEventListener('click', closeCountryPanel);
    }
    if (backNoDataBtn) {
        backNoDataBtn.addEventListener('click', closeCountryPanel);
    }
    
    // Search input: trigger search on Enter key
    const searchInput = document.getElementById('countrySearch');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                searchCountry();
            }
        });
    }
}

/**
 * Highlights the marker for the selected country on the map.
 * Changes the selected marker to gold color while resetting others to default.
 * @param {string} countryName - The name of the country to highlight
 */
function highlightCountryMarker(countryName) {
    markersLayer.eachLayer(layer => {
        if (layer.countryName === countryName) {
            layer.setStyle({
                weight: 4,
                color: '#FFD700'
            });
        } else {
            layer.setStyle({
                weight: 1,
                color: '#000'
            });
        }
    });
}

/**
 * Resets all marker highlights to their default style.
 * Removes any special highlighting from country markers.
 */
function resetMarkerHighlight() {
    markersLayer.eachLayer(layer => {
        layer.setStyle({
            weight: 1,
            color: '#000'
        });
    });
}

/**
 * Creates popup content for a country marker using DOM elements.
 * Builds the popup content programmatically to avoid innerHTML security issues.
 * @param {string} country - The country name
 * @param {Object} data - The emission data object for the country
 * @returns {HTMLElement} The DOM element containing the popup content
 */
function createPopupContent(country, data) {
    const container = document.createElement('div');
    
    const title = document.createElement('h6');
    const titleStrong = document.createElement('strong');
    titleStrong.textContent = country;
    title.appendChild(titleStrong);
    
    const yearParagraph = document.createElement('p');
    const yearStrong = document.createElement('strong');
    yearStrong.textContent = 'Jahr: ';
    yearParagraph.appendChild(yearStrong);
    yearParagraph.appendChild(document.createTextNode(data.year));
    
    const emissionsParagraph = document.createElement('p');
    const emissionsStrong = document.createElement('strong');
    emissionsStrong.textContent = 'CO₂ Emissionen: ';
    emissionsParagraph.appendChild(emissionsStrong);
    emissionsParagraph.appendChild(document.createTextNode(`${data.co2EmissionKt.toLocaleString()} kt`));
    
    const button = document.createElement('button');
    button.className = 'bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded text-sm transition-colors';
    button.textContent = '📊 Details anzeigen';
    button.addEventListener('click', () => loadCountryData(country));
    
    container.appendChild(title);
    container.appendChild(yearParagraph);
    container.appendChild(emissionsParagraph);
    container.appendChild(button);
    
    return container;
}

/**
 * Adds circle markers for each country to the map using the latest available emission data.
 * Creates markers with size and color based on emission values and attaches popups.
 */
function addCountryMarkers() {
    console.log('Adding markers for', emissionsData.length, 'emission records');
    
    const latestData = {};
    emissionsData.forEach(emission => {
        const country = emission.countryName;
        if (!latestData[country] || latestData[country].year < emission.year) {
            latestData[country] = emission;
        }
    });
    
    console.log('Latest data for', Object.keys(latestData).length, 'countries');
    
    Object.entries(latestData).forEach(([country, data]) => {
        const coords = countryCoords[country];
        if (coords) {
            const color = getColor(data.co2EmissionKt);
            const radius = Math.max(5, Math.min(30, Math.sqrt(data.co2EmissionKt / 10000)));
            
            const circle = L.circleMarker(coords, {
                radius: radius,
                fillColor: color,
                color: '#000',
                weight: 1,
                opacity: 1,
                fillOpacity: 0.8
            });
            
            circle.countryName = country;
            
            // Popup with DOM elements instead of innerHTML
            const popupContent = createPopupContent(country, data);
            circle.bindPopup(popupContent);
            
            markersLayer.addLayer(circle);
        }
    });
}

/**
 * Reloads all emissions data from the backend API and updates the map markers.
 * Clears existing markers and recreates them with the latest data from the server.
 */
async function reloadEmissionsData() {
    try {
        console.log('Reloading emissions data...');
        const response = await fetch('/api/emissions/all');
        
        if (response.ok) {
            const newData = await response.json();
            console.log('Loaded', newData.length, 'emission records');
            
            // Globale Daten aktualisieren
            emissionsData = newData;
            
            // Marker neu erstellen
            markersLayer.clearLayers();
            addCountryMarkers();
            
            console.log('Map updated with latest data');
        } else {
            console.warn('Could not reload data, using cached data');
        }
    } catch (error) {
        console.warn('Error reloading data:', error);
    }
}

/**
 * Handles the country search input and triggers data loading for the entered country.
 * Reads the search input value and loads country data if a name is provided.
 */
function searchCountry() {
    const countryName = document.getElementById('countrySearch').value.trim();
    if (countryName) {
        loadCountryData(countryName);
    }
}

/**
 * Resets the view by closing the detail panel and clearing the search input.
 * Returns the interface to its initial state with world map view.
 */
function resetView() {
    closeCountryPanel();
    document.getElementById('countrySearch').value = '';
}

/**
 * Refreshes the emissions data on the map without reloading the entire page.
 * Updates markers with the latest data from the backend API.
 */
function refreshData() {
    // Verwende die neue reloadEmissionsData Funktion statt location.reload()
    reloadEmissionsData();
}

/**
 * Downloads the emission data for a specific country as a CSV file.
 * Creates and triggers download of a CSV file containing all emission records for the country.
 * @param {string} countryName - The name of the country to export data for
 */
function downloadCountryData(countryName) {
    const countryEmissions = emissionsData.filter(e => e.countryName === countryName);
    
    if (countryEmissions.length === 0) {
        alert('Keine Daten zum Exportieren verfügbar');
        return;
    }
    
    let csv = 'Jahr,Land,CO2 Emissionen (kt)\n';
    countryEmissions.forEach(emission => {
        csv += `${emission.year},${emission.countryName},${emission.co2EmissionKt}\n`;
    });
    
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `co2_emissions_${countryName.replace(/\s+/g, '_')}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
}

/**
 * Test function to directly show the detail panel with sample data.
 * Development helper function for testing the country panel display.
 */
function testShowPanel() {
    const testData = [
        {year: 2023, co2EmissionKt: 644280},
        {year: 2022, co2EmissionKt: 701300},
        {year: 2021, co2EmissionKt: 659100}
    ];
    
    displayCountryPanel('Deutschland (Test)', testData);
}

/**
 * Initializes the emissions map, loads initial data, and sets up event listeners.
 * Main entry point for the emissions map module that coordinates the setup process.
 * @param {Array<Object>} data - Initial emission data array
 * @param {string|null} selected - Optionally pre-select a country on load
 */
async function initEmissionsMap(data, selected) {
    console.log('Initializing map with', data?.length || 0, 'records');
    
    emissionsData = data || [];
    selectedCountry = selected;

    await loadCountryCoordinates(); 
    
    initMap();
    
    // First, add markers with initial data
    addCountryMarkers();
    
    // Then, load latest data from API
    console.log('Loading latest emissions data including scientist uploads...');
    await reloadEmissionsData();
    
    setupEventListeners(); // Event Listeners für Template
    
    // If a country is already selected, load its data
    if (selectedCountry) {
        console.log('Preselected country:', selectedCountry);
        loadCountryData(selectedCountry);
    }
}