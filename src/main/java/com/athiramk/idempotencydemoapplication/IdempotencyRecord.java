package com.athiramk.idempotencydemoapplication;

import java.time.Instant;

public class IdempotencyRecord {
    enum Status { IN_PROGRESS, COMPLETED }

    private volatile Status status;
    private volatile Object response;
    private volatile int httpStatus;
    private final String requestHash; // fingerprint of the original request body
    private final Instant createdAt;

    public IdempotencyRecord(String requestHash) {
        this.status = Status.IN_PROGRESS;
        this.requestHash = requestHash;
        this.createdAt = Instant.now();
    }

    public synchronized void complete(Object response, int httpStatus) {
        this.response = response;
        this.httpStatus = httpStatus;
        this.status = Status.COMPLETED;
    }

    public Status getStatus() { return status; }
    public Object getResponse() { return response; }
    public int getHttpStatus() { return httpStatus; }
    public String getRequestHash() { return requestHash; }
    public Instant getCreatedAt() { return createdAt; }
}