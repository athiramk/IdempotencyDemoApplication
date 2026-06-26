package com.athiramk.idempotencydemoapplication;

import com.athiramk.idempotencydemoapplication.dto.PaymentRequest;
import com.athiramk.idempotencydemoapplication.dto.PaymentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final IdempotencyStore idempotencyStore;

    public PaymentController(PaymentService paymentService, IdempotencyStore idempotencyStore) {
        this.paymentService = paymentService;
        this.idempotencyStore = idempotencyStore;
    }

    @PostMapping
    public ResponseEntity<?> createPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody PaymentRequest request) {

        String requestHash = hash(request.toString());
        IdempotencyStore.ClaimResult claim = idempotencyStore.claimKey(idempotencyKey, requestHash);
        IdempotencyRecord record = claim.record();

        // Same key, DIFFERENT body -> reject. This is the "validate match" rule from your notes.
        if (!record.getRequestHash().equals(requestHash)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Idempotency-Key already used with a different request body");
        }

        if (!claim.wonRace()) {
            // Either still processing (concurrent retry) or already completed.
            return waitOrReturnCached(record);
        }

        // We won the race - actually do the work.
        PaymentResponse response = paymentService.process(request);
        record.complete(response, HttpStatus.CREATED.value());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private ResponseEntity<?> waitOrReturnCached(IdempotencyRecord record) {
        // Simple bounded poll - real systems might return 409 "still processing"
        // or use a CompletableFuture/condition variable instead of busy-waiting.
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (record.getStatus() == IdempotencyRecord.Status.IN_PROGRESS) {
            if (Instant.now().isAfter(deadline)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Request still processing, retry later");
            }
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }
        return ResponseEntity.status(record.getHttpStatus()).body(record.getResponse());
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}