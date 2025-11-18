/**
 * Shopping Cart JavaScript
 * Handles all cart operations via AJAX
 */

// Global variables
let selectedShippingMethod = null;
let selectedShippingCost = 0;
let calculatedTax = 0;

/**
 * Update quantity (kept for backward compatibility, but main logic is in event delegation)
 */
function updateQuantity(cartItemId, newQuantity) {
    if (newQuantity < 1) {
        if (confirm('Remove this item from cart?')) {
            removeItem(cartItemId);
        }
        return;
    }

    updateCartItemQuantity(cartItemId, newQuantity);
}

/**
 * Remove item (called from HTML onclick)
 */
function removeItem(cartItemId) {
    if (!confirm('Are you sure you want to remove this item from your cart?')) {
        return;
    }

    $.ajax({
        url: `/api/cart/remove/${cartItemId}`,
        method: 'DELETE',
        success: function(response) {
            if (response.success) {
                showMessage('Item removed from cart', 'success');
                // Reload page to reflect changes
                setTimeout(() => location.reload(), 800);
            } else {
                showMessage(response.message || 'Failed to remove item', 'error');
            }
        },
        error: function(xhr) {
            const response = xhr.responseJSON;
            showMessage(response?.message || 'An error occurred while removing item', 'error');
        }
    });
}

/**
 * Increase cart item quantity (for backward compatibility)
 */
function increaseQuantity(button) {
    const itemId = $(button).data('item-id');
    const max = parseInt($(button).data('max'));
    const input = $(`input[data-item-id="${itemId}"]`);
    let currentQty = parseInt(input.val());

    if (currentQty < max) {
        currentQty++;
        updateCartItemQuantity(itemId, currentQty);
    } else {
        showMessage('Maximum available stock reached', 'warning');
    }
}

/**
 * Decrease cart item quantity (for backward compatibility)
 */
function decreaseQuantity(button) {
    const itemId = $(button).data('item-id');
    const input = $(`input[data-item-id="${itemId}"]`);
    let currentQty = parseInt(input.val());

    if (currentQty > 1) {
        currentQty--;
        updateCartItemQuantity(itemId, currentQty);
    } else {
        // If quantity is 1, ask to remove item
        if (confirm('Remove this item from cart?')) {
            removeItem(itemId);
        }
    }
}

/**
 * Update cart item quantity via AJAX
 */
function updateCartItemQuantity(itemId, quantity) {
    $.ajax({
        url: '/api/cart/update',
        method: 'PUT',
        contentType: 'application/json',
        data: JSON.stringify({
            cartItemId: itemId,
            quantity: quantity
        }),
        success: function(response) {
            if (response.success) {
                // Update UI
                refreshCartDisplay(response.cart);
                showMessage('Cart updated successfully', 'success');
            } else {
                showMessage(response.message || 'Failed to update cart', 'error');
                // Reload page to sync
                setTimeout(() => location.reload(), 1500);
            }
        },
        error: function(xhr) {
            const response = xhr.responseJSON;
            showMessage(response?.message || 'An error occurred while updating cart', 'error');
            // Reload page to sync
            setTimeout(() => location.reload(), 1500);
        }
    });
}


/**
 * Apply coupon code
 */
function applyCoupon() {
    const couponCode = $('#couponCode').val().trim();

    if (!couponCode) {
        showMessage('Please enter a coupon code', 'warning');
        return;
    }

    $.ajax({
        url: '/api/cart/coupon/apply',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({
            couponCode: couponCode
        }),
        success: function(response) {
            if (response.success) {
                showMessage(`Coupon applied! You saved $${response.cart.discountAmount.toFixed(2)}`, 'success');
                // Update the UI dynamically instead of reloading
                updateCartDisplay(response.cart);
                // Also update the coupon section UI
                $('#couponCode').val(response.cart.couponCode);
                $('button:contains("Apply coupon")').text('Remove coupon').attr('onclick', 'removeCoupon()').removeClass('bg8').addClass('bg10');

            } else {
                showMessage(response.message || 'Invalid coupon code', 'error');
            }
        },
        error: function(xhr) {
            const response = xhr.responseJSON;
            showMessage(response?.message || 'Invalid or expired coupon code', 'error');
        }
    });
}

/**
 * Recalculate tax after coupon is applied
 */
function recalculateTaxAfterCoupon(country, state) {
    $.ajax({
        url: '/api/cart/shipping/calculate',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({
            country: country,
            state: state || '',
            postcode: $('#shippingPostcode').val() || ''
        }),
        success: function(response) {
            if (response.success && response.tax) {
                // Show updated tax in the preview
                const oldTax = calculatedTax;
                calculatedTax = response.tax;
                const taxSavings = oldTax - calculatedTax;

                if (taxSavings > 0) {
                    showMessage(`Tax updated! Additional savings: $${taxSavings.toFixed(2)}`, 'success');
                }

                // Update tax display if visible
                if ($('#taxAmount').length && $('#taxSection').is(':visible')) {
                    $('#taxAmount').text(calculatedTax.toFixed(2));
                }
            }
        }
    });
}

/**
 * Remove coupon
 */
function removeCoupon() {
    $.ajax({
        url: '/api/cart/coupon/remove',
        method: 'DELETE',
        success: function(response) {
            if (response.success) {
                showMessage('Coupon removed', 'success');
                setTimeout(() => location.reload(), 800);
            } else {
                showMessage('Failed to remove coupon', 'error');
            }
        },
        error: function(xhr) {
            showMessage('An error occurred', 'error');
        }
    });
}

/**
 * Calculate shipping rates
 */
function calculateShipping() {
    const country = $('#shippingCountry').val();
    const state = $('#shippingState').val();
    const postcode = $('#shippingPostcode').val();

    if (!country) {
        showMessage('Please select a country', 'warning');
        return;
    }

    if (!postcode) {
        showMessage('Please enter a postcode', 'warning');
        return;
    }

    $.ajax({
        url: '/api/cart/shipping/calculate',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({
            country: country,
            state: state || '',
            postcode: postcode
        }),
        success: function(response) {
            if (response.success) {
                displayShippingRates(response.shippingRates, response.tax);
                calculatedTax = response.tax;
                showMessage('Shipping calculated successfully', 'success');
            } else {
                showMessage(response.message || 'Failed to calculate shipping', 'error');
            }
        },
        error: function(xhr) {
            const response = xhr.responseJSON;
            showMessage(response?.message || 'An error occurred while calculating shipping', 'error');
        }
    });
}

/**
 * Display shipping rates
 */
function displayShippingRates(rates, tax) {
    const ratesContainer = $('#shippingRates');
    const resultsSection = $('#shippingResults');

    if (!rates || rates.length === 0) {
        ratesContainer.html('<p class="stext-111 cl6">No shipping methods available for this location.</p>');
        resultsSection.show();
        return;
    }

    ratesContainer.empty();

    rates.forEach((rate, index) => {
        const rateHtml = `
            <div class="shipping-rate-option" onclick="selectShippingRate(${index}, ${rate.cost}, '${rate.methodName || rate.method}')">
                <div class="flex-w flex-sb-m">
                    <div>
                        <strong class="stext-110 cl2">${rate.methodName || rate.method}</strong>
                        <p class="stext-111 cl6 m-b-0">${rate.estimatedDelivery}</p>
                    </div>
                    <div class="mtext-110 cl2">
                        $${rate.cost.toFixed(2)}
                    </div>
                </div>
            </div>
        `;
        ratesContainer.append(rateHtml);
    });

    // Show shipping results section
    resultsSection.show();

    // Display tax if available
    if (tax && tax > 0) {
        $('#taxAmount').text(tax.toFixed(2));
        $('#taxSection').show();
        calculatedTax = tax;
    }
}

/**
 * Select shipping method (for backward compatibility)
 */
function selectShippingMethod(method, cost) {
    selectedShippingMethod = method;
    selectedShippingCost = cost;
    updateTotalWithShipping();
}

/**
 * Select a shipping rate (new method for updated UI)
 */
function selectShippingRate(index, cost, methodName) {
    // Get shipping address info
    const country = $('#shippingCountry').val();
    const state = $('#shippingState').val();
    const postcode = $('#shippingPostcode').val();

    if (!country || !postcode) {
        showMessage('Please enter shipping address first', 'warning');
        return;
    }

    // Remove previous selection
    $('.shipping-rate-option').removeClass('selected');

    // Mark as selected
    $('.shipping-rate-option').eq(index).addClass('selected');

    // Call backend to save shipping selection
    $.ajax({
        url: '/api/cart/shipping/select',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({
            shippingMethod: methodName,
            shippingCost: cost,
            country: country,
            state: state || '',
            postcode: postcode
        }),
        success: function(response) {
            if (response.success) {
                // Store selected shipping
                selectedShippingMethod = methodName;
                selectedShippingCost = cost;

                // Update display with cart data from backend
                updateCartDisplay(response.cart);

                showMessage(`Selected: ${methodName}`, 'success');
            } else {
                showMessage(response.message || 'Failed to select shipping method', 'error');
            }
        },
        error: function(xhr) {
            const response = xhr.responseJSON;
            showMessage(response?.message || 'An error occurred while selecting shipping method', 'error');
        }
    });
}

/**
 * Update cart display with new cart data from backend
 */
function updateCartDisplay(cart) {
    // Update subtotal
    $('#subtotal').text(cart.subtotal ? cart.subtotal.toFixed(2) : '0.00');

    // Update discount if applied
    if (cart.discountAmount && cart.discountAmount > 0) {
        $('#discountAmount').text(cart.discountAmount.toFixed(2));
        $('#discountSection').css('display', 'flex').show();
    } else {
        $('#discountSection').hide();
    }

    // Update shipping
    if (cart.hasShippingMethod && cart.shippingCost && cart.shippingCost > 0) {
        $('#shippingAmount').text(cart.shippingCost.toFixed(2));
        $('#selectedShippingMethod').text(cart.selectedShippingMethod || 'Shipping');
        $('#shippingSelectedSection').css('display', 'flex').show();
    } else {
        $('#shippingSelectedSection').hide();
    }

    // Update tax
    if (cart.taxAmount && cart.taxAmount > 0) {
        $('#taxAmount').text(cart.taxAmount.toFixed(2));
        $('#taxSection').css('display', 'flex').show();
        calculatedTax = cart.taxAmount;
    } else {
        $('#taxSection').hide();
    }

    // Update final total
    const finalTotal = cart.finalTotal || cart.total || 0;
    $('#finalTotal').text(finalTotal.toFixed(2));

    // Update cart count in header
    if ($('.js-show-cart').length) {
        $('.js-show-cart').attr('data-notify', cart.totalItems || 0);
    }

    // Update checkout button state
    updateCheckoutButton(cart);
}

/**
 * Update checkout button state and text based on cart status
 */
function updateCheckoutButton(cart) {
    const checkoutBtn = $('button:contains("Proceed to Checkout"), button:contains("Cannot Checkout"), button:contains("Select Shipping")').first();

    if (!checkoutBtn.length) {
        return; // Button not found on page
    }

    // Check for out of stock items
    if (cart.hasOutOfStockItems) {
        checkoutBtn.prop('disabled', true);
        checkoutBtn.html('<span>Cannot Checkout - Out of Stock Items</span>');
        return;
    }

    // Check for shipping method
    if (!cart.hasShippingMethod) {
        checkoutBtn.prop('disabled', true);
        checkoutBtn.html('<span>Select Shipping Method First</span>');
        return;
    }

    // All good - enable checkout
    checkoutBtn.prop('disabled', false);
    checkoutBtn.html('<span>Proceed to Checkout</span>');
}

/**
 * Update total amount with shipping and tax (legacy - kept for backward compatibility)
 */
function updateTotalWithShipping() {
    // Try different possible element IDs
    let subtotalText = $('#subtotalAmount').text() || $('#subtotal').text();
    let discountText = $('#discountAmount').text();
    let totalElement = $('#totalAmount').length ? $('#totalAmount') : $('#total');

    const subtotal = parseFloat(subtotalText.replace('$', ''));
    const discount = parseFloat(discountText.replace('-$', '').replace('$', '')) || 0;
    const tax = calculatedTax || 0;
    const shipping = selectedShippingCost || 0;

    const total = subtotal - discount + tax + shipping;
    totalElement.text(total.toFixed(2));
}

/**
 * Refresh cart display after updates (for quantity/remove operations)
 */
function refreshCartDisplay(cart) {
    // Use the new updateCartDisplay function
    updateCartDisplay(cart);

    // Update individual item quantities and totals
    cart.items.forEach(item => {
        // Update quantity input
        const quantityInput = $(`#quantity-${item.id}`);
        if (quantityInput.length) {
            quantityInput.val(item.quantity);
        }

        // Update item total (column-5)
        const itemTotalSpan = $(`#item-total-${item.id}`);
        if (itemTotalSpan.length) {
            itemTotalSpan.text(item.itemTotal.toFixed(2));
        }

        // Update price display (column-3) - in case price changed
        const priceSpan = $(`#price-${item.id}`);
        if (priceSpan.length) {
            priceSpan.text(item.effectivePrice.toFixed(2));
        }
    });

    // Show stock warnings if any
    if (cart.hasOutOfStockItems) {
        let warningsHtml = '<div class="alert alert-warning mt-3"><strong>Stock Updates:</strong><ul class="mb-0 mt-2">';
        cart.stockWarnings.forEach(warning => {
            warningsHtml += `<li>${warning}</li>`;
        });
        warningsHtml += '</ul></div>';
        $('.container').first().prepend(warningsHtml);
    }
}

/**
 * Proceed to checkout with comprehensive validation
 */
function proceedToCheckout() {
    // Use new comprehensive validation endpoint
    $.ajax({
        url: '/api/cart/validate-checkout',
        method: 'POST',
        beforeSend: function() {
            // Disable checkout button to prevent double-click
            $('button:contains("Proceed to Checkout")').prop('disabled', true);
        },
        success: function(response) {
            if (response.success && response.valid) {
                // All validations passed, redirect to checkout
                showMessage('Redirecting to checkout...', 'success');
                setTimeout(() => {
                    window.location.href = '/checkout';
                }, 500);
            } else {
                // Should not reach here as errors are handled in error callback
                showMessage('Checkout validation failed', 'error');
                $('button:contains("Proceed to Checkout")').prop('disabled', false);
            }
        },
        error: function(xhr) {
            const response = xhr.responseJSON;
            $('button:contains("Proceed to Checkout")').prop('disabled', false);

            if (response && response.message) {
                // Show specific validation error
                showMessage(response.message, 'error');

                // If message mentions shipping, scroll to shipping section
                if (response.message.toLowerCase().includes('shipping')) {
                    $('html, body').animate({
                        scrollTop: $('#shippingCountry').offset().top - 100
                    }, 500);
                }
            } else {
                showMessage('An error occurred during checkout validation', 'error');
            }
        }
    });
}

/**
 * Show message to user
 */
function showMessage(message, type) {
    // Remove existing messages
    $('.cart-message').remove();

    // Create message element
    let alertClass = 'alert-info';
    if (type === 'success') alertClass = 'alert-success';
    else if (type === 'error') alertClass = 'alert-danger';
    else if (type === 'warning') alertClass = 'alert-warning';

    const messageHtml = `
        <div class="cart-message alert ${alertClass} alert-dismissible fade show" role="alert" style="position: fixed; top: 80px; right: 20px; z-index: 9999; min-width: 300px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
            ${message}
            <button type="button" class="close" data-dismiss="alert" aria-label="Close">
                <span aria-hidden="true">&times;</span>
            </button>
        </div>
    `;

    $('body').append(messageHtml);

    // Auto-dismiss after 5 seconds
    setTimeout(() => {
        $('.cart-message').fadeOut(400, function() {
            $(this).remove();
        });
    }, 5000);
}

/**
 * Update cart item count in header
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
 * Initialize on page load
 */
$(document).ready(function() {
    // Update cart count on page load
    updateCartCount();

    // Show shipping/tax sections if they have values on page load
    if ($('#shippingAmount').text() !== '0.00' && $('#shippingAmount').text() !== '') {
        $('#shippingSelectedSection').show();
    }
    if ($('#taxAmount').text() !== '0.00' && $('#taxAmount').text() !== '') {
        $('#taxSection').show();
    }
    if ($('#discountAmount').text() !== '0.00' && $('#discountAmount').text() !== '') {
        $('#discountSection').show();
    }

    // Event delegation for quantity update buttons
    $(document).on('click', '.btn-cart-update', function(e) {
        e.preventDefault();
        e.stopPropagation();

        const itemId = $(this).data('item-id');
        const action = $(this).data('action');
        const quantityInput = $(`#quantity-${itemId}`);
        let currentQuantity = parseInt(quantityInput.val());
        let newQuantity;

        if (action === 'increase') {
            const maxStock = parseInt(quantityInput.attr('max'));
            if (maxStock && currentQuantity >= maxStock) {
                showMessage('Maximum available stock reached', 'warning');
                return;
            }
            newQuantity = currentQuantity + 1;
        } else if (action === 'decrease') {
            if (currentQuantity <= 1) {
                if (confirm('Remove this item from cart?')) {
                    removeItem(itemId);
                }
                return;
            }
            newQuantity = currentQuantity - 1;
        }

        // Call update function
        updateCartItemQuantity(itemId, newQuantity);
    });

    // Enable enter key for coupon code
    $('#couponCode').on('keypress', function(e) {
        if (e.which === 13) {
            e.preventDefault();
            applyCoupon();
        }
    });

    // Enable enter key for shipping calculation
    $('#shippingPostcode').on('keypress', function(e) {
        if (e.which === 13) {
            e.preventDefault();
            calculateShipping();
        }
    });

    // Auto-save shipping info to session storage
    $('#shippingCountry, #shippingState, #shippingPostcode').on('change', function() {
        sessionStorage.setItem('shippingCountry', $('#shippingCountry').val());
        sessionStorage.setItem('shippingState', $('#shippingState').val());
        sessionStorage.setItem('shippingPostcode', $('#shippingPostcode').val());
    });

    // Restore shipping info from session storage
    const savedCountry = sessionStorage.getItem('shippingCountry');
    const savedState = sessionStorage.getItem('shippingState');
    const savedPostcode = sessionStorage.getItem('shippingPostcode');

    if (savedCountry) $('#shippingCountry').val(savedCountry).trigger('change');
    if (savedState) $('#shippingState').val(savedState);
    if (savedPostcode) $('#shippingPostcode').val(savedPostcode);
});
