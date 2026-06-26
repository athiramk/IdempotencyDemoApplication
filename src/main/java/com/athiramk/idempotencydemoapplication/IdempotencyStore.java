package com.athiramk.idempotencydemoapplication;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IdempotencyStore {

    private final Map<String, IdempotencyRecord> store = new ConcurrentHashMap<>();
    //private final Duration ttl = Duration.ofHours(24);
    private final Duration ttl = Duration.ofMinutes(1);

    public record ClaimResult(IdempotencyRecord record, boolean wonRace) {}

    /** Atomically claims the key. wonRace=true means caller must process the request. */
    public ClaimResult claimKey(String key, String requestHash) {
        boolean[] won = {false};
        IdempotencyRecord record = store.compute(key, (k, current) -> {
            if (current != null && !isExpired(current)) {
                return current; // someone else already owns this key
            }
            won[0] = true;
            return new IdempotencyRecord(requestHash);
        });
        return new ClaimResult(record, won[0]);
    }

    private boolean isExpired(IdempotencyRecord record) {
        return Instant.now().isAfter(record.getCreatedAt().plus(ttl));
    }
}