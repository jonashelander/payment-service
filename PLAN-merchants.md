# Feature: Merchants

## Goal

Allow merchants to be created via the API, and require every payment transaction to be linked to an existing merchant.

## Current state

- `Merchant` entity exists (`id`, `name`) with a `@OneToMany` to `Transaction`
- `Transaction` has a `@ManyToOne merchant` field but it is nullable — nothing enforces it
- `MerchantRepository` exists but there is no service or controller for merchants
- `PaymentRequest` has no `merchantId` field; `PaymentService` never sets a merchant on the transaction

## What to build

### ✅ 1. MerchantRequest DTO
New file: `dto/MerchantRequest.java`
- Field: `String name`
- Represents the JSON body sent to `POST /merchants` — needed so the controller has a typed object to read the incoming name from

### ✅ 2. MerchantService
New file: `service/MerchantService.java`
- `createMerchant(String name) → Merchant` — checks that the name is not already taken, then saves and returns the new merchant
- `findMerchantById(Long id) → Merchant` — used by `GET /merchants/{id}` and later by the FE
- `findMerchantByName(String name) → Merchant` — used by `PaymentService` to look up the merchant when a payment is initiated

### ✅ 3. MerchantNotFoundException
New file: `exception/MerchantNotFoundException.java`
- Thrown when a merchant name or ID doesn't exist, so the caller gets a clear 404 instead of a generic error

### ✅ 4. MerchantController
New file: `controller/MerchantController.java`
- `POST /merchants` — creates a merchant
- `GET /merchants/{id}` — fetches a merchant by ID, needed later when the FE lists a merchant's transactions

### ✅ 5. PaymentRequest — add merchant
- Add `String merchant` to `PaymentRequest`
- The payment request needs to know which merchant the transaction belongs to

### ✅ 6. PaymentService — enforce merchant
- Look up the merchant by the `merchant` name from the request before creating the transaction, so a payment can never be saved without a valid merchant attached to it

### ✅ 7. Make merchant_id non-nullable in the DB
- Enforces at the database level that a transaction always has a merchant, not just in the service layer

### ✅ 8. Update tests
- `PaymentServiceTest` and `PaymentControllerTest` currently create transactions without a merchant — they need to be updated to match the new requirement

### ✅ 9. Fixa cirkulär JSON-serialisering
- `Merchant` har `@OneToMany List<Transaction>` och `Transaction` har `@ManyToOne Merchant`
- Jackson följer båda sidor i oändlighet när det serialiserar svaret
- Lösning: lägg `@JsonIgnore` på `transactions`-fältet i `Merchant`
- `transactions`-listan behövs inte i API-svaret — den är en JPA-hjälpstruktur, inte ett datakrav

## Decisions

- **Unique names** — duplicate merchant names are rejected. `MerchantRepository.findByname` is used to check before saving. In a real system a merchant would have multiple MIDs/brands, but that's out of scope for now.
- **merchant in PaymentRequest** — the payment request uses `merchant` (not `merchantId`) since names are unique and more readable.
- **GET /merchants/{id}** — included, needed later for the FE where a merchant's transactions will be listed.

## API

**Create a merchant**
```
POST http://localhost:8080/merchants
Content-Type: application/json

{
  "name": "Acme Corp"
}
```

**Create a payment with a merchant**
```
POST http://localhost:8080/payments
Content-Type: application/json

{
  "merchant": "Acme Corp",
  "providerName": "Stripe",
  "amount": 100.00,
  "paymentMethod": "CARD",
  "cardToken": "pm_card_visa"
}
```

## Done when

- `POST /merchants` creates a merchant and returns it
- `POST /payments` without a valid `merchantId` returns 404
- The `merchant_id` column in the DB is NOT NULL
- All existing tests pass with the merchant added
