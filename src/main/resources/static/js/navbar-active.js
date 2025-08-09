/**
 * Highlights the active navigation link in the navbar based on the current URL path.
 * Removes the active class from all navigation links and adds it to the link
 * that matches the current page.
 *
 * Runs after the DOM content is fully loaded.
 */
document.addEventListener('DOMContentLoaded', function() {
    const currentPath = window.location.pathname;
    console.log('Current path:', currentPath); // Debug log

    // Remove active classes from all navigation links
    const navLinks = document.querySelectorAll('#nav-home, #nav-emissions, #nav-dashboard');
    console.log('Found nav links:', navLinks.length); // Debug log

    navLinks.forEach(link => {
        link.classList.remove('border-b-2', 'border-yellow-300');
    });

    // Add active class based on current URL
    if (currentPath === '/') {
        const homeLink = document.getElementById('nav-home');
        if (homeLink) {
            homeLink.classList.add('border-b-2', 'border-yellow-300');
            console.log('Home link activated');
        }
    } else if (currentPath === '/emissions') {
        const emissionsLink = document.getElementById('nav-emissions');
        if (emissionsLink) {
            emissionsLink.classList.add('border-b-2', 'border-yellow-300');
            console.log('Emissions link activated');
        }
    } else if (currentPath.startsWith('/scientist')) {
        const dashboardLink = document.getElementById('nav-dashboard');
        if (dashboardLink) {
            dashboardLink.classList.add('border-b-2', 'border-yellow-300');
            console.log('Dashboard link activated');
        }
    }
});