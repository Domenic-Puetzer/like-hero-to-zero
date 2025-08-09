/**
 * Emission Upload Page
 * Handles form validation, country code autofill, and user feedback for the emission upload page.
 */

/**
 * Converts the country code input to uppercase as the user types.
 */
document.getElementById('countryCode').addEventListener('input', function(e) {
    e.target.value = e.target.value.toUpperCase();
});

/**
 * Loads country ISO codes from a JSON file and maps them to country names.
 * The resulting map is used for autofilling the ISO code when a country is selected.
 */
let countryIsoMap = {};
fetch('../../json/country-coords.json')
    .then(response => response.json())
    .then(data => {
        countryIsoMap = {};
        for (const [country, info] of Object.entries(data.countries)) {
            countryIsoMap[country] = info.iso_a3;
        }
    });

/**
 * Autofills the ISO code input when a country is selected from the country name input.
 */
document.getElementById('countryName').addEventListener('input', function(e) {
    const selectedCountry = e.target.value;
    const code = countryIsoMap[selectedCountry] || '';
    document.getElementById('countryCode').value = code;
});

/**
 * Sets the current year as default in the year input if not already set.
 */
document.addEventListener('DOMContentLoaded', function() {
    const yearInput = document.getElementById('year');
    if (!yearInput.value) {
        yearInput.value = new Date().getFullYear();
    }

    let countryIsoMap = {};
});

/**
 * Validates the emission upload form before submission.
 * Checks for negative CO₂ values, valid year range, and non-empty country name.
 * Shows an alert and prevents submission if validation fails.
 * Shows a loading state on the submit button during form submission.
 */
document.querySelector('form').addEventListener('submit', function(e) {
    const co2Value = document.getElementById('co2EmissionKt').value;
    const year = document.getElementById('year').value;
    const country = document.getElementById('countryName').value;

    if (co2Value < 0) {
        e.preventDefault();
        alert('⚠️ CO₂-Werte können nicht negativ sein!');
        return;
    }

    if (year < 1900 || year > 2025) {
        e.preventDefault();
        alert('⚠️ Jahr muss zwischen 1900 und 2025 liegen!');
        return;
    }

    if (!country.trim()) {
        e.preventDefault();
        alert('⚠️ Bitte geben Sie einen Ländernamen ein!');
        return;
    }

    // Success feedback for submit button
    const submitBtn = document.querySelector('button[type="submit"]');
    submitBtn.innerHTML = '⏳ Speichere...';
    submitBtn.disabled = true;
});

/**
 * Automatically hides success and error messages after 5 seconds.
 */
setTimeout(function() {
    const alerts = document.querySelectorAll('.bg-green-100, .bg-red-100');
    alerts.forEach(alert => {
        if (alert) {
            alert.style.transition = 'opacity 0.5s';
            alert.style.opacity = '0';
            setTimeout(() => alert.remove(), 500);
        }
    });
}, 5000);