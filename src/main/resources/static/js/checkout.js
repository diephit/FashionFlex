/**
 * Checkout page JavaScript
 * Handles shipping calculation and tax calculation on checkout page
 */

// Global variables
let selectedShippingData = null;
let cartData = {
    subtotal: 0,
    discountAmount: 0,
    finalTotal: 0
};

// Initialize on page load
$(document).ready(function() {
    // Initialize cart data from page
    initializeCartData();

    // Pre-fill shipping information if available from address selection
    setupAddressChangeHandler();
});

/**
 * Initialize cart data from the page
 */
function initializeCartData() {
    try {
        // Get subtotal from the order summary
        const subtotalText = $('#checkoutTotalAmount').text().replace('$', '');
        cartData.subtotal = parseFloat(subtotalText) || 0;

        // Get discount if displayed
        const discountElement = $('.order-summary-row').find('span:contains("Discount")').next();
        if (discountElement.length > 0) {
            const discountText = discountElement.text().replace('-$', '').trim();
            cartData.discountAmount = parseFloat(discountText) || 0;
        }

        console.log('Cart data initialized:', cartData);
    } catch (error) {
        console.error('Error initializing cart data:', error);
    }
}

/**
 * Setup address change handler to auto-calculate shipping
 */
function setupAddressChangeHandler() {
    // Auto-calculate shipping when saved address is selected
    $('#savedAddressId').on('change', function() {
        const addressId = $(this).val();
        if (addressId) {
            // Get the selected option's data attributes
            const selectedOption = $(this).find('option:selected');
            const country = selectedOption.data('country') || '';
            const state = selectedOption.data('state') || '';
            const postcode = selectedOption.data('zipcode') || '';

            // Auto-fill shipping calculator and calculate
            if (country && state && postcode) {
                $('#checkoutShippingCountry').val(country);
                $('#checkoutShippingState').val(state);
                $('#checkoutShippingPostcode').val(postcode);

                // Auto-calculate shipping after a short delay
                setTimeout(() => {
                    calculateCheckoutShipping();
                }, 500);
            }
        }
    });
}

/**
 * Calculate shipping rates for checkout
 */
function calculateCheckoutShipping() {
    const country = $('#checkoutShippingCountry').val();
    const state = $('#checkoutShippingState').val();
    const postcode = $('#checkoutShippingPostcode').val();

    // Validate inputs
    if (!country) {
        alert('Please select a country');
        return;
    }

    if (!state) {
        alert('Please enter your state/province');
        return;
    }

    if (!postcode) {
        alert('Please enter your postcode/zip code');
        return;
    }

    // Show loading state
    const calculateBtn = $('button[onclick="calculateCheckoutShipping()"]');
    const originalText = calculateBtn.html();
    calculateBtn.html('<i class="fa fa-spinner fa-spin"></i> Calculating...').prop('disabled', true);

    // Make API call to calculate shipping
    const requestData = {
        country: country,
        state: state,
        postcode: postcode
    };

    console.log('=== SHIPPING CALCULATION DEBUG ===');
    console.log('Request URL:', '/api/checkout/shipping/calculate');
    console.log('Request Data:', requestData);
    console.log('Request Headers:', {
        'Content-Type': 'application/json',
        'X-Requested-With': 'XMLHttpRequest'
    });

    $.ajax({
        url: '/api/checkout/shipping/calculate',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(requestData),
        beforeSend: function(xhr) {
            xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
            console.log('Sending AJAX request...');
        },
        success: function(response) {
            console.log('=== SHIPPING CALCULATION SUCCESS ===');
            console.log('Response:', response);

            if (response.success) {
                console.log('Shipping rates:', response.shippingRates);
                console.log('Tax:', response.tax);
                displayShippingRates(response.shippingRates, response.tax, country, state, postcode);
            } else {
                console.error('API returned error:', response.message);
                alert('Error calculating shipping: ' + response.message);
            }
        },
        error: function(xhr, status, error) {
            console.error('=== SHIPPING CALCULATION ERROR ===');
            console.error('Status:', status);
            console.error('Error:', error);
            console.error('Response Text:', xhr.responseText);
            console.error('Response Code:', xhr.status);
            console.error('Response Headers:', xhr.getAllResponseHeaders());

            let errorMessage = 'Failed to calculate shipping rates.';
            if (xhr.responseJSON && xhr.responseJSON.message) {
                errorMessage = xhr.responseJSON.message;
            } else if (xhr.status === 401) {
                errorMessage = 'You are not authenticated. Please login again.';
            } else if (xhr.status === 403) {
                errorMessage = 'You do not have permission to access this resource.';
            } else if (xhr.status === 400) {
                errorMessage = 'Invalid request. Please check your input.';
            }

            alert(errorMessage + ' Please try again.');
        },
        complete: function() {
            // Restore button
            calculateBtn.html(originalText).prop('disabled', false);
        }
    });
}

/**
 * Display shipping rates and tax
 */
function displayShippingRates(shippingRates, tax, country, state, postcode) {
    const ratesContainer = $('#checkoutShippingRates');
    const resultsContainer = $('#checkoutShippingResults');

    // Clear previous results
    ratesContainer.empty();

    // Generate shipping options
    shippingRates.forEach(function(rate) {
        const rateHtml = `
            <div class="shipping-option" style="margin-bottom: 10px; padding: 10px; border: 1px solid #ddd; border-radius: 4px; cursor: pointer; transition: all 0.3s;"
                 onclick="selectShippingMethod('${rate.method}', ${rate.cost}, '${country}', '${state}', '${postcode}', ${tax})">
                <div class="form-check">
                    <input class="form-check-input" type="radio" name="shippingMethod" value="${rate.method}" id="shipping_${rate.method.replace(/\s+/g, '_')}">
                    <label class="form-check-label" for="shipping_${rate.method.replace(/\s+/g, '_')}" style="cursor: pointer;">
                        <strong>${rate.method}</strong> - $${rate.cost.toFixed(2)}
                        <br>
                        <small style="color: #666;">${rate.description}</small>
                    </label>
                </div>
            </div>
        `;
        ratesContainer.append(rateHtml);
    });

    // Show results container
    resultsContainer.show();

    // Store calculated tax
    selectedShippingData = {
        tax: tax,
        country: country,
        state: state,
        postcode: postcode
    };

    console.log('Shipping rates calculated:', { shippingRates, tax });
}

/**
 * Select shipping method
 */
function selectShippingMethod(method, cost, country, state, postcode, tax) {
    // Update selected radio button
    $(`input[name="shippingMethod"][value="${method}"]`).prop('checked', true);

    // Highlight selected option
    $('.shipping-option').removeClass('selected').css('border-color', '#ddd');
    $(`input[name="shippingMethod"][value="${method}"]`).closest('.shipping-option')
        .addClass('selected').css('border-color', '#007bff');

    // Make API call to save shipping selection
    const requestData = {
        shippingMethod: method,
        shippingCost: cost,
        country: country,
        state: state,
        postcode: postcode
    };

    console.log('=== SHIPPING SELECTION DEBUG ===');
    console.log('Request URL:', '/api/checkout/shipping/select');
    console.log('Request Data:', requestData);

    $.ajax({
        url: '/api/checkout/shipping/select',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(requestData),
        beforeSend: function(xhr) {
            xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
        },
        success: function(response) {
            console.log('=== SHIPPING SELECTION SUCCESS ===');
            console.log('Response:', response);

            if (response.success) {
                console.log('Updated cart:', response.cart);
                updateOrderSummary(response.cart);
                selectedShippingData = {
                    ...selectedShippingData,
                    method: method,
                    cost: cost
                };
                console.log('Shipping method selected successfully:', method);
            } else {
                console.error('API returned error:', response.message);
                alert('Error selecting shipping method: ' + response.message);
            }
        },
        error: function(xhr, status, error) {
            console.error('=== SHIPPING SELECTION ERROR ===');
            console.error('Status:', status);
            console.error('Error:', error);
            console.error('Response Text:', xhr.responseText);
            console.error('Response Code:', xhr.status);

            let errorMessage = 'Failed to select shipping method.';
            if (xhr.responseJSON && xhr.responseJSON.message) {
                errorMessage = xhr.responseJSON.message;
            } else if (xhr.status === 401) {
                errorMessage = 'You are not authenticated. Please login again.';
            } else if (xhr.status === 403) {
                errorMessage = 'You do not have permission to access this resource.';
            }

            alert(errorMessage + ' Please try again.');
        }
    });
}

/**
 * Update order summary with shipping and tax
 */
function updateOrderSummary(cart) {
    // Update shipping row
    if (cart.selectedShippingMethod && cart.shippingCost) {
        $('#checkoutShippingMethodName').text(`(${cart.selectedShippingMethod})`);
        $('#checkoutShippingAmount').text(`$${cart.shippingCost.toFixed(2)}`);
        $('#checkoutShippingRow').show();
    } else {
        $('#checkoutShippingRow').hide();
    }

    // Update tax row
    if (cart.taxAmount && cart.taxAmount > 0) {
        $('#checkoutTaxAmount').text(`$${cart.taxAmount.toFixed(2)}`);
        $('#checkoutTaxRow').show();
    } else {
        $('#checkoutTaxRow').hide();
    }

    // Update total
    const totalAmount = cart.finalTotal || cart.total || cart.subtotal;
    $('#checkoutTotalAmount').text(`$${totalAmount.toFixed(2)}`);

    console.log('Order summary updated:', cart);
}

/**
 * Validate checkout before form submission
 */
function validateCheckout() {
    // Check if payment method is selected
    const paymentMethod = $('input[name="paymentMethod"]:checked');
    if (paymentMethod.length === 0) {
        alert('Please select a payment method.');
        return false;
    }

    return true;
}

/**
 * Form submission handler
 */
$('#checkoutForm').on('submit', function(e) {
    // Add shipping information to form if selected
    if (selectedShippingData) {
        // You can add hidden fields if needed
        if (!$('#selectedShippingMethod').length) {
            $('<input>').attr({
                type: 'hidden',
                id: 'selectedShippingMethod',
                name: 'selectedShippingMethod'
            }).appendTo('#checkoutForm');
        }
        $('#selectedShippingMethod').val(selectedShippingData.method);
    }

    // Additional validation
    if (!validateCheckout()) {
        e.preventDefault();
        return false;
    }
});

/**
 * Auto-calculate shipping when address fields change
 */
$('#checkoutShippingCountry, #checkoutShippingState, #checkoutShippingPostcode').on('change', function() {
    // Auto-calculate if all fields are filled
    const country = $('#checkoutShippingCountry').val();
    const state = $('#checkoutShippingState').val();
    const postcode = $('#checkoutShippingPostcode').val();

    if (country && state && postcode) {
        // Debounce the calculation to avoid too many API calls
        clearTimeout(window.shippingCalculationTimeout);
        window.shippingCalculationTimeout = setTimeout(() => {
            calculateCheckoutShipping();
        }, 1000);
    }
});

// Add CSS for selected shipping option
$('<style>').text(`
    .shipping-option.selected {
        border-color: #007bff !important;
        background-color: #f8f9ff;
    }
    .shipping-option:hover {
        border-color: #007bff !important;
        background-color: #f8f9ff;
    }
`).appendTo('head');