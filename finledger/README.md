# FinLedger

FinLedger is a small banking platform built as three Spring Boot services:

- `account-service` stores accounts and applies balance changes in PostgreSQL.
- `transaction-service` performs transfers and stores an outbox event in PostgreSQL.
- `notification-service` consumes transfer events from Kafka and stores notifications in MongoDB.

## Run the complete stack

Requirements: Docker Compose, Java 21, and Maven 3.9 or newer.

```bash
cd finledger
docker compose up --build
```

For a clean database after changing migrations, use `docker compose down -v` before starting again. This removes local development data. Account and transaction data are stored in separate PostgreSQL schemas.

The services are available at:

- Account API: `http://localhost:8081`
- Transaction API: `http://localhost:8082`
- Notification API: `http://localhost:8083`
- Health checks: `/actuator/health`

Register a user through the account service, then use the returned JWT as a Bearer token. Set `JWT_SECRET` before starting Compose outside local development.

```bash
curl -X POST http://localhost:8081/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"correct-horse-battery"}'
```

## Example flow

Create two accounts:

```bash
curl -X POST http://localhost:8081/accounts \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"ownerName":"Alice","initialBalance":1000}'
```

Use the returned account numbers to transfer money:

```bash
curl -X POST http://localhost:8082/transfers \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: transfer-001' \
  -d '{"fromAccount":"ACC...","toAccount":"ACC...","amount":125.50}'
```

The same idempotency key returns the existing transfer instead of applying the transfer twice. Kafka publishes the event through the PostgreSQL outbox, and notifications can be read with:

```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" http://localhost:8083/notifications/ACC...
```

## Authorise, settle, or release funds

FinLedger distinguishes the **ledger balance** (posted money) from the **available balance** (money a customer can spend). A transfer first creates a durable, idempotent hold on the sender account. The hold lowers only the available balance; it is converted to a posted debit when the transfer settles. If a payment is cancelled, declined, or expires, the hold can be released without changing the ledger balance.

```text
AVAILABLE = LEDGER BALANCE - ACTIVE HOLDS

PENDING_HOLD -> PENDING_DEBIT -> PENDING_CREDIT -> COMPLETED
                                      |
                                      +-> COMPENSATION_PENDING -> COMPENSATED
```

This represents the real-world issue behind card authorisations and delayed bank-payment settlement: a customer must not be able to spend the same funds while a payment is still being confirmed. The transaction service calls the service-only hold APIs; their `X-Hold-Key` makes authorization, settlement, and release safe to retry.

## UPI-style pending payment reconciliation

Every transfer receives a customer-visible `paymentReference` (`UPI` + 12 digits) and an explicit reconciliation result:

```text
PENDING  -> SETTLED   (beneficiary credit confirmed)
PENDING  -> REVERSED  (sender debit was compensated)
PENDING  -> FAILED    (payment could not be authorised)
```

The transaction API exposes `GET /transfers` for the authenticated customer's last 50 payments, including the reference and reconciliation status. Its scheduled recovery worker retries unresolved `PENDING_HOLD`, `PENDING_DEBIT`, `PENDING_CREDIT`, and compensation states. This models a common UPI/IMPS operational problem: a sender must receive a final, traceable result even if the receiving-bank call times out after the sender side has started processing.

This is a simulated bank-network flow, not an integration with NPCI or a live payment rail.

## Local Maven verification

```bash
mvn -f pom.xml test
mvn -f pom.xml package
```

Database and broker connection values can be overridden with environment variables. See the `application.yaml` files in each service for the available names.

## Current production hardening boundary

The project now includes input validation, balance locking, available-versus-ledger balances, idempotent payment holds and settlement, UPI-style pending-payment reconciliation with customer payment references and automatic recovery, an event outbox with retries, notification deduplication, health endpoints, environment-driven connection settings, database migrations, JWT authentication, account ownership authorization, React-ready CORS, and container builds. Immutable double-entry ledger postings, external payment-rail integration, distributed tracing, full PostgreSQL/Kafka/Mongo integration tests, refresh-token rotation, and external notification delivery remain before handling real customer money.
