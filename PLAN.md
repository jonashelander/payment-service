# Payment Service — Plan

A payment routing service that receives payment requests, routes them to the correct provider (Stripe, Adyen), persists transactions to a database, and handles provider responses. Built topic by topic — each step adds a layer to the same running application.

---

## Current status

**Step 10 — Testing — assignments 2, 3, 4 done, assignment 5 next (integration test POST /payments)**

Update this line every session. Format: `Step N — Topic name — in progress / done`

---

## The app

A merchant sends a payment request to your service. Your service picks the right provider (Stripe or Adyen), calls their API, persists the result as a transaction, and returns the outcome. Each step below adds a piece of that flow.

---

## How the pieces fit together

Before you build anything, read this. It explains what every class in the system does and how they connect — so nothing you create in the steps below feels arbitrary.

---

### The flow of a payment request

```
HTTP request
    │
    ▼
PaymentController          ← receives the HTTP request, hands it off
    │
    ▼
PaymentService             ← orchestrates everything: find provider, call it, save result
    │           │
    │           ▼
    │     TransactionRepository   ← saves and reads Transaction records in the database
    │
    ▼
ProviderRegistry           ← knows which providers exist, finds the right one by name
    │
    ▼
PaymentProvider            ← interface: defines what every provider must be able to do
    │
AbstractPaymentProvider    ← implements PaymentProvider; shared fields and common code for all providers
    │
    ├── StripeProvider     ← extends AbstractPaymentProvider; calls Stripe's API
    └── AdyenProvider      ← extends AbstractPaymentProvider; calls Adyen's API
```

A request comes in at the top and flows downward. The response travels back up the same path.

---

### What each piece is

**`PaymentController`**
The entry point for HTTP. When a merchant sends `POST /payments`, this class receives it. It knows nothing about providers or databases — its only job is to accept the request and hand it to `PaymentServiceTest`, then return whatever `PaymentServiceTest` gives back.

**`PaymentServiceTest`**
The brain of the operation. It coordinates everything: asks `ProviderRegistry` for the right provider, calls `charge()` on it, and tells `TransactionRepository` to save the result. It is the only class that knows the full sequence of events for processing a payment.

**`ProviderRegistry`**
A directory of all available providers. When `PaymentServiceTest` says "I need Stripe", `ProviderRegistry` finds it. It holds a list of every `PaymentProvider` and can look one up by name.

**`PaymentProvider` (interface)**
A contract. It says: every provider — no matter how different their API — must implement `charge()` and `getName()`. `PaymentServiceTest` calls `charge()` without knowing or caring whether it's talking to Stripe or Adyen. This is what makes it easy to add a new provider later: implement the interface, register it, done.

**`AbstractPaymentProvider` (abstract class)**
Common ground shared by all providers. Things like building a log message or holding the API URL are the same regardless of provider — so they live here instead of being duplicated in every provider class. Provider-specific logic stays in the concrete class.

**`StripeProvider` / `AdyenProvider`**
The actual integrations. Each one knows how to talk to its specific provider: what URL to call, what format the request needs to be in, how to read the response. Everything unique to that provider lives here.

**`TransactionRepository`**
The connection to the database. Every payment attempt is saved as a `Transaction` record — with its status, amount, provider, and timestamp. `TransactionRepository` handles all reading and writing. `PaymentServiceTest` uses it but never writes SQL directly.

**`Transaction` (entity)**
A database row. Represents one payment attempt: who processed it, how much, which provider, what the outcome was, and when it happened. This is the permanent record of everything the system has done.

---

### The domain objects

These are not classes that *do* things — they are objects that *represent* things.

**`PaymentRequest`** — what comes in from the merchant: provider name, amount, payment method. This is the input.

**`Transaction`** — what gets saved to the database: the full record of what happened. This is the output and the audit trail.

**`TransactionStatus`** (enum) — the three states a transaction can be in: `PENDING` (created, not yet processed), `COMPLETED` (provider accepted it), `FAILED` (provider rejected it or something went wrong).

**`PaymentMethod`** (enum) — how the payment is being made: `CARD` or `BANK_TRANSFER`.

---

### Why this structure

You could put all of this in one class. It would work. But in a real codebase with multiple developers:

- `PaymentController` only changes when the HTTP API changes
- `PaymentServiceTest` only changes when the payment flow changes
- `StripeProvider` only changes when Stripe's API changes

Each class has one reason to change. When a new provider arrives, you add one class and register it — nothing else needs to touch. When Stripe updates their API, you edit `StripeProvider` and nothing else breaks. This is the structure you will be working inside of when you join a team.

---

## Step 0: Project setup

Not a learning topic — infrastructure. The app needs to exist and run before anything else.

**What gets created:**
- Maven project with Spring Boot
- `pom.xml` with starter dependencies
- `PaymentServiceApplication.java` — the entry point annotated with `@SpringBootApplication`
- `application.yml` — basic config
- Package structure under `com.example.paymentservice`

**Assignments:**
1. Create the project structure (done together)
2. Verify the app starts with `mvn spring-boot:run`

---

## Step 1: Beans & the application context

→ Read first: NOTES.md § Beans & the application context

Spring manages objects for you. A bean is an object Spring creates, owns, and makes available to the rest of the app. The application context is the container that holds all beans. You register a class as a bean by annotating it — Spring does the rest.

This is the foundation everything else builds on. You will also create the domain model here — the core objects the rest of the app will work with.

**What you'll build:** The domain model and first provider beans.

**Package structure:**
- `model/` — `TransactionStatus`, `PaymentMethod`
- `provider/` — `PaymentProvider`, `AbstractPaymentProvider`, `ProviderRegistry`
- `provider/stripe/` — `StripeProvider`
- `provider/adyen/` — `AdyenProvider`

Each provider lives in its own sub-package. This matches how real payment systems are structured — each integration is isolated and can grow independently without cluttering the shared provider package.

**Assignments:**
1. Create `TransactionStatus.java` enum in `model/` — values: `PENDING`, `COMPLETED`, `FAILED`
   *Reuses: Enums*
2. Create `PaymentMethod.java` enum in `model/` — values: `CARD`, `BANK_TRANSFER`
   *Reuses: Enums*
3. Create `PaymentProvider.java` interface in `provider/` — methods: `String getName()`, `String charge(BigDecimal amount, PaymentMethod method)`
   *Reuses: Interfaces & abstract classes*
4. Create `AbstractPaymentProvider.java` abstract class in `provider/` — implements `PaymentProvider`, with the following:
   - A `protected String apiUrl` field — this is the base URL of the provider's API (e.g. `"https://api.stripe.com"`). It lives here so every provider has it, but each provider sets its own value. It's `protected` so subclasses can read it directly.
   - Do NOT implement `getName()` or `charge()` here — leave them for the concrete subclasses. The abstract class only holds shared fields and shared methods.
   - A `protected String buildLogMessage(String action, BigDecimal amount)` method — `action` is a word describing what is being done, e.g. `"charge"` or `"refund"`. This lets the same method build log strings for different operations. It should call `getName()` (which the subclass will implement) so the provider name is included. Example output: `"Stripe: charge of 100.0"`. Format it however feels readable.
   *Reuses: Inheritance & abstract classes*
5. Create `StripeProvider.java` in `provider/stripe/` extending `AbstractPaymentProvider`:
   - In the constructor, set `apiUrl` to `"https://api.stripe.com"` — this is just a placeholder, we're not making real HTTP calls yet
   - Implement `getName()` — return `"Stripe"`
   - Implement `charge()` — call `buildLogMessage("charge", amount)` and return the result. This simulates what a real charge call would log before it hits the actual API.
   - Annotate the class with `@Component` so Spring creates it as a bean
6. Create `AdyenProvider.java` in `provider/adyen/` extending `AbstractPaymentProvider` — same structure as `StripeProvider`, but `apiUrl` is `"https://api.adyen.com"` and `getName()` returns `"Adyen"`
7. Run the app — verify it starts without errors

---

## Step 2: Dependency injection

→ Read first: NOTES.md § Dependency injection

Beans depend on other beans. Instead of creating dependencies yourself with `new StripeProvider()`, Spring injects them for you. Constructor injection is the preferred way — it makes dependencies explicit and the class easy to test.

**What you'll build:** Wire the providers into a registry, and the registry into the service.

**Assignments:**
1. Create `ProviderRegistry.java` — annotate with `@Component`. Add a `private final List<PaymentProvider> providers` field.
2. Add a constructor that takes `List<PaymentProvider> providers` as a parameter and assigns it to the field. Spring has a special behaviour here: when it sees a constructor parameter of type `List<SomeInterface>`, it automatically collects every bean that implements that interface and passes them all in as a list. So `StripeProvider` and `AdyenProvider` will both end up in this list without you doing anything extra.
3. Add a `public` method `Optional<PaymentProvider> findByName(String name)` — use a stream on the list to find the first provider whose `getName()` matches the given name. Return the result wrapped in Optional (stream's `findFirst()` does this automatically). It must be `public` because `PaymentServiceTest` lives in a different package and needs to call it.
   *Reuses: Streams, Optional*
4. Create `PaymentService.java` in `service/` — annotate with `@Service`
5. Add a constructor that takes `ProviderRegistry` as a parameter and assigns it to a `private final` field — this is constructor injection
6. Add a `public` method `Transaction processPayment(String providerName, BigDecimal amount, PaymentMethod method)` — it must be `public` because it will later be called from `PaymentController` in a different package. The return type is `Transaction` (not `String`) because Step 5 will update this method to save and return the full transaction record:
   - Call `findByName(providerName)` on the registry — it returns an `Optional<PaymentProvider>`
   - If no provider is found, throw a `ProviderNotFoundException` (create this class — it should extend `RuntimeException` and take the provider name as a constructor parameter, building a message like `"No provider found: Stripe"`)
   - If found, call `charge(amount, method)` on it and return the result
   *Reuses: Optional, Exceptions*

---

## Step 3: Core annotations

→ Read first: NOTES.md § Core annotations

`@Component` registers any class as a bean. But Spring also has semantic annotations that say *what role* the bean plays: `@Service` for business logic, `@Repository` for database access, `@RestController` for HTTP, `@Configuration` for config. Same behaviour under the hood — clearer intent for anyone reading the code.

**What you'll build:** Verify annotations are correct throughout the app and create `AppConfig` with a `RestTemplate` bean.

**Assignments:**
1. Verify `StripeProvider` and `AdyenProvider` are annotated with `@Component` — correct for provider classes since they don't fit `@Service` or `@Repository`
2. Verify `ProviderRegistry` is annotated with `@Component` — correct for a supporting component
3. Verify `PaymentServiceTest` is annotated with `@Service` — correct for a class containing business logic
4. Create `AppConfig.java` in `config/` — annotate the class with `@Configuration`
5. Inside `AppConfig`, add a method annotated with `@Bean` that returns `new RestTemplate()`. The method name can be anything — `restTemplate()` is conventional. `RestTemplate` is a Spring class for making outbound HTTP calls — you can't annotate it with `@Component` since you don't own it, so `@Bean` inside a `@Configuration` class is how you register it.

---

## Step 4: Database setup

Not a learning topic — infrastructure. Sets up PostgreSQL and connects the app to it. Done before JPA so the database is ready when it is needed.

**What gets done:**
1. Install and start PostgreSQL:
   ```
   brew install postgresql@17
   brew services start postgresql@17
   ```
2. Create the database and a dedicated user — run `psql postgres` then:
   ```sql
   CREATE DATABASE payment_service;
   CREATE USER payment_user WITH PASSWORD 'payment_password';
   GRANT ALL PRIVILEGES ON DATABASE payment_service TO payment_user;
   \q
   ```
3. Add the PostgreSQL driver dependency to `pom.xml` inside `<dependencies>`:
   ```xml
   <dependency>
       <groupId>org.postgresql</groupId>
       <artifactId>postgresql</artifactId>
       <scope>runtime</scope>
   </dependency>
   ```
4. Configure the datasource in `application.yaml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/payment_service  # where the database is running and what it's called
       username: payment_user   # the user you created in psql
       password: payment_password  # the password you set
     jpa:
       hibernate:
         ddl-auto: update  # Hibernate reads your @Entity classes and creates/updates the matching tables automatically
   ```
   - `jdbc:postgresql://localhost:5432` — JDBC is how Java connects to databases. `localhost:5432` is where PostgreSQL is running (your machine, default port).
   - `ddl-auto: update` — safe for development. Hibernate creates the table if it doesn't exist, updates it if your entity changes. Never use in production — use `validate` instead, which only checks that the schema matches without changing anything.
5. Verify the app starts and connects without errors

---

## Step 5: Spring Data JPA

→ Read first: NOTES.md § Spring Data JPA

JPA maps Java objects to database tables. You annotate a class with `@Entity` and Spring + Hibernate handle storing it as a row and reading it back. Spring Data JPA gives you a repository interface with `save()`, `findById()`, `findAll()` — no SQL needed for standard operations.

**What you'll build:** Persist payment transactions to the database.

**Assignments:**
1. Create `Transaction.java` — annotate the class with `@Entity` (tells JPA this class maps to a database table). Add these fields with their types:
   - `private Long id` — the database-generated primary key
   - `private String providerName` — which provider handled this transaction
   - `private BigDecimal amount` — the payment amount
   - `private PaymentMethod paymentMethod` — the enum value (CARD or BANK_TRANSFER)
   - `private TransactionStatus status` — the enum value (PENDING, COMPLETED, FAILED)
   - `private LocalDateTime createdAt` — when the transaction was created. Set this automatically by adding a method annotated with `@PrePersist` that sets `this.createdAt = LocalDateTime.now()` — Spring calls this method automatically just before saving to the database.
   - Add a no-argument constructor (JPA requires one — just leave the body empty)
   - Add getters and setters for all fields — these are required so `PaymentServiceTest` can set values when creating a transaction and so JPA can read values back when loading from the database. A getter is a method like `public String getProviderName() { return providerName; }` and a setter is `public void setProviderName(String providerName) { this.providerName = providerName; }`. IntelliJ can generate these for you: right-click inside the class → Generate → Getter and Setter.
2. Annotate the fields:
   - `id` → `@Id` (marks it as the primary key) and `@GeneratedValue(strategy = GenerationType.IDENTITY)` (tells the database to auto-increment it)
   - `paymentMethod` and `status` → `@Enumerated(EnumType.STRING)` on each — this tells JPA to store the enum as its name (e.g. `"CARD"`) rather than its position number in the list
   - All other fields don't need `@Column` unless you want to customize the column name — JPA maps them automatically
3. Create `TransactionRepository.java` — an interface that extends `JpaRepository<Transaction, Long>`. The two type parameters mean: `Transaction` is the entity type, `Long` is the type of its ID. Spring generates the implementation automatically — you write no code inside the interface body.
4. Add a custom query method inside `TransactionRepository`: `List<Transaction> findByTransactionStatus(TransactionStatus status)` — no implementation needed. Spring reads the method name and generates the SQL query. The method name must match the field name on `Transaction` exactly: the field is `transactionStatus`, so the method is `findByTransactionStatus`. If the name doesn't match, Spring will fail to start with a "No property found" error.
5. Add `TransactionRepository` as a constructor parameter in `PaymentServiceTest` and assign it to a `private final` field.
6. Update `processPayment()` in `PaymentServiceTest` — change the return type from `String` to `Transaction`, then update the body to:
   - Create a new `Transaction`, set `providerName`, `amount`, `paymentMethod`, and `status = TransactionStatus.PENDING`
   - Save it with `repository.save(transaction)` — this returns the saved transaction with the generated `id` filled in
   - Call the provider's `charge()` inside a try/catch
   - On success: set `status = TransactionStatus.COMPLETED`, save again, return the transaction
   - On failure (catch RuntimeException): set `status = TransactionStatus.FAILED`, save again, rethrow the exception
   - Note: right now `charge()` just builds a log message and will never throw, so the catch block will never trigger. That is expected — the try/catch structure is put in place now so that when Step 6 replaces `charge()` with a real HTTP call (which can fail), the failure handling is already wired up correctly.
   *Reuses: Enums, Exceptions*

---

## Step 5b: JPA relationships

→ Read first: NOTES.md § JPA relationships

A transaction belongs to a merchant. A merchant has many transactions. This is the most common database relationship pattern and shows up constantly in production codebases. JPA handles the linking automatically — you declare the relationship with annotations and Spring loads related entities for you.

**What you'll build:** Add a `Merchant` entity and link transactions to merchants.

**Package:** `model/`

**Assignments:**
1. Create `Merchant.java` — annotate with `@Entity`. Add these fields:
   - `private Long id` — annotate with `@Id` and `@GeneratedValue(strategy = GenerationType.IDENTITY)`
   - `private String name` — the merchant's name
   - `@OneToMany(mappedBy = "merchant", fetch = FetchType.LAZY) private List<Transaction> transactions` — JPA fills this list automatically when transactions are loaded. `mappedBy = "merchant"` tells JPA that the `merchant` field on `Transaction` owns the relationship (i.e. where the foreign key lives). `FetchType.LAZY` means transactions are only loaded from the database when you actually call `getTransactions()` — not every time you load a merchant.
   - Add a no-argument constructor
   - Add getters and setters for all fields — same reason as `Transaction`: JPA and any code that reads merchant data needs them. IntelliJ can generate these: right-click → Generate → Getter and Setter.
2. Add a `@ManyToOne` relationship to `Transaction`:
   - Add a field `private Merchant merchant`
   - Annotate it with `@ManyToOne` and `@JoinColumn(name = "merchant_id")` — this tells JPA that the `merchant_id` column in the transaction table holds the foreign key linking to the merchant. The `merchant_id` column does not exist yet — adding this annotation is what causes Hibernate to create it automatically in the `transaction` table on next startup (same mechanism as `ddl-auto: update` creating the `transaction` table itself)
3. Create `MerchantRepository.java` — extends `JpaRepository<Merchant, Long>`. Add a custom query method: `Optional<Merchant> findByName(String name)`
4. Run the app — verify it starts and that both a `merchant` table and an updated `transaction` table (with `merchant_id` column) appear in TablePlus

---

## Step 6: REST

→ Read first: NOTES.md § REST

REST endpoints let other systems call your app over HTTP. `@RestController` marks a class as an HTTP handler. `@PostMapping`, `@GetMapping` map HTTP verbs to your methods. `@RequestBody` deserializes the incoming JSON into a Java object. `@PathVariable` pulls a value from the URL path.

You also make outbound calls — calling the provider's API. `RestTemplate` is the standard way to do this in Spring Boot.

**What you'll build:** Expose payment endpoints and add outbound HTTP calls to providers.

**Assignments:**
1. Create `PaymentRequest.java` — a plain Java class (no Spring annotations needed, this is just a data container). Add these fields with their types:
   - `private String providerName`
   - `private BigDecimal amount`
   - `private PaymentMethod paymentMethod`
   Add a no-argument constructor, getters, and setters for each field — Spring uses the no-arg constructor to create the object, then calls setters to fill in the values from the incoming JSON. Without setters, the fields stay null. IntelliJ can generate these: right-click → Generate → Getter and Setter.
2. Create `PaymentController.java` — annotate the class with `@RestController` (marks it as an HTTP handler that returns data, not a view) and `@RequestMapping("/payments")` (all endpoints in this class start with `/payments`). Add only `PaymentServiceTest` as a constructor parameter — the controller never talks to the repository directly. All data access goes through the service. This keeps the controller's job limited to receiving HTTP requests and returning responses.
3. **POST /payments — process a payment**
   - Method name: `processPayment`
   - Annotation: `@PostMapping` — Spring maps this method to `POST /payments`
   - Return type: `Transaction` — Spring converts it to JSON and sends it back with a 200 status code
   - Parameter: `@RequestBody PaymentRequest request` — Spring reads the incoming JSON body and fills in the object automatically before your method runs
   - Body: call `paymentServiceTest.processPayment()` passing `request.getProviderName()`, `request.getAmount()`, and `request.getPaymentMethod()` — return the result

4. **GET /payments/{id} — look up a transaction by ID**

   **In `PaymentController`:**
   - Method name: `findTransactionById`
   - Annotation: `@GetMapping("/{id}")` — Spring maps this to `GET /payments/42` (or any ID)
   - Return type: `Transaction` — Spring converts it to JSON and sends it back
   - Parameter: `@PathVariable Long id` — Spring reads the number from the URL path and passes it in as a `Long`
   - Body: call `paymentServiceTest.findTransactionById(id)` and return the result

   **In `PaymentServiceTest`:**
   - Method name: `findTransactionById`
   - Return type: `Transaction`
   - Parameter: `Long id`
   - Body: call `transactionRepository.findById(id)` — this returns `Optional<Transaction>`. Call `.orElseThrow(() -> new TransactionNotFoundException(id))` on it — this either unwraps and returns the transaction, or throws the exception if not found

   **New class — `TransactionNotFoundException` in `exception/`:**
   - Extends `RuntimeException`
   - Constructor takes `Long id`
   - Pass `"Transaction not found: " + id` to `super()`
   - Note: the global exception handler added in Step 9 will catch this and return a 404 response to the caller. For now the app will return a 500 if thrown — that is expected and will be fixed in Step 9.

   *Reuses: Optional*

5. **GET /payments?status=COMPLETED — get all transactions with a given status**

   **In `PaymentController`:**
   - Method name: `getTransactionsByStatus`
   - Annotation: `@GetMapping` (no path — the `?status=` query parameter distinguishes it from other GET endpoints)
   - Return type: `List<Transaction>` — Spring converts the list to a JSON array
   - Parameter: `@RequestParam TransactionStatus status` — Spring reads `?status=COMPLETED` from the URL and converts the string `"COMPLETED"` to the enum value `TransactionStatus.COMPLETED` automatically
   - Body: call `paymentServiceTest.findTransactionsByStatus(status)` and return the result

   **In `PaymentServiceTest`:**
   - Method name: `findTransactionsByStatus`
   - Return type: `List<Transaction>`
   - Parameter: `TransactionStatus status`
   - Body: call `transactionRepository.findByTransactionStatus(status)` and return the result directly

   *Reuses: Enums*

6. **Add `RestTemplate` to providers**

   **In `StripeProvider`:**
   - Add `RestTemplate` as a constructor parameter — assign it to a `private final RestTemplate restTemplate` field
   - Spring will inject the `RestTemplate` bean you defined in `AppConfig` automatically

   **In `AdyenProvider`:**
   - Same as above

7. **Update `charge()` in both providers**

   **In `StripeProvider.charge()`:**
   - Replace the `buildLogMessage()` call with `return restTemplate.postForObject(apiUrl, amount, String.class)`
   - `postForObject` sends a POST request to `apiUrl` with `amount` as the request body, and returns the response body as a `String`
   - Since `apiUrl` is a placeholder (not a real server), this call will throw a `ResourceAccessException` (a subclass of `RuntimeException`) at runtime — that is expected. The try/catch in `PaymentService.processPayment()` will catch it and mark the transaction as `FAILED`. This will be fixed in Step 9.

   **In `AdyenProvider.charge()`:**
   - Same as above

---

## Step 7: JSON & XML serialization

→ Read first: NOTES.md § JSON / XML serialization

Spring automatically converts your Java objects to JSON when you return them from a `@RestController` — Jackson handles this. But providers don't always return clean JSON. You need to control field names with `@JsonProperty`, ignore unexpected fields with `@JsonIgnoreProperties`, and sometimes parse XML (some providers, including older Adyen flows, use XML).

In this step you also introduce request and response classes per provider — each provider gets its own classes for building what it sends and parsing what it receives back. These live in the provider's own sub-package. This is the same pattern used in real payment systems.

**What you'll build:** Control serialization of requests and responses, and parse realistic provider response formats.

**Package structure after this step:**
- `provider/stripe/` — `StripeProvider`, `StripeChargeResponse`
- `provider/adyen/` — `AdyenProvider`, `AdyenChargeResponse`

**Assignments:**
1. **Skipped** — `PaymentRequest` uses camelCase which Jackson maps automatically. No `@JsonProperty` needed. The concept is documented in NOTES.md § JSON & XML serialization.

2. **New class — `StripeChargeResponse` in `provider/stripe/`**
   - Plain Java class — no Spring annotations
   - Annotate the class with `@JsonIgnoreProperties(ignoreUnknown = true)` — Stripe's real API returns many fields you don't need. Without this, Jackson throws an error when it encounters an unknown field. With it, Jackson silently ignores anything not declared in your class.
   - Fields: `private String pspTransactionId`, `private String status`, `private BigDecimal amount`
   - `pspTransactionId` is Stripe's reference for the charge on their side — needed for refunds and for linking your transaction record to Stripe's. Add `@JsonProperty("id")` above it — but note: verify the exact field name in Stripe's real API response when you build the real integration. If Stripe uses a different key, update the annotation.
   - Add getters for all fields — needed in assignment 3 to read the values and build the response string

3. **Update `StripeProvider.charge()`**
   - Method signature stays the same: `public String charge(BigDecimal amount, PaymentMethod method)`
   - Add `ObjectMapper` as a new constructor parameter — assign it to a `private final ObjectMapper objectMapper` field. Spring Boot automatically creates an `ObjectMapper` bean, so Spring will inject it.
   - Body — send a POST to the provider URL, parse the JSON response into a `StripeChargeResponse` using `objectMapper`, and return a formatted string built from the response object's getters

4. **New class — `AdyenChargeResponse` in `provider/adyen/`**
   - Same structure as `StripeChargeResponse` (fields: `private String pspTransactionId`, `private String status`, `private BigDecimal amount`, getters for all)
   - `pspTransactionId` is Adyen's reference for the transaction on their side — same purpose as in `StripeChargeResponse`. Add `@JsonProperty` with the correct Adyen field name once you know it from their real API response.
   - Annotate the class with `@JacksonXmlRootElement(localName = "response")` instead of `@JsonIgnoreProperties` — this tells the XML parser what the root XML tag is called in Adyen's response. Without it, the XML parser doesn't know where to start reading.
   - Add getters for all fields — needed in assignment 5

5. **Update `AdyenProvider.charge()` and add XML dependency**
   - First, add this dependency to `pom.xml` inside `<dependencies>` — without it the `XmlMapper` class does not exist in the project:
     ```xml
     <dependency>
         <groupId>tools.jackson.dataformat</groupId>
         <artifactId>jackson-dataformat-xml</artifactId>
     </dependency>
     ```
   - Method signature stays the same: `public String charge(BigDecimal amount, PaymentMethod method)`
   - Body — send a POST to the provider URL, parse the XML response into an `AdyenChargeResponse` using `XmlMapper`, and return a formatted string built from the response object's getters

---

## Step 8: Profiles & config

→ Read first: NOTES.md § Profiles & config

You don't want the same config in development and production. Profiles let you have separate `application-dev.yaml` and `application-prod.yml` files. Sensitive values like API keys and database passwords never go in config files — they come from environment variables. `@ConfigurationProperties` maps config values from yml into a typed Java object.

**What you'll build:** Separate dev and prod config, and move all provider settings into a config object.

**Assignments:**
1. Create `application-dev.yaml` — API keys come from environment variables, not hardcoded values. `${VARIABLE_NAME}` is Spring's syntax for reading an environment variable — set them in your shell or IntelliJ run config before starting the app:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/payment_service
     jpa:
       hibernate:
         ddl-auto: update
   providers:
     stripe:
       base-url: https://api.stripe.com
       api-key: ${STRIPE_API_KEY}
     adyen:
       base-url: https://api.adyen.com
       api-key: ${ADYEN_API_KEY}
   ```
2. **Skipped** — no `application-prod.yml` needed. This app only runs in dev. The env var pattern from assignment 1 covers the same learning goal.
3. **New class — `ProviderProperties` in `config/`**
   - Annotate with `@ConfigurationProperties(prefix = "providers")` — this tells Spring to read the `providers:` block from the active yml file and map its values into this class automatically. Without this annotation, the class is just a plain object and Spring does not populate its fields.
   - Annotate with `@Component` so Spring creates it as a bean and can inject it into providers
   - Add two nested static classes inside `ProviderProperties`:
     - `public static class Stripe` with fields `private String baseUrl` and `private String apiKey` — add getters and setters for both. Setters are required — `@ConfigurationProperties` uses them to populate the fields from yml at startup. Without setters the fields stay null and the app fails to start.
     - `public static class Adyen` with fields `private String baseUrl` and `private String apiKey` — add getters and setters for both
   - Add fields on `ProviderProperties`: `private Stripe stripe = new Stripe()` and `private Adyen adyen = new Adyen()` — add getters for both. Spring maps the yml structure to these fields by matching names: `providers.stripe.base-url` → `stripe.baseUrl`.

4. **No action needed** — the yml from assignment 1 already uses the `providers` prefix that `ProviderProperties` reads from.

5. **Wire `ProviderProperties` into providers**

   **In `StripeProvider`:**
   - Add `ProviderProperties` as a constructor parameter — assign it to a `private final ProviderProperties properties` field
   - In the constructor body, replace `this.apiUrl = "https://api.stripe.com"` with `this.apiUrl = properties.getStripe().getBaseUrl()` — the value now comes from `application-dev.yaml` instead of being hardcoded in code
   - Add a `@PostConstruct` method that logs the loaded base URL and a masked version of the API key (e.g. first 8 characters + `***`) — this confirms the env vars were read correctly at startup. Read `NOTES.md § @PostConstruct` first.

   **In `AdyenProvider`:**
   - Same as above — use `properties.getAdyen().getBaseUrl()`

6. **Set the environment variables and verify**
   - Read `NOTES.md § Profiles & config` under "How to activate a profile and set environment variables in IntelliJ" — follow the steps there to set the active profile to `dev` and add `STRIPE_API_KEY` and `ADYEN_API_KEY` with placeholder values in the Environment variables field
   - Run the app — check the console for the `@PostConstruct` log output confirming the masked key was printed
   - Remove the `@PostConstruct` log from `AdyenProvider` after verifying — keep it in `StripeProvider` until the real Stripe integration is built

---

## Step 9: Spring Retry & error handling

→ Read first: NOTES.md § Spring Retry & error handling

External APIs fail. They return 503s, time out, or return unexpected responses. Spring Retry lets you automatically retry a failed call a set number of times before giving up. `@ControllerAdvice` lets you handle exceptions globally — one place to decide what HTTP response an exception produces, instead of repeating try/catch in every controller method.

**What you'll build:** Make provider calls resilient and add a global error handler.

**Assignments:**
1. Add these two dependencies to `pom.xml` inside `<dependencies>`:
   ```xml
   <dependency>
       <groupId>org.springframework.retry</groupId>
       <artifactId>spring-retry</artifactId>
   </dependency>
   <dependency>
       <groupId>org.springframework</groupId>
       <artifactId>spring-aspects</artifactId>
   </dependency>
   ```
2. **Enable retry in `PaymentServiceApplication`**
   - Add `@EnableRetry` to the class — this activates Spring Retry globally so the `@Retryable` annotation on provider methods works. Without this, `@Retryable` is silently ignored and no retries happen.

3. **Add retry to `StripeProvider.charge()`**
   - Add `@Retryable(retryFor = RuntimeException.class, maxAttempts = 3)` above `charge()` — when `charge()` throws a `RuntimeException`, Spring automatically calls it again. It tries up to 3 times total before giving up and calling the `@Recover` method.

4. **Add a recover method to `StripeProvider`**
   - Method name: `recover`
   - Annotation: `@Recover` — Spring calls this method automatically when all retries are exhausted. Without this method, Spring rethrows the last exception after the retries run out.
   - Return type: `String` — must match `charge()`'s return type exactly, otherwise Spring cannot match them
   - Parameters: `RuntimeException e` first (the exception that caused the failure), then `BigDecimal amount` and `PaymentMethod method` — must match `charge()`'s parameters in the same order
   - Body: return a message describing the failure, e.g. `"Stripe charge failed after retries: " + e.getMessage()`

5. **No changes to `PaymentServiceTest`** — the try/catch from Step 5 already sets the transaction to `FAILED` and rethrows. That is still correct.

6. **New class — `GlobalExceptionHandler` in `exception/`**
   - Annotate the class with `@ControllerAdvice` — this tells Spring that this class handles exceptions thrown from any controller in the app. When an exception escapes a controller method without being caught, Spring looks in this class for a matching `@ExceptionHandler` method and calls it to build the HTTP response instead of returning a 500.
   - No constructor needed — Spring creates it as a bean automatically

7. **Add handler for `ProviderNotFoundException`**

   **In `GlobalExceptionHandler`:**
   - Method name: `handleProviderNotFound`
   - Annotation: `@ExceptionHandler(ProviderNotFoundException.class)` — tells Spring to call this method when a `ProviderNotFoundException` is thrown anywhere in the app
   - Return type: `ResponseEntity<String>` — Spring's class for building an HTTP response with a specific status code and a body. `<String>` means the response body is a plain string.
   - Parameter: `ProviderNotFoundException e` — Spring passes the thrown exception in automatically
   - Body: return `ResponseEntity.status(404).body(e.getMessage())` — this sends a 404 response to the caller with the exception message as the body (e.g. `"No provider found: Stripe"`)

8. **Add handler for `TransactionNotFoundException`**

   **In `GlobalExceptionHandler`:**
   - Method name: `handleTransactionNotFound`
   - Annotation: `@ExceptionHandler(TransactionNotFoundException.class)`
   - Return type: `ResponseEntity<String>`
   - Parameter: `TransactionNotFoundException e`
   - Body: return `ResponseEntity.status(404).body(e.getMessage())` — sends a 404 with the message (e.g. `"Transaction not found: 42"`)

   *Reuses: Exceptions*

---

## Step 10: Testing

→ Read first: NOTES.md § Testing

Testing is not just about proving code works — it's about understanding **what to test, why, and at what level**. A test that tests the wrong thing gives you false confidence. This step teaches you how to think about testing before writing a single line of test code.

**The two levels of testing you'll write:**

- **Unit tests** — test one class in isolation. Dependencies (like the repository or provider) are replaced with fakes called mocks. Fast, no database needed. Use when you want to verify logic inside a single class — e.g. does `processPayment` set the status to `FAILED` when the provider throws?
- **Integration tests** — test multiple layers together, hitting a real database. Slower, but prove that the layers actually work together. Use when you want to verify that a full request flows correctly end to end — e.g. does `POST /payments` save a transaction and return it as JSON?

**What NOT to test:**
- Don't test Spring's own behaviour — Spring is already tested by its authors. Don't write a test that proves `@GetMapping` routes correctly — that's testing the framework, not your code.
- Don't test getters and setters — they contain no logic.
- Don't test things that can't fail — if a method just returns a hardcoded value, there's nothing to verify.

**What to test:**
- Business logic in the service layer — status transitions, exception throwing, routing to the right provider
- Controller responses — correct status codes, correct JSON shape
- Repository custom queries — does `findByTransactionStatus` actually return only transactions with that status?

**Assignments:**

1. **Read the notes for this step first** — the notes explain unit tests vs integration tests, what mocking is and why it exists, and what makes a good test. Read before writing anything.

2. **Unit test — `PaymentService.processPayment()` success case**

   Create the package `com.example.payment_service.service` under `src/test/java/` and create `PaymentServiceTest.java` inside it.

   - Annotate the class with `@ExtendWith(MockitoExtension.class)` — activates Mockito so `@Mock` and `@InjectMocks` work
   - Add three mock fields (no access modifier needed): `ProviderRegistry`, `TransactionRepository`, and `PaymentProvider` — each annotated with `@Mock`
   - Add a `PaymentServiceTest` field annotated with `@InjectMocks` — this is the real class being tested; Mockito injects the mocks above into its constructor automatically
   - Add a test method named `processPayment_whenChargeSucceeds_returnsCompletedTransaction`, annotated with `@Test`
   - In the test body: configure the mocks using `when(...).thenReturn(...)` — see NOTES.md § Mocking under "Controlling what mocks return" for the syntax. For `save()`, use `thenAnswer` instead of `thenReturn` so it returns whatever transaction was passed in.
   - Call `paymentServiceTest.processPayment("Stripe", new BigDecimal("100"), PaymentMethod.CARD)` and assign the result to a `Transaction`
   - Assert that the returned transaction's status is `COMPLETED` using `assertEquals`
   - Assert that `transactionRepository.save()` was called exactly twice (once for PENDING, once for COMPLETED) using `verify`

3. **Unit test — `PaymentService.processPayment()` failure case**

   Add a second test method to `PaymentServiceTest` named `processPayment_whenChargeThrows_returnsFailedTransactionAndRethrows`, annotated with `@Test`

   - Configure the mocks the same way as assignment 2, except make `paymentProvider.charge()` throw a `RuntimeException` instead of returning a value — use `thenThrow` instead of `thenReturn`
   - Use `assertThrows` to verify that calling `processPayment` throws a `RuntimeException`
   - Use `verify` to assert that `transactionRepository.save()` was still called twice — the FAILED save must still happen even when the charge throws

4. **Unit test — `PaymentService.findTransactionById()` not found case**

   Add a third test method to `PaymentServiceTest` named `findTransactionById_whenNotFound_throwsTransactionNotFoundException`, annotated with `@Test`

   - Configure `transactionRepository.findById()` to return `Optional.empty()` — use `anyLong()` as the argument matcher so it matches any ID value
   - Use `assertThrows(TransactionNotFoundException.class, ...)` to verify that calling `paymentService.findTransactionById(99L)` throws `TransactionNotFoundException`

5. **Integration test — `POST /payments`**

   Create the package `com.example.payment_service.controller` under `src/test/java/` and create `PaymentControllerTest.java` inside it.

   - Annotate the class with `@SpringBootTest` — starts the full Spring context including the database (PostgreSQL must be running)
   - Annotate the class with `@AutoConfigureMockMvc` — tells Spring to create a `MockMvc` bean you can inject
   - Add a `@BeforeEach` method that calls `transactionRepository.deleteAll()` — clears the database before each test so tests don't interfere with each other
   - Add a test method named `postPayment_validRequest_savesTransactionToDatabase`, annotated with `@Test`
   - Send a `POST /payments` request with a JSON body containing `providerName`, `amount`, and `paymentMethod` using `mockMvc.perform(...)`
   - Assert the response status is 200 using `.andExpect(status().isOk())`
   - Assert the response JSON contains a `status` field using `.andExpect(jsonPath("$.status").exists())`
   - After the request, call `transactionRepository.findAll()` and assert the list contains exactly one transaction

6. **Integration test — `GET /payments/{id}` not found**

   Add a second test method to `PaymentControllerTest` named `getPaymentById_whenNotFound_returns404`, annotated with `@Test`

   - Send a `GET /payments/99999` request using `mockMvc.perform(...)`
   - Assert the response status is 404 using `.andExpect(status().isNotFound())` — the `GlobalExceptionHandler` you built in Step 9 maps `TransactionNotFoundException` to a 404

---

## Java fundamentals used in this project

A reference for which topics from the fundamentals show up and where.

| Fundamental | Where it appears |
|---|---|
| Enums | `TransactionStatus`, `PaymentMethod` — Step 1 |
| Interfaces | `PaymentProvider` interface — Step 1 |
| Abstract classes | `AbstractPaymentProvider` — Step 1 |
| Inheritance | `StripeProvider`, `AdyenProvider` extend abstract class — Step 1 |
| Generics | `JpaRepository<Transaction, Long>`, `Optional<PaymentProvider>` — Steps 2, 5 |
| Optional | `findByName()`, `findById()` — Steps 2, 6 |
| Streams & Lambdas | Filtering providers and transactions — Steps 2, 6 |
| Collections | `List<PaymentProvider>` in registry — Step 2 |
| Exception handling | `PaymentFailedException`, global handler — Steps 2, 9 |
| Regular expressions | Can be added to routing rules in `ProviderRegistry` — Step 2 extension |
