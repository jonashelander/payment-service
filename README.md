# payment-service

A backend service that receives payment requests, routes them to the right provider (Stripe or Adyen), and saves the result as a transaction. Built with Java 21 and Spring Boot 4.

See [PLAN.md](PLAN.md) for what's built and what's left, and [NOTES.md](NOTES.md) for explanations of the concepts used.

---

## Prerequisites

- Java 21
- PostgreSQL (running locally)
- A Stripe sandbox account (free) if you want to test the real Stripe integration — [dashboard.stripe.com](https://dashboard.stripe.com)
- IntelliJ IDEA with the **EnvFile** plugin (recommended — see below)

---

## 1. Set up the database

```bash
psql postgres
```
```sql
CREATE DATABASE payment_service;
CREATE USER payment_user WITH PASSWORD 'payment_password';
GRANT ALL PRIVILEGES ON DATABASE payment_service TO payment_user;
```

Tables are created automatically on first startup (`ddl-auto: update` in [application-dev.yaml](src/main/resources/application-dev.yaml)).

---

## 2. Set your API keys

The app reads `STRIPE_API_KEY` and `ADYEN_API_KEY` as environment variables — never hardcoded in a file that gets committed.

Create a `.env` file in the project root (it's gitignored, never ends up in git):
```
STRIPE_API_KEY=sk_test_your_real_key
ADYEN_API_KEY=adyen_test_your_key
```

Get your Stripe key from [dashboard.stripe.com → API keys](https://dashboard.stripe.com/apikeys) (sandbox mode, **Secret key**, starts with `sk_test_`). `ADYEN_API_KEY` can be any value for now — the Adyen integration isn't real yet.

**Connect the `.env` file in IntelliJ:**
1. Install the **EnvFile** plugin (Settings → Plugins → search "EnvFile")
2. Run/Debug Configurations → `PaymentServiceApplication (dev)` → check **Enable EnvFile** → **+** → select the `.env` file
3. Run the app

Without IntelliJ/EnvFile: set the variables in your shell before running `mvn spring-boot:run`:
```bash
export STRIPE_API_KEY=sk_test_your_real_key
export ADYEN_API_KEY=adyen_test_your_key
```

---

## 3. Run the app

The `dev` profile must be active (it loads [application-dev.yaml](src/main/resources/application-dev.yaml), which has the database config and API keys):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Or in IntelliJ: run the `PaymentServiceApplication (dev)` run configuration (already has `-Dspring.profiles.active=dev` set).

The app starts on `http://localhost:8080`.

---

## 4. Test a payment

**POST `http://localhost:8080/payments`**, `Content-Type: application/json`:

```json
{
  "providerName": "Stripe",
  "amount": 100.00,
  "paymentMethod": "CARD",
  "cardToken": "pm_card_visa"
}
```

`cardToken` is one of Stripe's fixed test IDs (no real card data is ever sent) — more at [docs.stripe.com/testing](https://docs.stripe.com/testing):

| Token | Result |
|---|---|
| `pm_card_visa` | Successful payment |
| `pm_card_chargeDeclinedIncorrectCvc` | Declined payment (incorrect CVC) |

The response is a `Transaction` with `transactionStatus` (`COMPLETED`/`FAILED`) and `pspTransactionStatus` (Stripe's actual response).

**Other endpoints:**
- `GET /payments/{id}` — look up a transaction
- `GET /payments?status=COMPLETED` — list all transactions with a given status

---

## Running the tests

```bash
./mvnw test
```

The integration tests (`PaymentControllerTest`) run against your real local database — see NOTES.md § Integration testing for why, and what a production setup would do instead (Testcontainers).
