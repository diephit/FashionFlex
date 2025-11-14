package g6.fashionFlex.service;

import g6.fashionFlex.entity.Payment;
import g6.fashionFlex.entity.Payment.PaymentStatus;
import g6.fashionFlex.entity.PaymentHistory;
import g6.fashionFlex.repository.PaymentHistoryRepository;
import g6.fashionFlex.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AdminPaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;

    public Page<Payment> getAllPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable);
    }

    public Page<Payment> searchPayments(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return paymentRepository.findAll(pageable);
        }
        return paymentRepository.searchPayments(keyword, pageable);
    }

    public List<Payment> getPaymentsByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status);
    }

    public Optional<Payment> getPaymentById(Integer paymentID) {
        return paymentRepository.findById(paymentID);
    }

    public List<Payment> getOrderPayments(Integer orderID) {
        return paymentRepository.findByOrderOrderID(orderID);
    }

    public List<PaymentHistory> getPaymentHistory(Integer paymentID) {
        return paymentHistoryRepository.findByPaymentPaymentID(paymentID);
    }

    @Transactional
    public Payment updatePaymentStatus(Integer paymentID, PaymentStatus status, String gatewayResponse) {
        Payment payment = paymentRepository.findById(paymentID)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        payment.setStatus(status);
        Payment savedPayment = paymentRepository.save(payment);
        
        // Record in payment history
        PaymentHistory history = new PaymentHistory();
        history.setPayment(savedPayment);
        history.setStatus(convertPaymentStatus(status));
        history.setGatewayResponse(gatewayResponse);
        paymentHistoryRepository.save(history);
        
        return savedPayment;
    }

    @Transactional
    public Payment processRefund(Integer paymentID, String note) {
        Payment payment = paymentRepository.findById(paymentID)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        if (payment.getStatus() != PaymentStatus.success) {
            throw new RuntimeException("Can only refund successful payments");
        }
        
        payment.setStatus(PaymentStatus.refunded);
        Payment savedPayment = paymentRepository.save(payment);
        
        // Record in payment history
        PaymentHistory history = new PaymentHistory();
        history.setPayment(savedPayment);
        history.setStatus(PaymentHistory.PaymentStatus.refunded);
        history.setNote(note);
        paymentHistoryRepository.save(history);
        
        return savedPayment;
    }

    private PaymentHistory.PaymentStatus convertPaymentStatus(PaymentStatus status) {
        switch (status) {
            case pending:
                return PaymentHistory.PaymentStatus.pending;
            case success:
                return PaymentHistory.PaymentStatus.success;
            case failed:
                return PaymentHistory.PaymentStatus.failed;
            case refunded:
                return PaymentHistory.PaymentStatus.refunded;
            default:
                return PaymentHistory.PaymentStatus.pending;
        }
    }
}

