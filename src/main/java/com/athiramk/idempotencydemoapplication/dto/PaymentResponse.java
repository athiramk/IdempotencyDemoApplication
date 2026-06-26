package com.athiramk.idempotencydemoapplication.dto;

public record PaymentResponse(String paymentId, double amount, String status) {}
