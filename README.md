

```bash
mvn spring-boot:run
```

**1. Same key, twice — should get the identical response back, second call instant:**
```bash
KEY=$(uuidgen)
curl -i -X POST localhost:8080/payments \
  -H "Idempotency-Key: $KEY" -H "Content-Type: application/json" \
  -d '{"accountId":"acc-1","amount":100}'

curl -i -X POST localhost:8080/payments \
  -H "Idempotency-Key: $KEY" -H "Content-Type: application/json" \
  -d '{"accountId":"acc-1","amount":100}'
```
Same `paymentId` both times.

**2. Same key, concurrently (the race condition):**
```bash
for i in 1 2 3; do
  curl -s -X POST localhost:8080/payments \
    -H "Idempotency-Key: $KEY" -H "Content-Type: application/json" \
    -d '{"accountId":"acc-1","amount":100}' &
done
wait
```
All three should return the same `paymentId` — `paymentService.process()` only ran once.

**3. Same key, different body — should 409:**
```bash
curl -i -X POST localhost:8080/payments \
  -H "Idempotency-Key: $KEY" -H "Content-Type: application/json" \
  -d '{"accountId":"acc-1","amount":999}'
```