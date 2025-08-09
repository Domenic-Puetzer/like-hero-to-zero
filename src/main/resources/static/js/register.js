/**
 * Register Page
 * Handles registration form validation and UI interactions for the Like Hero To Zero registration page.
 * Provides password match validation and chevron icon rotation for the role dropdown.
 */

document.addEventListener('DOMContentLoaded', function() {
    // References to password fields and form
    const password = document.getElementById('password');
    const confirmPassword = document.getElementById('confirmPassword');
    const form = document.querySelector('form');

    // Reference to role select and its parent container
    const roleSelect = document.getElementById('role');
    const roleSelectParent = roleSelect.parentElement;

    /**
     * Validates that password and confirm password fields match.
     * Sets custom validity message if they do not match.
     */
    function validatePassword() {
        if (password.value !== confirmPassword.value) {
            confirmPassword.setCustomValidity('Passwörter stimmen nicht überein');
        } else {
            confirmPassword.setCustomValidity('');
        }
    }

    /**
     * Rotates the chevron icon when the role select is focused.
     * Adds the Tailwind rotate-180 class to the SVG.
     */
    function rotateChevron() {
        const chevron = roleSelectParent.querySelector('svg');
        if (chevron) {
            chevron.classList.add('rotate-180');
        }
    }

    /**
     * Resets the chevron icon rotation when the role select loses focus or changes.
     * Removes the Tailwind rotate-180 class from the SVG.
     */
    function resetChevron() {
        const chevron = roleSelectParent.querySelector('svg');
        if (chevron) {
            chevron.classList.remove('rotate-180');
        }
    }

    /**
     * Handles changes to the role select.
     * Resets the chevron and removes focus from the select.
     */
    function handleRoleChange() {
        resetChevron();
        roleSelect.blur();
    }

    // Password validation event listeners
    password.addEventListener('change', validatePassword);
    confirmPassword.addEventListener('keyup', validatePassword);

    /**
     * Prevents form submission if passwords do not match.
     * Shows an alert to the user.
     */
    form.addEventListener('submit', function(e) {
        if (password.value !== confirmPassword.value) {
            e.preventDefault();
            alert('Passwörter stimmen nicht überein!');
        }
    });

    // Chevron rotation event listeners for role select
    roleSelect.addEventListener('focus', rotateChevron);
    roleSelect.addEventListener('blur', resetChevron);
    roleSelect.addEventListener('change', handleRoleChange);
});