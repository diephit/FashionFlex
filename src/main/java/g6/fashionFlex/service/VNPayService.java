package g6.fashionFlex.service;

import g6.fashionFlex.config.VNPAYConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VNPayService {

    public String createOrder(HttpServletRequest request, long amount, String orderInfo, String urlReturn) {
        // Validate amount
        if (amount < 5000) {
            throw new IllegalArgumentException("Amount must be at least 5,000 VND");
        }
        
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = VNPAYConfig.getRandomNumber(8);
        String vnp_IpAddr = VNPAYConfig.getIpAddress(request);
        String vnp_TmnCode = VNPAYConfig.vnp_TmnCode;
        String orderType = "other";

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount * 100));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", orderInfo);
        vnp_Params.put("vnp_OrderType", orderType);

        String locate = request.getParameter("language");
        if (locate != null && !locate.isEmpty()) {
            vnp_Params.put("vnp_Locale", locate);
        } else {
            vnp_Params.put("vnp_Locale", "vn");
        }
        vnp_Params.put("vnp_ReturnUrl", urlReturn);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        
        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // Build hash data và query string
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        
        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hashData - dùng URLEncoder với UTF-8
                try {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                    
                    // Build query - giống như hashData
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                    
                    query.append('&');
                    hashData.append('&');
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        
        // Xóa dấu & cuối cùng
        if (hashData.length() > 0) {
            hashData.setLength(hashData.length() - 1);
        }
        if (query.length() > 0) {
            query.setLength(query.length() - 1);
        }
        
        // Tạo secure hash
        String vnp_SecureHash = VNPAYConfig.hmacSHA512(VNPAYConfig.vnp_HashSecret, hashData.toString());
        
        String queryUrl = query.toString() + "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = VNPAYConfig.vnp_PayUrl + "?" + queryUrl;
        
        // Debug
        System.out.println("=== VNPay Create Order Debug ===");
        System.out.println("TmnCode: " + vnp_TmnCode);
        System.out.println("TxnRef: " + vnp_TxnRef);
        System.out.println("Amount: " + amount);
        System.out.println("OrderInfo: " + orderInfo);
        System.out.println("Hash Secret: " + VNPAYConfig.vnp_HashSecret);
        System.out.println("Hash Data: " + hashData.toString());
        System.out.println("Secure Hash: " + vnp_SecureHash);
        System.out.println("Payment URL: " + paymentUrl);
        System.out.println("================================");
        
        return paymentUrl;
    }

    public int orderReturn(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        
        // Lấy tất cả parameters (Spring tự động decode)
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        if (vnp_SecureHash == null) {
            System.out.println("ERROR: vnp_SecureHash is null");
            return -1;
        }
        
        // Remove secure hash fields
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");
        
        // Tính hash để verify
        String signValue = VNPAYConfig.hashAllFields(fields);
        
        // Debug chi tiết
        System.out.println("=== VNPay Return Debug ===");
        System.out.println("Raw Query String: " + request.getQueryString());
        System.out.println("All Parameters:");
        fields.forEach((key, value) -> System.out.println("  " + key + " = " + value));
        System.out.println("Received Hash: " + vnp_SecureHash);
        System.out.println("Calculated Hash: " + signValue);
        System.out.println("Match: " + signValue.equals(vnp_SecureHash));
        System.out.println("Response Code: " + request.getParameter("vnp_ResponseCode"));
        System.out.println("Transaction Status: " + request.getParameter("vnp_TransactionStatus"));
        System.out.println("Transaction No: " + request.getParameter("vnp_TransactionNo"));
        System.out.println("Bank Code: " + request.getParameter("vnp_BankCode"));
        System.out.println("=========================");
        
        if (signValue.equals(vnp_SecureHash)) {
            String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
            String vnp_TransactionStatus = request.getParameter("vnp_TransactionStatus");
            
            if ("00".equals(vnp_ResponseCode) && "00".equals(vnp_TransactionStatus)) {
                return 1; 
            } else {
                return 0; 
            }
        } else {
            return -1; 
        }
    }
}