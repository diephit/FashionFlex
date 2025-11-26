/**
 * Wishlist JavaScript
 * Handles all wishlist operations via AJAX
 */

/**
 * Toggle wishlist (add/remove)
 */
function toggleWishlist(productId, element) {
    // Check if user is logged in
    const isAuthenticated = $('body').data('authenticated');
    if (!isAuthenticated) {
        // Redirect to login
        window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname);
        return;
    }

    const $heartIcon = $(element).find('i');
    const isInWishlist = $heartIcon.hasClass('text-danger');

    if (isInWishlist) {
        // Remove from wishlist
        removeFromWishlistByProductId(productId, element);
    } else {
        // Add to wishlist
        addToWishlist(productId, element);
    }
}

/**
 * Add product to wishlist
 */
function addToWishlist(productId, element) {
    $.ajax({
        url: '/user/api/wishlist/add',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({
            productId: productId
        }),
        success: function(response) {
            if (response.success) {
                // Update heart icon
                if (element) {
                    const $heartIcon = $(element).find('i');
                    $heartIcon.removeClass('text-muted').addClass('text-danger');
                    $(element).attr('title', 'Remove from wishlist');
                }

                // Update wishlist count
                updateWishlistCount(response.count);

                showMessage(response.message || 'Added to wishlist', 'success');
            } else {
                showMessage(response.message || 'Failed to add to wishlist', 'error');
            }
        },
        error: function(xhr) {
            const response = xhr.responseJSON;
            if (xhr.status === 401) {
                // Redirect to login if not authenticated
                window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname);
            } else {
                showMessage(response?.message || 'An error occurred while adding to wishlist', 'error');
            }
        }
    });
}

/**
 * Remove from wishlist by product ID
 */
function removeFromWishlistByProductId(productId, element) {
    $.ajax({
        url: `/user/api/wishlist/remove-by-product/${productId}`,
        method: 'DELETE',
        success: function(response) {
            if (response.success) {
                // Update heart icon
                if (element) {
                    const $heartIcon = $(element).find('i');
                    $heartIcon.removeClass('text-danger').addClass('text-muted');
                    $(element).attr('title', 'Add to wishlist');
                }

                // Update wishlist count
                updateWishlistCount(response.count);

                showMessage(response.message || 'Removed from wishlist', 'success');
            } else {
                showMessage(response.message || 'Failed to remove from wishlist', 'error');
            }
        },
        error: function(xhr) {
            const response = xhr.responseJSON;
            showMessage(response?.message || 'An error occurred while removing from wishlist', 'error');
        }
    });
}

/**
 * Remove from wishlist by wishlist ID (for wishlist page)
 */
function removeFromWishlist(wishlistId) {
    if (!confirm('Remove this item from your wishlist?')) {
        return;
    }

    $.ajax({
        url: `/user/api/wishlist/remove/${wishlistId}`,
        method: 'DELETE',
        success: function(response) {
            if (response.success) {
                showMessage(response.message || 'Removed from wishlist', 'success');

                // Remove item from DOM
                $(`#wishlist-item-${wishlistId}`).fadeOut(400, function() {
                    $(this).remove();

                    // Check if wishlist is empty
                    if ($('.wishlist-item').length === 0) {
                        location.reload();
                    }
                });

                // Update wishlist count
                updateWishlistCount(response.count);
            } else {
                showMessage(response.message || 'Failed to remove from wishlist', 'error');
            }
        },
        error: function(xhr) {
            const response = xhr.responseJSON;
            showMessage(response?.message || 'An error occurred while removing from wishlist', 'error');
        }
    });
}

/**
 * Prepare move to cart (check if variants needed)
 */
function prepareMoveToCart(wishlistId, hasVariants, availableSizes, availableColors, productName) {
    if (!hasVariants || ((!availableSizes || availableSizes.length === 0) && (!availableColors || availableColors.length === 0))) {
        // No variants, move directly
        moveToCart(wishlistId, 1, null, null);
    } else {
        // Show modal to select variants
        showVariantModal(wishlistId, availableSizes, availableColors, productName);
    }
}

/**
 * Show variant selection modal
 */
function showVariantModal(wishlistId, availableSizes, availableColors, productName) {
    // Build size options
    let sizeOptions = '';
    if (availableSizes && availableSizes.length > 0) {
        sizeOptions = '<div class="form-group"><label for="variantSize">Size *</label><select class="form-control" id="variantSize" required>';
        sizeOptions += '<option value="">Select Size</option>';
        availableSizes.forEach(size => {
            sizeOptions += `<option value="${size}">${size}</option>`;
        });
        sizeOptions += '</select></div>';
    }

    // Build color options
    let colorOptions = '';
    if (availableColors && availableColors.length > 0) {
        colorOptions = '<div class="form-group"><label for="variantColor">Color *</label><select class="form-control" id="variantColor" required>';
        colorOptions += '<option value="">Select Color</option>';
        availableColors.forEach(color => {
            colorOptions += `<option value="${color}">${color}</option>`;
        });
        colorOptions += '</select></div>';
    }

    // Build quantity input
    const quantityInput = '<div class="form-group"><label for="variantQuantity">Quantity</label><input type="number" class="form-control" id="variantQuantity" value="1" min="1" required></div>';

    // Create modal HTML
    const modalHtml = `
        <div class="modal fade" id="variantModal" tabindex="-1" role="dialog" aria-labelledby="variantModalLabel" aria-hidden="true">
            <div class="modal-dialog" role="document">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="variantModalLabel">Select Options - ${productName}</h5>
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                            <span aria-hidden="true">&times;</span>
                        </button>
                    </div>
                    <div class="modal-body">
                        <form id="variantForm">
                            ${sizeOptions}
                            ${colorOptions}
                            ${quantityInput}
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-dismiss="modal">Cancel</button>
                        <button type="button" class="btn btn-primary" onclick="submitMoveToCart(${wishlistId})">Add to Cart</button>
                    </div>
                </div>
            </div>
        </div>
    `;

    // Remove existing modal if any
    $('#variantModal').remove();

    // Append and show modal
    $('body').append(modalHtml);
    $('#variantModal').modal('show');

    // Clean up on close
    $('#variantModal').on('hidden.bs.modal', function() {
        $(this).remove();
    });
}

/**
 * Submit move to cart from modal
 */
function submitMoveToCart(wishlistId) {
    const size = $('#variantSize').val();
    const color = $('#variantColor').val();
    const quantity = parseInt($('#variantQuantity').val()) || 1;

    // Validate required fields
    if ($('#variantSize').length && !size) {
        showMessage('Please select a size', 'warning');
        return;
    }

    if ($('#variantColor').length && !color) {
        showMessage('Please select a color', 'warning');
        return;
    }

    // Close modal
    $('#variantModal').modal('hide');

    // Move to cart
    moveToCart(wishlistId, quantity, size, color);
}

/**
 * Move wishlist item to cart
 */
function moveToCart(wishlistId, quantity, size, color) {
    $.ajax({
        url: '/user/api/wishlist/move-to-cart',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({
            wishlistId: wishlistId,
            quantity: quantity || 1,
            size: size,
            color: color
        }),
        success: function(response) {
            if (response.success) {
                showMessage(response.message || 'Product moved to cart successfully', 'success');

                // Remove item from wishlist page
                $(`#wishlist-item-${wishlistId}`).fadeOut(400, function() {
                    $(this).remove();

                    // Check if wishlist is empty
                    if ($('.wishlist-item').length === 0) {
                        location.reload();
                    }
                });

                // Update counts
                updateWishlistCount(response.wishlistCount);
                updateCartCount();
            } else {
                showMessage(response.message || 'Failed to move to cart', 'error');
            }
        },
        error: function(xhr) {
            const response = xhr.responseJSON;
            showMessage(response?.message || 'An error occurred while moving to cart', 'error');
        }
    });
}

/**
 * Clear entire wishlist
 */
function clearWishlist() {
    if (!confirm('Are you sure you want to clear your entire wishlist?')) {
        return;
    }

    $.ajax({
        url: '/user/api/wishlist/clear',
        method: 'DELETE',
        success: function(response) {
            if (response.success) {
                showMessage(response.message || 'Wishlist cleared', 'success');
                setTimeout(() => location.reload(), 800);
            } else {
                showMessage(response.message || 'Failed to clear wishlist', 'error');
            }
        },
        error: function(xhr) {
            const response = xhr.responseJSON;
            showMessage(response?.message || 'An error occurred while clearing wishlist', 'error');
        }
    });
}

/**
 * Update wishlist count in header
 */
function updateWishlistCount(count) {
    if (count !== undefined) {
        $('.wishlist-count').text(count);
        $('.wishlist-badge').text(count);

        // Update data attribute
        $('.wishlist-icon').attr('data-notify', count);

        // Hide badge if count is 0
        if (count === 0) {
            $('.wishlist-badge').hide();
        } else {
            $('.wishlist-badge').show();
        }
    } else {
        // Fetch from server
        $.ajax({
            url: '/user/api/wishlist/count',
            method: 'GET',
            success: function(response) {
                if (response.success) {
                    updateWishlistCount(response.count);
                }
            }
        });
    }
}

/**
 * Update cart count in header
 */
function updateCartCount() {
    $.ajax({
        url: '/api/cart/count',
        method: 'GET',
        success: function(response) {
            if (response.success && $('.js-show-cart').length) {
                $('.js-show-cart').attr('data-notify', response.count);
            }
        }
    });
}

/**
 * Check if product is in wishlist
 */
function checkWishlistStatus(productId, element) {
    $.ajax({
        url: `/user/api/wishlist/check/${productId}`,
        method: 'GET',
        success: function(response) {
            if (response.success && response.inWishlist) {
                const $heartIcon = $(element).find('i');
                $heartIcon.removeClass('text-muted').addClass('text-danger');
                $(element).attr('title', 'Remove from wishlist');
            }
        }
    });
}

/**
 * Show message notification
 */
function showMessage(message, type) {
    // Remove existing messages
    $('.wishlist-message').remove();

    // Create message element
    let alertClass = 'alert-info';
    if (type === 'success') alertClass = 'alert-success';
    else if (type === 'error') alertClass = 'alert-danger';
    else if (type === 'warning') alertClass = 'alert-warning';

    const messageHtml = `
        <div class="wishlist-message alert ${alertClass} alert-dismissible fade show" role="alert"
             style="position: fixed; top: 80px; right: 20px; z-index: 9999; min-width: 300px;
                    box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
            ${message}
            <button type="button" class="close" data-dismiss="alert" aria-label="Close">
                <span aria-hidden="true">&times;</span>
            </button>
        </div>
    `;

    $('body').append(messageHtml);

    // Auto-dismiss after 5 seconds
    setTimeout(() => {
        $('.wishlist-message').fadeOut(400, function() {
            $(this).remove();
        });
    }, 5000);
}

/**
 * Initialize on page load
 */
$(document).ready(function() {
    // Update wishlist count on page load
    updateWishlistCount();

    // Check wishlist status for all heart buttons on the page
    $('.wishlist-btn').each(function() {
        const productId = $(this).data('product-id');
        if (productId) {
            checkWishlistStatus(productId, this);
        }
    });

    // Handle move to cart button click
    $(document).on('click', '.move-to-cart-btn', function() {
        if ($(this).prop('disabled')) {
            return;
        }

        const wishlistId = $(this).data('wishlist-id');
        const hasVariants = $(this).data('has-variants');
        const sizesStr = $(this).data('sizes');
        const colorsStr = $(this).data('colors');
        const productName = $(this).data('product-name');

        // Convert comma-separated strings to arrays
        const availableSizes = sizesStr ? sizesStr.split(',').filter(s => s) : [];
        const availableColors = colorsStr ? colorsStr.split(',').filter(c => c) : [];

        prepareMoveToCart(wishlistId, hasVariants, availableSizes, availableColors, productName);
    });
});
