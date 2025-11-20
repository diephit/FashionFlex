/* ============================================
   FashionFlex Admin Common JavaScript
   ============================================ */

// Bulk Selection Handler
document.addEventListener('DOMContentLoaded', function() {

    // Select All Checkbox
    const selectAllCheckbox = document.getElementById('selectAll');
    const itemCheckboxes = document.querySelectorAll('.item-checkbox');
    const bulkActionsBar = document.querySelector('.bulk-actions-bar');
    const selectedCountSpan = document.getElementById('selectedCount');

    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener('change', function() {
            itemCheckboxes.forEach(checkbox => {
                checkbox.checked = this.checked;
            });
            updateBulkActionsBar();
        });
    }

    if (itemCheckboxes.length > 0) {
        itemCheckboxes.forEach(checkbox => {
            checkbox.addEventListener('change', function() {
                updateBulkActionsBar();

                // Update "Select All" checkbox state
                if (selectAllCheckbox) {
                    const allChecked = Array.from(itemCheckboxes).every(cb => cb.checked);
                    const someChecked = Array.from(itemCheckboxes).some(cb => cb.checked);
                    selectAllCheckbox.checked = allChecked;
                    selectAllCheckbox.indeterminate = someChecked && !allChecked;
                }
            });
        });
    }

    function updateBulkActionsBar() {
        const checkedCount = document.querySelectorAll('.item-checkbox:checked').length;

        if (bulkActionsBar) {
            if (checkedCount > 0) {
                bulkActionsBar.classList.add('show');
                if (selectedCountSpan) {
                    selectedCountSpan.textContent = checkedCount;
                }
            } else {
                bulkActionsBar.classList.remove('show');
            }
        }
    }

    // Bulk Action Execution
    const bulkActionButtons = document.querySelectorAll('[data-bulk-action]');
    bulkActionButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();
            const action = this.getAttribute('data-bulk-action');
            const checkedIds = Array.from(document.querySelectorAll('.item-checkbox:checked'))
                .map(cb => cb.value);

            if (checkedIds.length === 0) {
                alert('Please select at least one item');
                return;
            }

            const confirmMessage = this.getAttribute('data-confirm') ||
                `Are you sure you want to ${action} ${checkedIds.length} item(s)?`;

            if (confirm(confirmMessage)) {
                executeBulkAction(action, checkedIds);
            }
        });
    });

    function executeBulkAction(action, ids) {
        // Show loading
        showLoading();

        // Make AJAX request
        fetch(`/admin/bulk/${action}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ ids: ids })
        })
        .then(response => response.json())
        .then(data => {
            hideLoading();
            if (data.success) {
                location.reload();
            } else {
                alert(data.message || 'An error occurred');
            }
        })
        .catch(error => {
            hideLoading();
            alert('An error occurred while processing your request');
            console.error('Error:', error);
        });
    }

    // Auto-dismiss alerts after 5 seconds
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(alert => {
        setTimeout(() => {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });

    // Confirm delete actions
    const deleteButtons = document.querySelectorAll('[data-confirm-delete]');
    deleteButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            const message = this.getAttribute('data-confirm-delete') ||
                'Are you sure you want to delete this item? This action cannot be undone.';
            if (!confirm(message)) {
                e.preventDefault();
                return false;
            }
        });
    });

    // Form validation enhancement
    const forms = document.querySelectorAll('.needs-validation');
    forms.forEach(form => {
        form.addEventListener('submit', function(event) {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            form.classList.add('was-validated');
        }, false);
    });

    // Loading spinner functions
    function showLoading() {
        let spinner = document.querySelector('.spinner-overlay');
        if (!spinner) {
            spinner = document.createElement('div');
            spinner.className = 'spinner-overlay';
            spinner.innerHTML = '<div class="spinner-border text-light" role="status"><span class="visually-hidden">Loading...</span></div>';
            document.body.appendChild(spinner);
        }
        spinner.classList.add('show');
    }

    function hideLoading() {
        const spinner = document.querySelector('.spinner-overlay');
        if (spinner) {
            spinner.classList.remove('show');
        }
    }

    // Export functions for global use
    window.adminCommon = {
        showLoading: showLoading,
        hideLoading: hideLoading
    };
});

// Filter form auto-submit on change
function setupAutoSubmitFilters() {
    const filterSelects = document.querySelectorAll('.filter-auto-submit');
    filterSelects.forEach(select => {
        select.addEventListener('change', function() {
            this.closest('form').submit();
        });
    });
}

// Clear filters
function clearFilters() {
    const form = document.querySelector('.filter-form');
    if (form) {
        form.querySelectorAll('input[type="text"], input[type="search"], select').forEach(input => {
            input.value = '';
        });
        form.submit();
    }
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    setupAutoSubmitFilters();
});
