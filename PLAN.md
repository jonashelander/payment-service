# Payment Service — Plan: Real Stripe Integration (PaymentIntents)

## Current status

**Done. Both success (`pm_card_visa`) and decline (`pm_card_chargeDeclinedIncorrectCvc`) paths verified working end-to-end.**

---

## Step 1: Add card token field

- `PaymentRequest.java`: add `String cardToken` + getter/setter — carries which card to charge; nothing today represents that at all. Card data must be collected and tokenized without passing through your own server. https://docs.stripe.com/security/guide

---

## Step 2: Update provider contract

- `PaymentProvider.charge(...)`: add `String cardToken` parameter — `PaymentService` calls through this interface, so the token has to be part of the shared contract to reach any implementation. NOTES.md § Interfaces & abstract classes
- `StripeProvider.charge(...)`: update signature — must match the interface above, or the project doesn't compile
- `AdyenProvider.charge(...)`: update signature only, param unused — same reason, must match the interface
- `PaymentService.processPayment(...)`: pass the new param through — otherwise the token never reaches `charge()`

---

## Step 3: Real Stripe call in `StripeProvider`

- `Authorization: Bearer <secret_key>` header (`HttpHeaders`/`HttpEntity`), key from `providerProperties.getStripe().getApiKey()` — Stripe rejects unauthenticated requests. https://docs.stripe.com/api/authentication

- `amount` in minor units (e.g. `10000` for $100.00) — Stripe reads amounts in cents, not decimals; wrong unit = wrong charge, silently. https://docs.stripe.com/currencies#specify-amounts-in-api-requests

The following three points are all confirmed from the same page, Stripe's own example request for creating a PaymentIntent — https://docs.stripe.com/api/payment_intents/create:
- Request body as `MultiValueMap<String, String>`, form-encoded: `amount`, `currency`, `payment_method`, `confirm=true` — this is the body format Stripe's API actually expects
- `currency` hardcoded for now (e.g. `"eur"`) — mandatory parameter on every PaymentIntent, no field for it exists yet
- POST to `apiUrl + "/v1/payment_intents"` — the endpoint path shown in the example

The following two points are both confirmed from the PaymentIntent object reference — https://docs.stripe.com/api/payment_intents/object:
- Update `StripeChargeResponse` to parse `id`, `status`, `amount`, `currency` — these are the actual fields on the object
- `charge()` returns a `ChargeResult` (provider transaction id + real `status`) instead of a formatted string — `PaymentService` reads `status` itself to decide `COMPLETED`/`FAILED`, so a declined charge isn't silently reported as a success

- `automatic_payment_methods[enabled]=true`, `automatic_payment_methods[allow_redirects]=never` — discovered by actually testing, not from the docs example. The Stripe account has multiple payment methods enabled in the Dashboard, some of which can redirect the customer off-page; without `allow_redirects=never`, Stripe demands a `return_url` we have no use for (no frontend exists to redirect back to). Stripe's own error message named this fix directly.

- Retry scope narrowed: `@Retryable(retryFor = {HttpServerErrorException.class, ResourceAccessException.class})` instead of plain `RuntimeException` — a declined card (4xx) is a deterministic business outcome, not a transient failure; retrying it 3 times wastes time and can look like card-testing fraud to Stripe. Only genuine transient errors (5xx, network issues) are retried. Known simplification: 429 (rate limit) is technically 4xx but should ideally still be retried — not handled by this simple version.

- `Transaction.pspTransactionStatus` (String, `@Column(columnDefinition = "TEXT")`) — always set, on both success and failure, not just on failure. On success: Stripe's real `status` (e.g. `"succeeded"`). On failure: either Stripe's real non-succeeded status, or the exception message for genuine technical failures (401, timeout, etc.).

---

## Step 4: Postman test

```json
{
  "providerName": "Stripe",
  "amount": 100.00,
  "paymentMethod": "CARD",
  "cardToken": "pm_card_visa"
}
```

- Expect 200, `Transaction` status `COMPLETED` — proves the success path from Step 3 works end-to-end
- Retry with `"pm_card_chargeDeclinedIncorrectCvc"` → expect 200, status `FAILED` — proves the failure path still returns cleanly, not a 500. Test PaymentMethod IDs: https://docs.stripe.com/testing

---

## Deferred

- Webhooks (NOTES.md § Callbacks & webhooks, § HMAC signatures)
- Real cashier (Stripe.js/Elements)
- Dynamic `currency` field
- Automated tests (`MockRestServiceServer`)
