package g6.fashionFlex.service;

import g6.fashionFlex.config.VNPayConfig;
import g6.fashionFlex.entity.Order;
import g6.fashionFlex.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * VNPay Payment Service
 * Handles VNPay payment processing and callback verification
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayService {

    private final VNPayConfig vnPayConfig;

    /**
     * Create VNPay payment URL for order
     *
     * @param order Order to create payment for
     * @param request HTTP request for IP address extraction
     * @return VNPay payment URL
     */
    public String createPaymentUrl(Order order, HttpServletRequest request) {
        try {
            log.info("Creating VNPay payment URL for order: {}", order.getOrderNumber());

            // Convert amount to VNPay format (multiply by 100, no decimal)
            long amount = order.getTotalAmount().multiply(new BigDecimal(100)).longValue()*25000;

            // Get client IP
            String vnp_IpAddr = VNPayUtil.getIpAddress(request);

            // Build VNPay parameters
            Map<String, String> vnpParams = new HashMap<>();
            vnpParams.put("vnp_Version", vnPayConfig.getVersion());
            vnpParams.put("vnp_Command", vnPayConfig.getCommand());
            vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
            vnpParams.put("vnp_Amount", String.valueOf(amount));
            vnpParams.put("vnp_CurrCode", vnPayConfig.getCurrCode());

            // Use order ID as transaction reference
            vnpParams.put("vnp_TxnRef", order.getId().toString());
            vnpParams.put("vnp_OrderInfo", "Thanh toan don hang: " + order.getOrderNumber());
            vnpParams.put("vnp_OrderType", vnPayConfig.getOrderType());
            vnpParams.put("vnp_Locale", vnPayConfig.getLocale());
            vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
            vnpParams.put("vnp_IpAddr", vnp_IpAddr);

            // Add timestamps
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnp_CreateDate = formatter.format(calendar.getTime());
            vnpParams.put("vnp_CreateDate", vnp_CreateDate);

            // Payment expires in 15 minutes
            calendar.add(Calendar.MINUTE, 15);
            String vnp_ExpireDate = formatter.format(calendar.getTime());
            vnpParams.put("vnp_ExpireDate", vnp_ExpireDate);

            // Build hash data and query string
            String hashData = VNPayUtil.buildHashData(vnpParams);
            String queryUrl = VNPayUtil.buildQueryString(vnpParams);

            // Generate secure hash
            String vnpSecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getSecretKey(), hashData);
            queryUrl += "&vnp_SecureHash=" + vnpSecureHash;

            // Build final payment URL
            String paymentUrl = vnPayConfig.getPayUrl() + "?" + queryUrl;

            log.info("VNPay payment URL created successfully for order: {}", order.getOrderNumber());
            log.debug("Payment URL: {}", paymentUrl);

            return paymentUrl;

        } catch (Exception e) {
            log.error("Error creating VNPay payment URL for order: {}", order.getOrderNumber(), e);
            throw new RuntimeException("Failed to create VNPay payment URL: " + e.getMessage(), e);
        }
    }

    /**
     * Verify VNPay callback signature
     *
     * @param request HTTP request containing VNPay callback parameters
     * @return true if signature is valid, false otherwise
     */
    public boolean verifyCallback(HttpServletRequest request) {
        try {
            log.info("Verifying VNPay callback signature");

            // Extract all parameters
            Map<String, String> fields = new HashMap<>();
            for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
                String fieldName = params.nextElement();
                String fieldValue = request.getParameter(fieldName);

                if (fieldValue != null && !fieldValue.isEmpty()) {
                    try {
                        fields.put(
                            URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()),
                            URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString())
                        );
                    } catch (Exception e) {
                        log.error("Error encoding parameter: {} = {}", fieldName, fieldValue, e);
                    }
                }
            }

            // Get secure hash from VNPay
            String vnpSecureHash = request.getParameter("vnp_SecureHash");
            if (vnpSecureHash == null) {
                log.warn("No vnp_SecureHash in callback");
                return false;
            }

            // Remove hash fields before verification
            fields.remove(URLEncoder.encode("vnp_SecureHashType", StandardCharsets.US_ASCII.toString()));
            fields.remove(URLEncoder.encode("vnp_SecureHash", StandardCharsets.US_ASCII.toString()));

            // Generate hash from received fields
            String signValue = VNPayUtil.hashAllFields(fields, vnPayConfig.getSecretKey());

            // Verify signature
            boolean isValid = signValue.equals(vnpSecureHash);
            log.info("VNPay callback signature verification: {}", isValid ? "SUCCESS" : "FAILED");

            return isValid;

        } catch (Exception e) {
            log.error("Error verifying VNPay callback", e);
            return false;
        }
    }

    /**
     * Extract payment result from VNPay callback
     *
     * @param request HTTP request containing VNPay callback parameters
     * @return Map containing payment result data
     */
    public Map<String, String> getPaymentResult(HttpServletRequest request) {
        Map<String, String> result = new HashMap<>();

        try {
            result.put("orderId", request.getParameter("vnp_TxnRef"));
            result.put("transactionNo", request.getParameter("vnp_TransactionNo"));
            result.put("responseCode", request.getParameter("vnp_ResponseCode"));
            result.put("transactionStatus", request.getParameter("vnp_TransactionStatus"));
            result.put("amount", request.getParameter("vnp_Amount"));
            result.put("bankCode", request.getParameter("vnp_BankCode"));
            result.put("cardType", request.getParameter("vnp_CardType"));
            result.put("orderInfo", request.getParameter("vnp_OrderInfo"));
            result.put("payDate", request.getParameter("vnp_PayDate"));

            log.info("Payment result extracted - orderId: {}, responseCode: {}, transactionStatus: {}",
                    result.get("orderId"), result.get("responseCode"), result.get("transactionStatus"));

        } catch (Exception e) {
            log.error("Error extracting payment result", e);
        }

        return result;
    }

    /**
     * Check if VNPay transaction was successful
     *
     * @param responseCode VNPay response code
     * @param transactionStatus VNPay transaction status
     * @return true if transaction was successful
     */
    public boolean isPaymentSuccess(String responseCode, String transactionStatus) {
        return "00".equals(responseCode) && "00".equals(transactionStatus);
    }

    /**
     * Get VNPay response code description
     *
     * @param responseCode VNPay response code
     * @return Human-readable description
     */
    public String getResponseDescription(String responseCode) {
        if (responseCode == null) return "Unknown error";

        switch (responseCode) {
            case "00":
                return "Transaction successful";
            case "07":
                return "Transaction suspicious (related to fraud, unusual transactions)";
            case "09":
                return "Customer's card/account not registered for online payment";
            case "10":
                return "Customer authentication failed more than 3 times";
            case "11":
                return "Payment deadline expired. Please try again";
            case "12":
                return "Card/Account is locked";
            case "13":
                return "Incorrect transaction password (OTP)";
            case "24":
                return "Customer cancelled transaction";
            case "51":
                return "Insufficient account balance";
            case "65":
                return "Daily transaction limit exceeded";
            case "75":
                return "Payment bank under maintenance";
            case "79":
                return "Incorrect payment password too many times";
            default:
                return "Transaction failed - Error code: " + responseCode;
        }
    }
}
