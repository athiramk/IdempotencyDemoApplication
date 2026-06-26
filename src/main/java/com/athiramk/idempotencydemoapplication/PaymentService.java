package com.athiramk.idempotencydemoapplication;

import com.athiramk.idempotencydemoapplication.dto.PaymentRequest;
import com.athiramk.idempotencydemoapplication.dto.PaymentResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentService {
    public PaymentResponse process(PaymentRequest request) {
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {} // simulate latency
        return new PaymentResponse(UUID.randomUUID().toString(), request.amount(), "SUCCESS");
    }
}