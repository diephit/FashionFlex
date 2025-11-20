// Verify Token Page - Resend Code Functionality with Countdown Timer

let countdownInterval = null;
let remainingSeconds = 60;

document.addEventListener('DOMContentLoaded', function() {
    const resendBtn = document.getElementById('resendBtn');
    const cooldownText = document.getElementById('cooldownText');

    if (!resendBtn) return;

    // Check initial cooldown status
    checkCooldownStatus();

    // Resend button click handler
    resendBtn.addEventListener('click', function() {
        if (resendBtn.disabled) return;

        // Disable button
        resendBtn.disabled = true;
        resendBtn.style.backgroundColor = '#999';
        resendBtn.style.cursor = 'not-allowed';

        // Send AJAX request
        fetch('/resend-code?email=' + encodeURIComponent(userEmail), {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            }
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                // Show success message
                showMessage('Verification code sent successfully!', 'success');

                // Start countdown
                remainingSeconds = data.cooldown || 60;
                startCountdown();
            } else {
                // Show error message
                showMessage(data.message, 'error');

                // Re-enable button after a short delay
                setTimeout(() => {
                    resendBtn.disabled = false;
                    resendBtn.style.backgroundColor = '#6c757d';
                    resendBtn.style.cursor = 'pointer';
                }, 2000);

                // If there's remaining time, start countdown
                if (data.remainingSeconds && data.remainingSeconds > 0) {
                    remainingSeconds = data.remainingSeconds;
                    startCountdown();
                }
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showMessage('An error occurred. Please try again.', 'error');

            // Re-enable button
            setTimeout(() => {
                resendBtn.disabled = false;
                resendBtn.style.backgroundColor = '#6c757d';
                resendBtn.style.cursor = 'pointer';
            }, 2000);
        });
    });

    function startCountdown() {
        // Clear any existing interval
        if (countdownInterval) {
            clearInterval(countdownInterval);
        }

        // Show cooldown text
        cooldownText.style.display = 'block';
        updateCooldownText();

        // Start countdown
        countdownInterval = setInterval(() => {
            remainingSeconds--;

            if (remainingSeconds <= 0) {
                // Countdown finished
                clearInterval(countdownInterval);
                countdownInterval = null;

                // Re-enable button
                resendBtn.disabled = false;
                resendBtn.style.backgroundColor = '#6c757d';
                resendBtn.style.cursor = 'pointer';
                resendBtn.textContent = 'Resend Code';

                // Hide cooldown text
                cooldownText.style.display = 'none';
            } else {
                updateCooldownText();
            }
        }, 1000);
    }

    function updateCooldownText() {
        resendBtn.textContent = `Resend Code (${remainingSeconds}s)`;
        cooldownText.textContent = `You can request a new code in ${remainingSeconds} seconds`;
    }

    function checkCooldownStatus() {
        fetch('/check-cooldown?email=' + encodeURIComponent(userEmail))
            .then(response => response.json())
            .then(data => {
                if (data.success && !data.canResend && data.remainingSeconds > 0) {
                    remainingSeconds = data.remainingSeconds;
                    resendBtn.disabled = true;
                    resendBtn.style.backgroundColor = '#999';
                    resendBtn.style.cursor = 'not-allowed';
                    startCountdown();
                }
            })
            .catch(error => {
                console.error('Error checking cooldown:', error);
            });
    }

    function showMessage(message, type) {
        // Remove existing alert
        const existingAlert = document.querySelector('.alert-dynamic');
        if (existingAlert) {
            existingAlert.remove();
        }

        // Create new alert
        const alert = document.createElement('div');
        alert.className = 'alert alert-dynamic alert-' + type;
        alert.style.padding = '10px';
        alert.style.marginBottom = '15px';
        alert.style.borderRadius = '5px';
        alert.style.border = '1px solid';

        if (type === 'success') {
            alert.style.backgroundColor = '#d4edda';
            alert.style.color = '#155724';
            alert.style.borderColor = '#c3e6cb';
        } else {
            alert.style.backgroundColor = '#f8d7da';
            alert.style.color = '#721c24';
            alert.style.borderColor = '#f5c6cb';
        }

        alert.textContent = message;

        // Insert at top of forgot-box
        const forgotBox = document.querySelector('.forgot-box');
        const forgotHeader = document.querySelector('.forgot-header');
        forgotBox.insertBefore(alert, forgotHeader.nextSibling);

        // Auto remove after 5 seconds
        setTimeout(() => {
            alert.remove();
        }, 5000);
    }
});
