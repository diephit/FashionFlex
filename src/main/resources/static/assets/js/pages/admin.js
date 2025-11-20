/**
 * Admin Dashboard JavaScript
 * Handles admin-specific functionality
 */

(function($) {
    'use strict';

    // Sidebar Toggle for Mobile
    $('.admin-mobile-toggle').on('click', function() {
        $('.admin-sidebar').toggleClass('active');
    });

    // Close sidebar when clicking outside on mobile
    $(document).on('click', function(e) {
        if ($(window).width() <= 768) {
            if (!$(e.target).closest('.admin-sidebar, .admin-mobile-toggle').length) {
                $('.admin-sidebar').removeClass('active');
            }
        }
    });

    // Active menu item
    function setActiveMenuItem() {
        var currentPath = window.location.pathname;
        var fileName = currentPath.substring(currentPath.lastIndexOf('/') + 1);

        $('.admin-sidebar-menu li a').each(function() {
            var href = $(this).attr('href');
            if (href && href.indexOf(fileName) !== -1) {
                $(this).addClass('active');
            }
        });
    }

    // Initialize on page load
    setActiveMenuItem();

    // Data Table Sorting (Simple implementation)
    $('.admin-table th').on('click', function() {
        var table = $(this).parents('table').eq(0);
        var rows = table.find('tbody tr').toArray().sort(comparer($(this).index()));
        this.asc = !this.asc;
        if (!this.asc) { rows = rows.reverse(); }
        for (var i = 0; i < rows.length; i++) { table.append(rows[i]); }
    });

    function comparer(index) {
        return function(a, b) {
            var valA = getCellValue(a, index);
            var valB = getCellValue(b, index);
            return $.isNumeric(valA) && $.isNumeric(valB) ?
                valA - valB : valA.toString().localeCompare(valB);
        };
    }

    function getCellValue(row, index) {
        return $(row).children('td').eq(index).text();
    }

    // Delete Confirmation
    $('.action-btn.delete').on('click', function(e) {
        e.preventDefault();
        var itemName = $(this).data('item') || 'this item';

        swal({
            title: "Are you sure?",
            text: "Do you want to delete " + itemName + "? This action cannot be undone!",
            icon: "warning",
            buttons: {
                cancel: {
                    text: "Cancel",
                    value: null,
                    visible: true,
                    className: "",
                    closeModal: true,
                },
                confirm: {
                    text: "Yes, delete it!",
                    value: true,
                    visible: true,
                    className: "bg-danger",
                    closeModal: true
                }
            },
            dangerMode: true,
        }).then((willDelete) => {
            if (willDelete) {
                // Here you would make an AJAX call to delete the item
                swal("Deleted!", itemName + " has been deleted.", "success");
                // Remove the row from table
                $(this).closest('tr').fadeOut(300, function() {
                    $(this).remove();
                });
            }
        });
    });

    // Search Filter for Tables
    $('#tableSearch').on('keyup', function() {
        var value = $(this).val().toLowerCase();
        $('.admin-table tbody tr').filter(function() {
            $(this).toggle($(this).text().toLowerCase().indexOf(value) > -1);
        });
    });

    // Chart Initialization (if using charts)
    if (typeof Chart !== 'undefined') {
        // Revenue Chart
        var revenueCtx = document.getElementById('revenueChart');
        if (revenueCtx) {
            new Chart(revenueCtx, {
                type: 'line',
                data: {
                    labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
                    datasets: [{
                        label: 'Revenue',
                        data: [12000, 19000, 15000, 25000, 22000, 30000],
                        borderColor: '#667eea',
                        backgroundColor: 'rgba(102, 126, 234, 0.1)',
                        tension: 0.4
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false
                }
            });
        }

        // Orders Chart
        var ordersCtx = document.getElementById('ordersChart');
        if (ordersCtx) {
            new Chart(ordersCtx, {
                type: 'bar',
                data: {
                    labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
                    datasets: [{
                        label: 'Orders',
                        data: [12, 19, 15, 25, 22, 30, 28],
                        backgroundColor: '#764ba2'
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false
                }
            });
        }
    }

    // Image Preview for Product Upload
    $('#productImage').on('change', function() {
        var reader = new FileReader();
        reader.onload = function(e) {
            $('#imagePreview').attr('src', e.target.result).show();
        };
        reader.readAsDataURL(this.files[0]);
    });

    // Bulk Actions
    $('#selectAll').on('change', function() {
        $('.item-checkbox').prop('checked', $(this).prop('checked'));
    });

    $('.item-checkbox').on('change', function() {
        if ($('.item-checkbox:checked').length === $('.item-checkbox').length) {
            $('#selectAll').prop('checked', true);
        } else {
            $('#selectAll').prop('checked', false);
        }
    });

    // Status Update
    $('.status-select').on('change', function() {
        var newStatus = $(this).val();
        var orderId = $(this).data('order-id');

        // Here you would make an AJAX call to update the status
        swal("Updated!", "Order status has been updated to " + newStatus, "success");
    });

})(jQuery);
