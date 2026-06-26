package com.athiramk.idempotencydemoapplication.dto;

public record PaymentRequest(String accountId, double amount) {}