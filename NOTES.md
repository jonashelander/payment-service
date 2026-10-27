# Notes

---

## Contents

[Concepts & terminology](#concepts--terminology)

**Foundations** 
[Variables & types](#variables--types) · [Access modifiers](#access-modifiers) · [static](#static) · [Classes & OOP](#classes--oop) · [Nested classes](#nested-classes) · [Records](#records) · [Memory model](#memory-model)

**Java fundamentals**
[Streams](#streams) · [Lambdas & functional interfaces](#lambdas--functional-interfaces) · [Optional](#optional) · [Collections](#collections) · [Exception handling](#exception-handling) · [Inheritance & polymorphism](#inheritance--polymorphism) · [Interfaces & abstract classes](#interfaces--abstract-classes) · [Generics](#generics) · [Enums](#enums) · [Regular expressions](#regular-expressions)

**Spring Boot**
[Beans & the application context](#beans--the-application-context) · [Dependency injection](#dependency-injection) · [Core annotations](#core-annotations) · [Spring Data JPA](#spring-data-jpa) · [REST](#rest) · [JSON & XML serialization](#json--xml-serialization) · [Profiles & config](#profiles--config) · [@PostConstruct](#postconstruct--running-code-after-spring-has-finished-wiring-a-bean) · [Spring Retry & error handling](#spring-retry--error-handling)

**Testing**
[Unit testing](#unit-testing) · [Mocking](#mocking) · [Integration testing](#integration-testing) · [Mocked HTTP servers](#mocked-http-servers)

**Payment service — flow reference**
[Component map](#component-map) · [POST success](#post-payments--success) · [POST failure + retry](#post-payments--failure--retry) · [GET by ID](#get-paymentsid) · [GET by status](#get-paymentsstatus) · [App startup](#app-startup--config-loading)

**Webhooks & async patterns**
[Callbacks & webhooks](#callbacks--webhooks) · [Request/response vs event-driven](#requestresponse-vs-event-driven)

**Cryptography basics**
[HMAC signatures](#hmac-signatures) · [AES encryption & RSA](#aes-encryption--rsa)

**Build & tooling**
[Git](#git) · [Maven](#maven) · [Docker](#docker) · [CI/CD](#cicd) · [Database migrations](#database-migrations)

**Observability**
[Logging](#logging)

---

## Concepts & terminology

Terms that come up constantly. Read this first so nothing in the notes catches you off guard.

---

**OOP — Object-Oriented Programming**
A way of structuring code around objects — things that have data (fields) and behaviour (methods). Instead of writing a list of instructions top to bottom, you model the real world: a `Payment` has an amount, a status, a provider. A `User` has a name and an email. You then write methods on those objects to do things with them. Java is built around OOP — almost everything you write will be a class.

---

**API**
The word gets used in multiple ways and that causes confusion:
- **A web API** — a URL you send HTTP requests to, e.g. `POST https://api.stripe.com/charge`. This is what most people mean in a payment/provider context.
- **A Java API** — the set of public methods on a class or library that you're allowed to call. When the docs say "use the Collections API", they mean the methods Java gives you on List, Map, Set etc.
- **Your own API** — the endpoints your Spring Boot app exposes so other systems can call you.

Same word, three different things. Context tells you which one.

---

**Interface (the concept)**
A contract. It says: "anything that implements me must have these methods." It doesn't say how — just what. A `PaymentProvider` interface might say every provider must have a `charge()` method. Stripe implements it one way, Adyen another. The rest of your code calls `provider.charge()` without caring which provider it is. This is how Spring wires things together — you depend on the interface, Spring decides which implementation to inject at runtime.

---

**Abstraction**
Hiding the details so you only see what matters. When you call `list.add("Stripe")` you don't know or care how ArrayList stores it in memory — you just know it works. That's abstraction. In your own code: a `PaymentServiceTest` abstracts the details of which provider to call and how — the controller just calls `paymentServiceTest.processPayment()` and gets a result back.

---

**Runtime vs compile time**
- **Compile time** — when Java turns your source code into bytecode. Errors here are caught before the program runs — wrong types, missing methods, syntax mistakes. The compiler refuses to build.
- **Runtime** — when the program is actually running. Errors here happen while the program executes — a NullPointerException, a provider returning 503, a missing config value. The program builds fine but crashes or misbehaves when used.

Rule of thumb: compile-time errors are easier to fix because the compiler tells you exactly where. Runtime errors require investigation.

---

**Framework vs library**
- **Library** — code you call. You're in control. You decide when to use it. Jackson is a library — you call `objectMapper.writeValueAsString(obj)` when you want to.
- **Framework** — code that calls you. The framework is in control. You write code that fits its structure and the framework runs it. Spring Boot is a framework — it starts the app, handles HTTP requests, manages beans, and calls your code at the right time.

---

**Dependency**
Something your code needs in order to work. A `PaymentServiceTest` depends on a `ProviderClient` to make API calls. In Spring Boot, dependencies are injected — Spring creates the objects and passes them in, so you don't have to wire them together manually.

---

**Boilerplate**
Code you have to write repeatedly that adds no real logic — just ceremony. Getters, setters, constructors that do nothing but assign fields. Records exist specifically to eliminate boilerplate for data classes.

---

## Foundations

*Topics covered before the fast track. Not in the fast track but useful reference.*

---

### Variables & types

**Production scenario:** A payment object has an `amount` field. You use `BigDecimal` not `double` because you're dealing with money. The `status` is a `boolean isPremium`. The transaction ID is a `long` because it comes from a database sequence that exceeds `int` range. These type choices matter — using the wrong one causes precision bugs or runtime crashes.

**Primitives** — stored directly on the stack, cannot be null, fast and lightweight:

| Type | Use when |
|---|---|
| `int` | Default for whole numbers — counters, IDs, indexes |
| `long` | Whole numbers too large for `int` — timestamps (milliseconds since epoch), large transaction IDs |
| `double` | Default for decimals — but never for money (use `BigDecimal` instead) |
| `float` | Rarely used — only when a library specifically requires it |
| `boolean` | True/false flags — `isActive`, `isPremium`, `hasError` |
| `char` | Single character — rarely used directly, mostly handled via `String` |
| `byte` / `short` | Almost never used directly — only in low-level or performance-critical code |

**Literal suffixes** — Java assumes whole numbers are `int` and decimals are `double`. Use suffixes to override:
- `L` → `long` (e.g. `10000000000L`) — required if the number is too large for `int`
- `F` → `float` (e.g. `3.14F`) — required because `3.14` defaults to `double`
- `0xFF` → hex literal, `0b1010` → binary literal

**Wrapper objects** — object versions of primitives: `Integer`, `Long`, `Double`, `Boolean`, `Character` etc.
- Live on the heap, the variable holds a reference
- Can be `null` — useful when absence of a value matters
- Have utility methods: `Integer.parseInt("42")`, `Integer.MAX_VALUE` etc.
- Required when Java expects an object — e.g. collections (`List<Integer>`, not `List<int>`)
- Use `int` by default, reach for `Integer` when you need null, collections, or object methods

**In Spring Boot** — use wrapper types (`Integer`, `Boolean`) for `@RequestParam` and similar, because parameters can be missing (null). `int` can't be null so Spring would throw an exception if the parameter is absent.

**BigDecimal — use for money, never double or float**
- `double` and `float` have floating point precision issues that show up during arithmetic: `0.1 + 0.2 = 0.30000000000000004`
- `BigDecimal` stores numbers exactly with no rounding surprises
- Always pass the value as a String to the constructor: `new BigDecimal("19.99")` — passing a double would reintroduce the precision issue

**BigInteger** — like `BigDecimal` but for whole numbers with no size limit. Use when `long` isn't big enough (e.g. cryptography, very large IDs). Rare in practice.

**Type casting** — converting a value from one type to another:
- **Widening** (smaller → larger, e.g. `int` → `long`) — happens automatically, no cast needed
- **Narrowing** (larger → smaller, e.g. `long` → `int`) — requires explicit cast, risk of losing data if value doesn't fit:
```java
long l = 100L;
int i = (int) l; // explicit cast required
```
- Works between all numeric types (`byte`, `short`, `int`, `long`, `float`, `double`)
- Can't cast unrelated types — to convert a `String` to `int` use `Integer.parseInt("42")` instead

**Operators and type rules**

- `+` with a `String` always produces a `String` — Java automatically converts the other operand. `String` is always the "master": `"charged: " + 50.0` → `"charged: 50.0"`.
- `-`, `*`, `/`, `%` are purely arithmetic — they never work with `String`. Using them with a String is a compile error.
- In arithmetic without Strings, the higher-precision type wins: `int + double` → `double`, `int + long` → `long`. This is called type promotion and happens automatically (widening).
- This is the only special operator behavior in Java. Unlike JavaScript, you can't do `"5" - 2` — Java is strict and the compiler rejects it.

**Autoboxing & unboxing** — Java automatically converts between primitive and wrapper. You don't need to think about it most of the time, but knowing it happens helps you understand unexpected `NullPointerException`s — unboxing a `null Integer` to `int` will crash:
- Autoboxing: `Integer x = 5;` (int → Integer, done automatically)
- Unboxing: `int y = x;` (Integer → int, done automatically)
- Watch out: `Integer x = null; int y = x;` — crashes at runtime because you can't unbox null

**`var`** — lets Java infer the type instead of writing it out explicitly:
- `var grouped = names.stream().collect(Collectors.groupingBy(...));` — Java figures out the type from the right side
- Useful when the full type is long or obvious from context
- Can reduce readability if the type isn't clear from the right side — a teammate has to know what the method returns to know what `var` holds
- Some teams avoid it for this reason; use it when the type is obvious, be explicit when it's not

---

### Access modifiers

Controls who can see a field, method, or class.

**Production scenario:** A `PaymentServiceTest` class has a `processPayment()` method that's `public` — it's called from a controller. Internally it calls `validateAmount()` which is `private` — no other class should call this directly. The `amount` field on the `Payment` object is `private` and only accessible via a getter. This keeps the internal logic hidden and lets you change it without breaking other parts of the codebase.

| Modifier | Same class | Same package | Subclass | Everywhere |
|---|---|---|---|---|
| `private` | yes | no | no | no |
| package-private (no keyword) | yes | yes | no | no |
| `protected` | yes | yes | yes | no |
| `public` | yes | yes | yes | yes |

- **`private`** — only the class itself can access it. Use for internal fields you want to hide from everything else. Most fields should be private (encapsulation).
- **package-private** — no keyword needed, just omit the modifier. Accessible within the same package but invisible outside it. Rarely used intentionally.
- **`protected`** — like package-private but also extends to subclasses. Use when a subclass needs direct access to a parent's field or method.
- **`public`** — accessible from anywhere. Use for methods and classes that form the public API of your code.

Example: `Animal`'s fields are `private`, so `Dog` cannot access `size`, `breed`, or `weight` directly even though it is a subclass — it can only set them via `super(...)`. Changing them to `protected` would allow `Dog` to access them directly.

---

### static

**Production scenario:** A `PaymentServiceTest` has a static constant `MAX_RETRY_ATTEMPTS = 3` — it's the same value for all payment attempts, no need for an instance. A utility class `CurrencyUtils` has a static method `convertToMinorUnits(amount)` — it doesn't need any object state, it just takes a number and returns one. You call it directly: `CurrencyUtils.convertToMinorUnits(19.99)`.

- A **non-static** field or method belongs to the instance — each object gets its own copy
- A **static** field or method belongs to the class itself — shared across all instances
- You can't call a non-static field or method directly on the class, you need to create an instance first
- You can call static from non-static code — just use the class name directly: `Dog.staticField` — this is fine and common
- Don't call static through an instance reference (`d.staticField`) — IntelliJ will warn you. It works, but it's misleading because the reader might think the field belongs to that specific instance, and that changing it only affects that instance. It doesn't — it affects all of them.
- Static methods can't use `this` — `this` refers to a specific instance, and static has no instance

---

### Classes & OOP

**Production scenario:** You have a `BasePaymentProvider` class with shared logic — logging, error handling, retry logic. `StripeProvider` and `AdyenProvider` both extend it and override the `processPayment()` method with their own provider-specific implementation. When a payment comes in, the code calls `provider.processPayment()` without caring whether it's Stripe or Adyen — polymorphism picks the right implementation at runtime.

- Inheritance: a subclass (`Dog extends Animal`) inherits fields and methods from the parent class
- `super(...)` calls the parent constructor to initialize inherited fields
- Method overriding: a subclass can replace a parent method with its own version
- `@Override` annotation tells the compiler you intend to override — catches typos in method names at compile time
- Polymorphism: declaring `Animal dog = new Dog()` means Java uses the actual object type (`Dog`) at runtime to decide which method to call, even though the declared type is `Animal`
- Declaring as `Animal` limits you to `Animal`'s methods — useful when you want to write code that works for any animal regardless of specific type

---

### Nested classes

A nested class is a class defined inside another class. You use it when a class is only relevant in the context of one other class and has no reason to exist independently.

**Production scenario:** `ProviderProperties` holds config for all providers. Each provider's config is its own small group of fields — `baseUrl`, `apiKey`. Rather than creating separate top-level files `StripeConfig.java` and `AdyenConfig.java` that only ever get used by `ProviderProperties`, you define them as nested classes inside it. They're logically grouped, and anyone reading the code immediately understands they belong to `ProviderProperties`.

**Static nested class — the most common form**

`static` means the nested class does not need an instance of the outer class to exist. It's just a regular class that happens to live inside another one for organisation:

```java
public class ProviderProperties {

    private Stripe stripe = new Stripe();
    private Adyen adyen = new Adyen();

    public static class Stripe {
        private String baseUrl;
        private String apiKey;

        public String getBaseUrl() { return baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }

    public static class Adyen {
        private String baseUrl;
        private String apiKey;

        public String getBaseUrl() { return baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }

    public Stripe getStripe() { return stripe; }
    public Adyen getAdyen() { return adyen; }
}
```

From outside `ProviderProperties` you reference the nested class as `ProviderProperties.Stripe`. Inside `ProviderProperties` itself you just use `Stripe` directly.

**Without `static`** — the nested class would need an instance of the outer class to exist. Almost never what you want. Always use `static` for nested classes unless you have a specific reason not to.

**When to use a nested class vs a separate file:**
- Nested: the class is only ever used by one other class and has no standalone meaning
- Separate file: the class is used by multiple classes, or it's large enough to warrant its own file

---

### Records

A `record` is a shorthand class for holding data. Java automatically generates the constructor, getters, `equals`, `hashCode`, and `toString` — you don't write any of it yourself.

```java
record User(int id, String name, String email, boolean isPremium) {}
```

Is equivalent to a full class with those four fields and all the boilerplate written out manually. You access fields as methods:

```java
user.name()
user.email()
```

You access fields as method calls with parentheses — not direct field access:

```java
user.name()    // correct — calling the generated getter method
user.name      // works but wrong — accessing the field directly, not the intended API
```

In a normal class the getter would be `getName()`. In a record it's just `name()`.

Used in exercises as a convenient data container. Not in the fast track — covered in detail in section 1 (Shaping data).

---

### Memory model

**Stack** — short-lived memory for method execution:
- Each method call gets a "frame" on the stack holding its local variables and primitives
- Fast to allocate and free — the frame is thrown away automatically when the method returns
- Primitives (`int`, `double`, `boolean`, etc.) live here directly

**Heap** — long-lived shared memory for objects:
- All objects (`String`, `ArrayList`, `Double`, etc.) live here
- Stick around as long as something holds a reference to them
- Cleaned up by the garbage collector when nothing points to them anymore

When you declare an object variable, the variable itself (the reference/pointer) lives on the stack, but the object it points to lives on the heap:

```java
int x = 5;          // x lives on the stack
String s = "hello"; // s (the reference) lives on the stack,
                    // but the String object lives on the heap
double d = 4.8;     // primitive — stack
Double d2 = 4.8;    // reference on stack → Double object on heap
```

---

## Java fundamentals

---

### Streams

A stream is a pipeline of operations on a collection — you don't modify the original list, you process it and get a result. All streams start with `.stream()` on a collection.

**Production scenario:** You fetch a list of transactions from the database. You need to return only the failed ones, with their amounts converted from pence to pounds, sorted newest first. Without streams you'd write multiple loops. With streams it's one readable pipeline: `filter` for failed → `map` to convert amount → `sorted` by date.

Streams never modify the original collection — they produce a new result. You can call `.stream()` on the same list multiple times safely.

**Stream operations quick reference:**

| Method | When to use |
|---|---|
| `filter` | Get a subset — only failed transactions, only premium users, only payments above a threshold |
| `map` | Transform data — extract emails from users, convert amounts, map objects to DTOs |
| `forEach` | Act on each element without returning anything — log, print, send notification |
| `sorted` | Order results — newest first, alphabetically, highest amount first |
| `count` | Get a number — how many transactions failed, how many users are premium |
| `anyMatch` | Yes/no across the collection — did any transaction fail? is there at least one admin? |
| `allMatch` | Yes/no across the collection — are all payments confirmed? |
| `noneMatch` | Yes/no across the collection — are there any duplicates? any blocked users? |
| `findFirst` | Get one result — first failed transaction to retry, first admin user |
| `reduce` | Collapse to one value — total amount, combined error message |
| `distinct` | Remove duplicates — unique providers from a list of transactions |
| `limit` | Pagination — take the first 20 results |
| `skip` | Pagination — skip the first page to get page 2 |
| `flatMap` | Flatten nested collections — all items across all orders in one stream |
| `mapToInt` | Numeric aggregation — total transaction amount, average order value, highest payment |
| `collect` | Materialise the stream into a List, Set, or Map — use after filter/map when you need to store the result |
| `toList` | Shorthand for collect into a List (Java 16+) |

**Notable details:**
- `findFirst()` — returns an `Optional` (might find nothing)
- `reduce((a, b) -> combine)` — returns an `Optional`
- `count()` — returns `long`, not `int`
- `forEach` — if just iterating with no other operations, call directly on the list without `.stream()`
- `collect(Collectors.toList())` — old way to get a List, replaced by `.toList()` in Java 16+
- `.toList()` returns an **unmodifiable** list — you can read it but not add or remove. If you need to modify the result, wrap it: `new ArrayList<>(stream.toList())`

**Method references** — shorthand for a lambda that just calls a single method:
- `n -> n.toUpperCase()` is the same as `String::toUpperCase`
- `n -> System.out.println(n)` is the same as `System.out::println`

**`count()` returns `long`** — declare as `long count = ...`, not `int`. If you need `int`, cast explicitly: `(int) count`

**Numeric stream operations** — use `mapToInt()` to convert a stream to an `IntStream`, which unlocks numeric aggregation methods:
- `mapToInt(n -> n.length())` — maps each element to an int (e.g. string length)
- `.sum()` — total of all values
- `.average()` — returns `OptionalDouble` (empty if stream is empty)
- `.min()` / `.max()` — return `OptionalInt` (empty if stream is empty)

```java
int total    = names.stream().mapToInt(n -> n.length()).sum();
int average  = (int) names.stream().mapToInt(n -> n.length()).average().orElse(0);
int shortest = names.stream().mapToInt(n -> n.length()).min().orElse(0);
int longest  = names.stream().mapToInt(n -> n.length()).max().orElse(0);
```

`average()` returns `OptionalDouble` — not `int` or `double` — because if the stream is empty there's no meaningful answer. `.orElse(0)` unwraps it to a plain `double`, and `(int)` casts that to an `int` (truncates, does not round).

**Collectors** — a utility class with specialized collectors for use with `.collect()`:
- `Collectors.joining(", ")` — use when building a formatted string from a collection, e.g. a comma-separated list of provider names for a log message or API response
- `Collectors.groupingBy(n -> key)` — use when you need to categorise data, e.g. group transactions by status, group payments by provider. Returns `Map<Key, List<Element>>`
- `Collectors.toMap(keyFn, valueFn)` — use when you need a lookup table from a collection, e.g. `Map<userId, email>` for quick lookups later
- `Collectors.toList()` — old way to collect into a List, replaced by `.toList()` in Java 16+

```java
// joining
String result = names.stream().collect(Collectors.joining(", "));

// groupingBy — returns Map<Character, List<String>>
var grouped = names.stream().collect(Collectors.groupingBy(n -> n.charAt(0)));
// e.g. {A=[Alice, Anna], B=[Bob], C=[Charlie], D=[David]}
```

A `Map` is a collection of key-value pairs — `map.get("A")` returns the value associated with key `"A"`.

---

### Lambdas & functional interfaces

A lambda is an anonymous function — same idea as an arrow function in JavaScript. Just a `parameter -> body` with no name.

**Production scenario:** You have a list of payments and need to find all failed ones above £1000. Instead of writing a separate named method for this one-off condition, you write it inline as a lambda passed to `filter()`: `payments.stream().filter(p -> p.status().equals("FAILED") && p.amount() > 1000)`.

```java
s -> s.length() > 3   // takes a String, returns boolean
s -> s.length()       // takes a String, returns Integer
s -> System.out.println(s)  // takes a String, returns nothing
```

The lambda itself is just the `->` expression. What it's allowed to return depends on which **functional interface** you assign it to.

**Functional interfaces** — each one defines a contract for what the lambda takes and returns:

| Interface | Takes | Returns | Use when |
|---|---|---|---|
| `Predicate<T>` | `T` | `boolean` | you need to filter a collection or check a condition — e.g. only premium users, only failed transactions |
| `Function<T, R>` | `T` | `R` | you need to transform data — extract a field, convert an object to a DTO, map amounts to a different currency |
| `Consumer<T>` | `T` | nothing | you need to act on each element without returning anything — log, print, send a notification |
| `Supplier<T>` | nothing | `T` | you need a lazy fallback value that should only be created if actually needed — e.g. `orElseGet`, `orElseThrow` |

The lambda body must match what the functional interface promises to return — if it doesn't, it won't compile.

**Predicate** — a condition that returns true or false:

```java
Predicate<String> isLongerThan3 = s -> s.length() > 3;
Predicate<String> startsWithA   = s -> s.startsWith("A");
Predicate<Integer> isEven       = n -> n % 2 == 0;
```

You could technically use `Function<String, Boolean>` instead of `Predicate<String>`, but `Predicate` is preferred — it's purpose-built and gives you `.and()`, `.or()`, `.negate()` for chaining conditions:

```java
Predicate<User> isAdult   = user -> user.age() >= 18;
Predicate<User> isPremium = user -> user.isPremium();

Predicate<User> premiumAdult  = isAdult.and(isPremium);  // both must be true
Predicate<User> adultOrPremium = isAdult.or(isPremium);  // either is enough
Predicate<User> isMinor       = isAdult.negate();        // flips the result

users.stream().filter(premiumAdult).toList();
```

A `Predicate` has a `.test()` method that runs the condition and returns true or false:

```java
Predicate<User> isAdult = user -> user.age() >= 18;
boolean result = isAdult.test(new User("Alice", 30)); // true
```

In practice you rarely call `.test()` directly like this — you pass the predicate into a method that calls it for you on each element:

```java
users.stream()
    .filter(isAdult)   // filter calls .test() on each user internally
    .toList();
```

**A lambda is a Predicate (or any functional interface) based on its shape, not its declaration.** If you write a lambda inline and the method expects a `Predicate<User>`, the compiler sees it takes a `User` and returns a `boolean` and wires it up automatically — you never have to write the word `Predicate`:

```java
// These two are identical — the inline lambda IS a Predicate, you just didn't name it
Predicate<User> isAdult = user -> user.age() >= 18;
users.stream().filter(isAdult)

users.stream().filter(user -> user.age() >= 18)
```

The reason to store it in a variable is **reuse** — if you need the same predicate in multiple places (e.g. for chaining), give it a name. If you only need it once, write it inline.

**Where each functional interface is used** — the pattern is always the same: if a method needs a condition it takes a Predicate, if it needs a transformation it takes a Function, and so on. You don't need to memorise this list — you'll start recognising the pattern as you read more code.

| Interface | Common methods that accept it |
|---|---|
| `Predicate<T>` | `stream.filter()`, `stream.anyMatch()`, `allMatch()`, `noneMatch()`, `list.removeIf()` |
| `Function<T,R>` | `stream.map()`, `stream.flatMap()`, `Optional.map()` |
| `Consumer<T>` | `stream.forEach()`, `list.forEach()`, `Optional.ifPresent()` |
| `Supplier<T>` | `Optional.orElseGet()`, `Optional.orElseThrow()` |

---

### Optional

A container that either holds a value or is empty — used instead of returning `null` to force explicit handling of the "no value" case and avoid NullPointerExceptions.

**Production scenario:** A payment service looks up a user by ID before processing a payment. The user might not exist. Without Optional you'd return `null` and risk a NullPointerException somewhere down the line. With Optional, `findById()` returns `Optional<User>` — the caller is forced to handle the empty case explicitly: either throw a meaningful error (`orElseThrow`) or provide a fallback (`orElseGet`).

- Some stream operations return `Optional` because they might find nothing: `findFirst()`, `reduce()`, `findAny()`
- Spring Data repository methods like `findById()` also return `Optional`

**Common ways to handle it:**
- `.orElse("default")` — return the value if present, otherwise return the default. The fallback is always evaluated, even if not needed.
- `.orElseGet(() -> fallback)` — same as orElse but lazy — the lambda only runs if the Optional is empty. Prefer this when the fallback is expensive (e.g. a database call).
- `.orElseThrow(() -> new SomeException())` — return the value if present, otherwise throw
- `.ifPresent(n -> action)` — only run the action if a value is present, does nothing if empty
- `.get()` — returns the value directly but throws `NoSuchElementException` if empty. Discouraged — use the options above instead.
- `.map(fn)` — transform the value inside the Optional if present, stays wrapped in Optional. Useful for extracting a field without unwrapping first:

```java
Optional<String> email = findById(1).map(user -> user.email());
System.out.println(email.orElse("no email found"));

// or chained into a String directly
String email = findById(1).map(user -> user.email()).orElse("no email found");
```

**In Spring Boot** — use `orElseThrow` when the entity must exist (e.g. updating a record), use `orElse` or `ifPresent` when absence is a normal case (e.g. optional profile data)

**Primitive Optionals** — `OptionalDouble`, `OptionalInt`, `OptionalLong` are specialized versions of `Optional` for primitive types. They work the same way but avoid boxing overhead:
- `Optional<Double>` wraps a `Double` object — two heap allocations (the Optional + the Double inside)
- `OptionalDouble` stores a primitive `double` directly inside — one heap allocation, no boxing
- Stream methods like `average()` return `OptionalDouble` rather than `Optional<Double>` for this reason

---

### Collections

#### Lists & queues

A `List` is an ordered collection that allows duplicates. Elements stay in the order you put them in and can be accessed by position.

**Production scenario:** You fetch all transactions for a merchant from the database and return them to the frontend in chronological order. Order matters — the merchant expects to see newest first after sorting. The same provider can appear many times, which is fine.

**Variants — which to use:**

| Implementation | Use when |
|---|---|
| `ArrayList` | Default choice — fast random access by index, good for most cases |
| `LinkedList` | You frequently add/remove from the middle or beginning — rare in practice |
| `ArrayDeque` | You need a queue (first-in first-out) or stack (last-in first-out) — e.g. processing jobs in order |

In practice you'll use `ArrayList` almost always. `LinkedList` and `ArrayDeque` are niche.

**`List.of()` — quick inline list**
`List.of("Stripe", "Adyen")` creates a list in one line without declaring a variable. Java infers the type from what you put inside. It is unmodifiable — you can read from it but cannot add or remove elements. Use it when you just need a fixed list to pass somewhere. If you need to modify the list afterwards, use `new ArrayList<>()` instead.

**Declare as the interface, create with the implementation** — this is standard Java convention you'll see everywhere in production code:

```java
List<String> providers = new ArrayList<>();   // not ArrayList<String>
Set<String> unique = new HashSet<>();          // not HashSet<String>
Map<String, Double> limits = new HashMap<>();  // not HashMap<String, Double>
```

This means if you ever need to swap the implementation (e.g. `ArrayList` → `LinkedList`), only the one line where you create it needs to change — nothing else in your code is affected.

**Real-world flow:**
A payment service fetches all transactions for a merchant from the database. They come back as a `List<Transaction>` in chronological order. You iterate over them, filter for failures, and return the results — order matters because you want to show the most recent first after sorting.

**Common methods:**
- `.add(value)` — append to the end
- `.add(index, value)` — insert at a specific position
- `.get(index)` — retrieve by position
- `.remove(value)` — remove by value
- `.remove(index)` — remove by position
- `.size()` — number of elements

**For-each loop — iterate over any collection**

The for-each loop goes through every element one at a time. Left side is the single item, right side is the collection:

```java
List<String> providers = List.of("Stripe", "Adyen", "Klarna");

for (String provider : providers) {
    System.out.println(provider);
}
```

Read it as: "for each String **provider** in **providers**". Works on any collection — List, Set, arrays. For Map you loop over `map.entrySet()` to get both key and value:

```java
Map<String, Double> limits = Map.of("Stripe", 10000.0, "Adyen", 50000.0);

for (Map.Entry<String, Double> entry : limits.entrySet()) {
    System.out.println(entry.getKey() + ": " + entry.getValue());
}
```

In modern Java you'll use `.forEach()` on streams more often, but the for-each loop is common when you need a regular loop without stream operations.

---

#### Sets

A `Set` is a collection with no duplicates. Use when you need uniqueness and don't care about order.

**Production scenario:** You process a batch of transactions and need to know which providers were involved for a report. You don't want duplicates and you don't care about order — just the unique provider names. You stream the transactions, extract the provider from each, and collect into a `Set` — duplicates are discarded automatically.

**Variants — which to use:**

| Implementation | Order | Use when |
|---|---|---|
| `HashSet` | None | Default choice — fastest, order doesn't matter |
| `LinkedHashSet` | Insertion order | You need uniqueness but also want elements in the order they were added |
| `TreeSet` | Sorted (A-Z or 0-9) | You need uniqueness and want elements sorted automatically |

In practice you'll use `HashSet` almost always. Reach for `LinkedHashSet` when order matters, `TreeSet` when you need sorting.

**Real-world flow:**
You have a list of thousands of transactions. A report needs to show which providers were used, each listed once. You stream the transactions, extract the provider name from each, and collect into a `Set` — duplicates are discarded automatically:

```java
Set<String> usedProviders = transactions.stream()
    .map(t -> t.provider())
    .collect(Collectors.toSet()); // [Stripe, Adyen, Klarna] — no duplicates
```

**Common methods:**
- `.add(value)` — adds the value, ignores it if already present
- `.remove(value)` — removes the value
- `.contains(value)` — check if a value exists — faster than List for large collections
- `.size()` — number of elements

---

#### Maps

A `Map` stores key-value pairs — you look up a value by its key instantly, without looping or if/else chains.

**Real-world flow — provider transaction limits:**
1. App starts up — provider limits are loaded from the database once and stored in a `HashMap` in memory
2. Payment request comes in — `provider = "Adyen"`, `amount = 30000.0`
3. You look up the limit: `limits.get("Adyen")` → `50000.0` — instant, no database call needed
4. You compare: `30000.0 < 50000.0` → payment allowed

Without a Map you'd either hit the database on every request (slow) or write if/else chains that break every time a new provider is added:

```java
// without Map — breaks every time a provider is added or removed
if (provider.equals("Adyen")) limit = 50000.0;
else if (provider.equals("Stripe")) limit = 10000.0;

// with Map — one lookup regardless of how many providers there are
double limit = limits.getOrDefault(provider, 0.0);
```

**Variants — which to use:**

| Implementation | Order | Use when |
|---|---|---|
| `HashMap` | None | Default choice — fastest, order doesn't matter |
| `LinkedHashMap` | Insertion order | You need key-value lookup but also want entries in the order they were added |
| `TreeMap` | Sorted by key (A-Z or 0-9) | You need key-value lookup and want keys sorted automatically |

In practice you'll use `HashMap` almost always.

**Common methods:**
- `.put(key, value)` — add or update an entry
- `.get(key)` — returns the value, or `null` if the key doesn't exist
- `.getOrDefault(key, fallback)` — returns the value, or the fallback if the key doesn't exist. Prefer over `.get()` to avoid NullPointerExceptions when a missing key is a normal case.
- `.containsKey(key)` — check if a key exists
- `.forEach((key, value) -> action)` — iterate over all entries. Takes two parameters unlike List/Set forEach which takes one.

**When to use a Map vs fetching from the DB:**
- Fetch from DB each time — when the data is user-specific, changes frequently, or you only need it once (e.g. a user's profile, a specific transaction)
- Load into a Map in memory — when the data is shared across many requests and rarely changes (e.g. provider fee rates, config values). This is called caching. Spring Boot has built-in support for this (`@Cacheable`) which you'll cover later.

---

### Exception handling

**The full picture — how exceptions actually work in production:**

A payment request comes in. Your code calls an external provider API. The provider returns a 503. You can't just return null or an empty string — the caller has no idea something went wrong. Instead you throw an exception. Execution stops immediately, and the exception travels back up through every method that called this one until something catches it and decides what to do — log it, retry, return a fallback, or let it reach Spring Boot which turns it into a 500 response.

**Two flows — with and without try/catch:**

Most of the time you just throw and let it bubble — no try/catch needed:
1. Define the exception class
2. Throw it in a method when something goes wrong
3. Call the method — the exception travels up on its own to Spring Boot

Only write try/catch when you can recover from the failure — retry, fallback, log and continue:
1. Define the exception class
2. Throw it in a method when something goes wrong
3. Call that method inside a try/catch block and handle the failure

**The full try/catch flow, in order:**

**Step 1 — define the exception**

The structure is always the same. You just swap the name:

```java
class ProviderException extends RuntimeException {
    public ProviderException(String message) {
        super(message);
    }
}
```

`RuntimeException` is the base. You extend it to give your exception a meaningful name. That name is what shows up in logs — `ProviderException: Adyen returned 503` tells you immediately what went wrong without reading further. A plain `RuntimeException` loses that.

**Step 2 — throw it when something goes wrong**

```java
String chargeCard(String provider, double amount) {
    HttpResponse response = httpClient.post("https://api." + provider + ".com/charge", amount);

    if (response.statusCode() != 200) {
        throw new ProviderException(provider + " returned " + response.statusCode());
    }

    return response.body();
}
```

When `throw` executes, the method stops immediately. No return value. The exception travels up to whoever called `chargeCard`.

**Step 3 — catch it where you can do something useful**

```java
String processPayment(String provider, double amount) {
    try {
        return chargeCard(provider, amount);
    } catch (ProviderException e) {
        System.err.println("Provider call failed: " + e.getMessage());
        return "payment queued for retry";
    }
}
```

`processPayment` calls `chargeCard` inside a try block. If `chargeCard` throws a `ProviderException`, execution jumps immediately to the catch block. You write `ProviderException e` because you know that's what `chargeCard` throws — `e` gives you access to the message via `e.getMessage()`.

You catch it here because this is where you have enough context to do something useful — log it and queue for retry. Don't catch just to silence the error — an empty catch block hides bugs.

**Bubbling up — when nothing catches it**

If `processPayment` had no try/catch, the exception would keep traveling up to whatever called `processPayment`, and so on. In Spring Boot, if it reaches the top without being caught, Spring catches it and returns a 500 response. The app keeps running — only that one request fails.

**Rethrowing — catch, do something, then let it keep traveling**

Sometimes you need to catch an exception to do some work (like updating a database record), but you still want the exception to propagate up so the caller knows something went wrong. You do this by catching and then rethrowing with `throw e`:

```java
Transaction processPayment(String provider, double amount) {
    Transaction transaction = new Transaction();
    transaction.setStatus(TransactionStatus.PENDING);
    repository.save(transaction);

    try {
        provider.charge(amount);
        transaction.setStatus(TransactionStatus.COMPLETED);
        repository.save(transaction);
    } catch (RuntimeException e) {
        transaction.setStatus(TransactionStatus.FAILED);
        repository.save(transaction);
        throw e; // rethrow — let it travel up to the caller
    }

    return transaction;
}
```

Without `throw e`, the catch block would swallow the exception and the method would return the transaction silently — the caller would think the payment succeeded. With `throw e`, the catch block marks the transaction as `FAILED` in the database, then lets the exception continue traveling up so the controller can return a proper error response.

**The rule:** only swallow an exception (catch without rethrowing) when you can genuinely recover — retry the call, return a fallback value. If you can't recover, always rethrow.

**Checked vs unchecked — the two types:**

| Type | Extends | Must be declared/caught? | Use when |
|---|---|---|---|
| Checked | `Exception` | Yes — compiler forces you to handle it | External failures you can reasonably recover from — file not found, network timeout |
| Unchecked | `RuntimeException` | No — compiler doesn't force handling | Programming errors or unrecoverable failures — invalid input, null values, provider errors |

In modern Java and Spring Boot you'll almost always use unchecked exceptions (`RuntimeException`). Checked exceptions add boilerplate and are increasingly avoided.

**Custom exception — generic vs specific message**

If the exception always means the same thing, hardcode the message and take the relevant value as a parameter:

```java
class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(int id) {
        super("User not found: " + id);  // always the same shape
    }
}
```

If the exception covers multiple scenarios that each need a different message, take the message as a parameter and let the caller decide:

```java
class InvalidPaymentRequestException extends RuntimeException {
    public InvalidPaymentRequestException(String message) {
        super(message);
    }
}

throw new InvalidPaymentRequestException("Card number is missing");
throw new InvalidPaymentRequestException("Amount must be greater than 0");
```

**Multiple catch blocks — handle different failures differently**

One try block can have multiple catch clauses — each catches a specific exception type:

```java
try {
    processPayment(userId, amount);
} catch (UserNotFoundException e) {
    System.out.println("User error: " + e.getMessage());  // handle 404
} catch (PaymentFailedException e) {
    System.out.println("Payment error: " + e.getMessage());  // handle 400
}
```

Catch blocks are checked top to bottom — the first one that matches runs, the rest are skipped. Only one exception can be thrown per try block — once an exception is thrown execution jumps to the catch immediately.

**finally — code that always runs**

A `finally` block runs after the try/catch regardless of whether an exception was thrown or not. Use for cleanup that must always happen — logging that a call was attempted, resetting state:

```java
String provider = "Revolut";
try {
    fetchFromProvider(provider);
} catch (RuntimeException e) {
    System.out.println("Failed: " + e.getMessage());
} finally {
    System.out.println("Provider call finished for: " + provider);  // always runs
}
```

In most real code you'd use try-with-resources instead of finally for closing resources. Use finally for non-closeable cleanup.

**In Spring Boot — @ControllerAdvice**

In Spring Boot you almost never write try/catch for business logic failures. You throw custom exceptions and let them bubble up to a `@ControllerAdvice` class that maps each exception type to an HTTP response — defined once, applies to every endpoint automatically:

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFound(UserNotFoundException e) {
        return ResponseEntity.status(404).body(e.getMessage());
    }

    @ExceptionHandler(InvalidPaymentRequestException.class)
    public ResponseEntity<String> handleInvalidRequest(InvalidPaymentRequestException e) {
        return ResponseEntity.status(400).body(e.getMessage());
    }
}
```

Your controller and service methods just return data or throw exceptions — they never touch `ResponseEntity` or HTTP status codes. Spring wraps every return value in a 200 response automatically. Only write try/catch yourself when you can genuinely recover — retry a provider call, return a fallback.

**try-with-resources — automatically close what you open**

Some resources need to be closed after use — file handles, database connections, HTTP clients. If you open one inside a regular try block and an exception is thrown, the close call never runs and the resource leaks. try-with-resources closes it automatically when the block ends, even if an exception is thrown:

```java
try (BufferedReader reader = new BufferedReader(new FileReader("payments.csv"))) {
    String line = reader.readLine();
} catch (Exception e) {
    System.out.println("Failed: " + e.getMessage());
} // reader.close() is called automatically here
```

The class inside the parentheses must implement `AutoCloseable` — that's what tells Java it can be closed. You'll understand this fully when you cover interfaces.

---

### Inheritance & polymorphism

**The full picture:**

You're building a payment platform that supports multiple providers — Stripe, Adyen, Klarna. Each provider needs to charge a card, refund a payment, and check a transaction status. The logic for logging, error handling, and retries is identical across all of them. Without inheritance you'd copy that shared logic into every provider class — a bug fix in one means fixing it in all of them. With inheritance, shared logic lives in one `BasePaymentProvider` class. Each provider extends it, inherits the shared logic for free, and only overrides the parts that are provider-specific.

**Inheritance — extending a class**

A subclass extends a parent class and inherits all its non-private fields and methods:

```java
class BasePaymentProvider {
    String name;

    BasePaymentProvider(String name) {
        this.name = name;
    }

    void logAttempt(double amount) {
        System.out.println(name + ": attempting charge of " + amount);
    }
}

class StripeProvider extends BasePaymentProvider {
    StripeProvider() {
        super("Stripe");  // calls the parent constructor
    }
}
```

`StripeProvider` inherits `logAttempt()` for free — you don't write it again. `super("Stripe")` calls the parent constructor to set the `name` field.

**Method overriding — replacing a parent method**

A subclass can replace a parent method with its own implementation:

```java
class BasePaymentProvider {
    String charge(double amount) {
        return "charged " + amount;
    }
}

class AdyenProvider extends BasePaymentProvider {
    @Override
    String charge(double amount) {
        return "Adyen charged " + amount;  // Adyen-specific implementation
    }
}
```

`@Override` tells the compiler you're intentionally replacing the parent method — if you typo the method name, the compiler catches it.

Calling `super.charge(amount)` inside the override runs the parent version first — useful when you want to keep the parent behaviour and add to it:

```java
class KlarnaProvider extends BasePaymentProvider {
    @Override
    String charge(double amount) {
        String result = super.charge(amount);       // runs parent: "charged 75.0"
        return result + " [Klarna fee added]";      // "charged 75.0 [Klarna fee added]"
    }
}
```

**Runtime polymorphism — the key concept**

You can declare a variable as the parent type but assign a child object to it:

```java
BasePaymentProvider provider = new AdyenProvider();
provider.charge(100.0);  // calls AdyenProvider's charge(), not BasePaymentProvider's
```

Java uses the actual object type (`AdyenProvider`) at runtime to decide which method to call, even though the declared type is `BasePaymentProvider`. This is runtime polymorphism.

The real power is when you have a list of providers:

```java
List<BasePaymentProvider> providers = List.of(
    new StripeProvider(),
    new AdyenProvider(),
    new KlarnaProvider()
);

for (BasePaymentProvider provider : providers) {
    provider.charge(100.0);  // each calls its own implementation
}
```

The loop doesn't know or care which provider it's dealing with — it just calls `charge()` and Java picks the right one. Adding a new provider means creating a new class — nothing else changes.

**Method overloading — same name, different parameters**

The same method name can exist multiple times in a class as long as the parameters differ. Java picks the right one at compile time based on what you pass:

```java
class PaymentService {
    void charge(double amount) { ... }
    void charge(double amount, String currency) { ... }
    void charge(double amount, String currency, String provider) { ... }
}

service.charge(100.0);                        // calls first
service.charge(100.0, "GBP");                 // calls second
service.charge(100.0, "GBP", "Adyen");        // calls third
```

Use overloading when a method does the same thing but with optional extra parameters — it's cleaner than different method names like `chargeWithCurrency` or `chargeWithProvider`.

---

### Interfaces & abstract classes

**The full picture:**

You're building a payment platform. Stripe, Adyen, Klarna — each charges a card differently, calls a different API, refunds differently. But your `PaymentServiceTest` doesn't want to know which provider it's dealing with. It just wants to call `charge()` and `refund()`. An interface defines *what* every provider must be able to do without saying *how*. Every provider implements the interface. Your service declares the type as `PaymentProvider` — Spring injects whichever implementation is active at runtime. This is the core pattern Spring Boot is built on.

An abstract class sits between an interface and a full class. Use it when multiple providers share real code — fields, constructors, concrete methods — but you still want to force each one to implement a specific part themselves.

**Production scenario:** `PaymentProvider` is an interface. `StripeProvider` and `AdyenProvider` implement it. `PaymentServiceTest` depends on `PaymentProvider` (the interface) — it never imports `StripeProvider` directly. In tests, you swap in a fake implementation. In production, Spring injects the real one. Nothing in `PaymentServiceTest` changes between environments.

---

**Interface — a contract**

An interface declares method signatures only — no method bodies, no instance fields. Any class that `implements` it must provide a body for every method. The compiler refuses to build if any are missing.

Fields declared in an interface are implicitly `public static final` — they are constants, shared across everything, and cannot be changed. You don't need to write those keywords yourself:

```java
interface PaymentProvider {
    String DEFAULT_CURRENCY = "USD";  // implicitly public static final
}
```

This is different from methods — regular interface methods are instance methods (not static) unless you explicitly mark them `static`.

```java
interface PaymentProvider {
    String charge(double amount);   // implicitly public
    String refund(double amount);
}

class StripeProvider implements PaymentProvider {
    @Override
    public String charge(double amount) {
        return "Stripe charged " + amount;
    }

    @Override
    public String refund(double amount) {
        return "Stripe refunded " + amount;
    }
}
```

`implements` is used instead of `extends`. A class can implement multiple interfaces — separate them with commas: `class Foo implements A, B`. This is not possible with `extends` — a class can only extend one other class.

**Interface as type — the real power**

Declaring a variable as the interface type means your code works for any implementation:

```java
PaymentProvider provider = new StripeProvider();
provider.charge(100.0);  // calls StripeProvider's charge()

List<PaymentProvider> providers = List.of(new StripeProvider(), new AdyenProvider());

for (PaymentProvider p : providers) {
    p.charge(100.0);  // each calls its own implementation
}
```

This is exactly how Spring Boot dependency injection works — your service declares `PaymentProvider provider` and Spring decides which class to inject at runtime.

**Default methods — shared behaviour in an interface**

An interface can have a `default` method — a method with a body. All implementing classes get it for free, but can override it if they want. The `default` keyword is required:

```java
interface PaymentProvider {
    String charge(double amount);

    default void log(double amount) {
        System.out.println("Attempting charge of " + amount);
    }
}
```

Use default methods when you want shared behaviour in an interface without forcing every implementing class to write it. It's like combining a contract with inheritance for that one method — the difference from a regular abstract class is that a class can implement multiple interfaces and get default methods from all of them, whereas it can only extend one class.

**Abstract class — partial implementation**

An abstract class cannot be instantiated directly — you must extend it. It can have fields, constructors, concrete methods, and `abstract` methods. An `abstract` method has no body — the subclass must implement it:

```java
abstract class AbstractPaymentProvider {
    String name;

    AbstractPaymentProvider(String name) {
        this.name = name;
    }

    abstract String callApi(double amount);  // no body — subclass must implement

    String charge(double amount) {
        return "Charging via " + callApi(amount);  // calls the subclass version at runtime
    }
}

class KlarnaProvider extends AbstractPaymentProvider {
    KlarnaProvider() {
        super("Klarna");
    }

    @Override
    String callApi(double amount) {
        return "Klarna API: " + amount;
    }
}
```

`new AbstractPaymentProvider()` won't compile — you can never instantiate an abstract class directly.

**Interface vs abstract class — when to use each**

| | Interface | Abstract class |
|---|---|---|
| Fields | Constants only (`public static final`) | Regular instance fields |
| Constructor | No | Yes |
| Multiple allowed | Yes — `implements A, B` | No — only one `extends` |
| Use when | Defining a contract — what something *must* do | Sharing code — what something *partly* does |

**Fields — the key difference:**
- Abstract class fields are regular instance fields — not static, not final. You can declare them without a value and subclasses set them via `super(...)`. Each instance gets its own copy.
- Interface fields are always implicitly `public static final` — you must assign a value immediately. They are constants, shared everywhere, never changed. You cannot declare an uninitialized field in an interface.

**Why a helper method like `buildLogMsg` belongs in the abstract class, not the interface:**
Two reasons. First, it has a body with real logic — interfaces can only hold uninitialized method signatures by default (a `default` method is the exception, but it can't access instance fields like `apiUrl`). Second, it's not part of the contract — `PaymentServiceTest` and `ProviderRegistry` never need to call `buildLogMsg`. It's an internal helper for providers to reuse. If it's not something the outside world needs to call, it doesn't belong in the interface.

Rule of thumb: **start with an interface**. Add an abstract class only when multiple implementations share real code you want to write once.

**The standard pattern:** abstract class for the shared hierarchy (fields, shared logic, forces subclasses to implement the provider-specific step), interfaces for the contracts your service depends on and for optional capabilities. A class can only extend one abstract class but implement as many interfaces as needed — so `StripeProvider extends AbstractPaymentProvider implements PaymentProvider, Refundable` is valid and common. Your `PaymentServiceTest` only knows about `PaymentProvider`. Where you specifically need refund support, you declare `Refundable` — and only the providers that can refund will satisfy that contract.

**How Spring uses interfaces**

In Spring Boot you almost never depend on a concrete class directly. You declare the type as an interface:

```java
@Service
public class PaymentService {
    private final PaymentProvider provider;  // interface — not StripeProvider

    public PaymentService(PaymentProvider provider) {
        this.provider = provider;  // Spring injects the matching implementation
    }
}
```

Spring finds whichever class implements `PaymentProvider` and registered as a bean, and injects it automatically. You can swap implementations (Stripe vs Adyen, real vs mock) without touching `PaymentServiceTest`.

---

### Generics

**The full picture:**

You're reading a provider integration codebase and you see: `public abstract class AbstractCreditCardDeposit<C extends ProviderConfig>`. Before you can extend it with your own provider, you need to understand what `<C extends ProviderConfig>` means. Generics are how Java lets a class or method work with any type while still being type-safe — instead of writing `ApiResponse` that holds an `Object` and requires casting everywhere, you write `ApiResponse<T>` and the caller decides what type `T` is.

**Production scenario:** Your payment platform has a generic `ApiResponse<T>` wrapper that every provider call returns — `T` is the body type, which differs per call: `ApiResponse<ChargeResult>`, `ApiResponse<RefundResult>`. One class handles the envelope (status code, error message, success flag); the caller decides what type lives inside. Without generics you'd either write a separate class per response type, or use `Object` and cast everywhere — both are bad.

---

**Why generics exist**

Without generics, a box that holds anything must use `Object`:

```java
class Box {
    Object value;
}

Box b = new Box();
b.value = "hello";
String s = (String) b.value;  // must cast, compiler can't help you
```

With generics, the type is declared upfront and the compiler enforces it:

```java
class Box<T> {
    T value;
}

Box<String> b = new Box<>();
b.value = "hello";
String s = b.value;  // no cast needed — compiler already knows it's a String
```

`T` is a type parameter — a placeholder filled in when the class is used. The name `T` is a convention (Type), but you'll also see `E` (Element), `K`/`V` (Key/Value in Maps), `R` (Return type). They're just names.

---

**Generic class**

Declare `<T>` after the class name. Use `T` anywhere in the class body:

```java
class ApiResponse<T> {
    int statusCode;
    T body;
    String error;

    ApiResponse(int statusCode, T body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }
}
```

The caller fills in `T`:

```java
ApiResponse<ChargeResult> response = new ApiResponse<>(200, new ChargeResult(...));
ChargeResult result = response.body;  // no cast — compiler knows it's ChargeResult
```

---

**Generic method**

A method can declare its own type parameter independently of the class:

```java
class ProviderUtils {
    static <T> T firstOrThrow(List<T> items, String errorMessage) {
        if (items.isEmpty()) throw new RuntimeException(errorMessage);
        return items.get(0);
    }
}
```

The `<T>` before the return type declares it. Java infers `T` from what you pass in — you never write it explicitly when calling:

```java
ChargeResult first = ProviderUtils.firstOrThrow(charges, "No charges found");
```

---

**Bounded type parameters**

`<T extends SomeClass>` restricts which types are allowed. `T` must be `SomeClass` or a subclass of it:

```java
class ProviderRunner<T extends AbstractPaymentProvider> {
    T provider;

    ProviderRunner(T provider) {
        this.provider = provider;
    }

    String run(double amount) {
        return provider.charge(amount);  // safe — T is guaranteed to have charge()
    }
}
```

Without the bound, `T` could be anything — `String`, `Integer` — and `provider.charge()` wouldn't compile because the compiler doesn't know `T` has that method. The bound tells the compiler: `T` is always an `AbstractPaymentProvider`, so all its methods are available.

---

**Extending a generic class — the pattern from your target codebase**

This is what you'll actually do on the job. A generic base class defines the shared structure; you extend it with your specific type:

```java
abstract class AbstractProviderHandler<C extends ProviderConfig> {
    C config;

    AbstractProviderHandler(C config) {
        this.config = config;
    }

    abstract String callApi(double amount);

    String charge(double amount) {
        return "Charging via " + callApi(amount);
    }
}
```

You extend it by filling in `C` with your specific config type:

```java
class KlarnaHandler extends AbstractProviderHandler<KlarnaConfig> {
    KlarnaHandler(KlarnaConfig config) {
        super(config);
    }

    @Override
    String callApi(double amount) {
        return "Klarna API key: " + config.apiKey + ", amount: " + amount;
    }
}
```

`config` is now typed as `KlarnaConfig` — not `ProviderConfig`, not `Object`. You get full access to all `KlarnaConfig` fields without casting.

---

**Wildcards — `?`**

`?` means "some unknown type". You use it when you want to accept a generic type but don't need to know what `T` is:

```java
void printAll(List<?> items) {
    for (Object item : items) {
        System.out.println(item);
    }
}
```

This accepts `List<String>`, `List<Integer>`, `List<ChargeResult>` — anything. You see it most often in method parameters in library code. In your own production code you'll mostly read it, not write it.

`List<? extends ProviderConfig>` — any list whose elements are `ProviderConfig` or a subclass. You can read from it but not add to it (the compiler doesn't know the exact type so it can't guarantee safety on writes).

---

### Enums

**The problem enums solve:**

Without enums, you'd represent a transaction's status as a String:

```java
String status = "COMPLETED";

if (status.equals("COMPELTED")) {  // typo — no compiler error, silent bug
    // ...
}
```

The compiler can't help you — "COMPELTED" is a valid String. The bug only shows up at runtime.

With an enum, the compiler knows every valid value upfront:

```java
TransactionStatus status = TransactionStatus.COMPLETED;

if (status == TransactionStatus.COMPELTED) {  // compiler error — this value doesn't exist
    // ...
}
```

**Production scenario:** A payment transaction moves through states — created, sent to provider, confirmed by provider, or failed. Each state is a fixed known value. You use an enum so the compiler catches invalid states at build time, not in production at 2am.

---

**Basic enum**

An enum is a class with a fixed set of named values. You declare it with the `enum` keyword:

```java
enum TransactionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED
}
```

You use a value like this:

```java
TransactionStatus status = TransactionStatus.COMPLETED;
```

Each value is its own instance of the enum — `TransactionStatus.COMPLETED` is always the same object, so you compare with `==` instead of `.equals()`:

```java
if (status == TransactionStatus.COMPLETED) {
    // process it
}
```

---

**Enum with fields — attaching data to each value**

An enum can have fields, a constructor, and methods — just like a class. This lets each value carry extra information:

```java
enum PaymentMethod {
    CARD("Credit or debit card"),
    BANK_TRANSFER("Direct bank transfer"),
    WALLET("Digital wallet");

    String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }
}
```

You access the field directly on the value:

```java
PaymentMethod method = PaymentMethod.CARD;
System.out.println(method.displayName);  // "Credit or debit card"
```

Note: you never call `new PaymentMethod(...)` yourself — the enum creates its values automatically using the constructor when the class loads.

---

**Built-in enum methods**

Every enum gets these for free:

- `.name()` — returns the value's name as a String: `TransactionStatus.COMPLETED.name()` → `"COMPLETED"`
- `.ordinal()` — returns the position (0-based): `TransactionStatus.COMPLETED.ordinal()` → `2`
- `TransactionStatus.values()` — returns an array of all values in declaration order — useful for looping
- `TransactionStatus.valueOf("COMPLETED")` — converts a String to the matching enum value. Throws `IllegalArgumentException` if the String doesn't match any value.

In practice you'll use `.name()` and `values()` most. `valueOf()` is useful when you receive a status as a String from an API or database and need to convert it.

---

**Switch on an enum**

Switch works cleanly with enums — no need to write the enum name on each case:

```java
TransactionStatus status = TransactionStatus.FAILED;

switch (status) {
    case COMPLETED:
        System.out.println("Payment confirmed");
        break;
    case FAILED:
        System.out.println("Payment failed — retry");
        break;
    case REFUNDED:
        System.out.println("Payment refunded");
        break;
    default:
        System.out.println("Payment in progress");
}
```

In modern Java (14+) you'll also see switch expressions which are more concise, but the above form is what you'll encounter most in existing codebases.

---

**Where enums appear in production code**

- Database columns — Spring Data JPA can store an enum value as a String in the database automatically
- API responses — Jackson (the JSON library) serializes an enum to its name by default: `COMPLETED` becomes `"COMPLETED"` in the JSON
- Method parameters — instead of accepting a String `"CARD"` or `"BANK_TRANSFER"`, a method takes a `PaymentMethod` enum so the compiler enforces valid values

---

### Regular expressions

**The problem regular expressions solve:**

You receive a webhook from a payment provider. The payload contains a phone number, but it could be formatted in multiple ways — `+46701234567`, `0701234567`, `070-123 45 67`. You need to validate it and extract the digits. Writing if/else logic to cover every format would be messy and brittle. A regular expression describes the pattern in one line, and Java checks if the input matches it.

**Production scenario:** Your platform receives card numbers, IBANs, phone numbers, and webhook event names from providers — all as Strings. You use regular expressions to validate format before processing, extract specific parts (e.g. the country code from an IBAN), or route events based on their name pattern (e.g. all events starting with `payment.` go to one handler).

---

**How it works**

A regular expression (regex) is a pattern written as a String. Java checks whether another String matches that pattern.

The two classes you need are both in `java.util.regex`:
- `Pattern` — compiles the regex pattern. Expensive to create, so compile once and reuse.
- `Matcher` — runs the compiled pattern against a specific input String.

```java
import java.util.regex.Pattern;
import java.util.regex.Matcher;

Pattern pattern = Pattern.compile("\\d{4}");  // matches exactly 4 digits
Matcher matcher = pattern.matcher("Card expires 0928");
boolean found = matcher.find();  // true — "0928" matches
```

Or for a quick one-off check, `String` has a built-in shortcut:

```java
boolean valid = "0928".matches("\\d{4}");  // true
```

Use `String.matches()` for simple one-off checks. Use `Pattern` + `Matcher` when you reuse the same pattern many times (better performance) or need to extract matched parts.

---

**Pattern syntax — the building blocks**

You only need a small subset of regex to cover most production use cases:

| Pattern | Means | Example match |
|---|---|---|
| `\d` | any digit (0-9) | `\d\d\d` matches `"123"` |
| `\w` | any letter, digit, or underscore | `\w+` matches `"stripe_key"` |
| `\s` | any whitespace (space, tab) | `hello\sworld` matches `"hello world"` |
| `.` | any single character | `p.y` matches `"pay"`, `"p3y"` |
| `+` | one or more of the previous | `\d+` matches `"1"`, `"42"`, `"9999"` |
| `*` | zero or more of the previous | `\d*` matches `""`, `"1"`, `"999"` |
| `?` | zero or one of the previous | `colou?r` matches `"color"` and `"colour"` |
| `{n}` | exactly n of the previous | `\d{4}` matches `"0928"` only |
| `{n,m}` | between n and m of the previous | `\d{2,4}` matches `"12"`, `"123"`, `"1234"` |
| `^` | start of the string | `^pay` matches `"payment"` but not `"stripe_pay"` |
| `$` | end of the string | `\d$` matches `"ref123"` but not `"123ref"` |
| `[abc]` | any one of these characters | `[aeiou]` matches any vowel |
| `[^abc]` | any character NOT in this set | `[^0-9]` matches any non-digit |
| `(abc)` | a group — captures this part | `(\d{4})` captures 4 digits as a group |
| `\|` | or | `card\|wallet` matches `"card"` or `"wallet"` |

Note: in Java Strings, `\` must be written as `\\`. So the regex `\d` is written as `"\\d"` in Java code.

---

**Matching vs finding**

Two different operations:

- `matcher.matches()` — the entire String must match the pattern from start to end
- `matcher.find()` — looks for the pattern anywhere inside the String

```java
Pattern p = Pattern.compile("\\d{4}");

p.matcher("0928").matches();           // true  — entire string is 4 digits
p.matcher("expires 0928").matches();   // false — not the entire string
p.matcher("expires 0928").find();      // true  — found 4 digits somewhere inside
```

Use `matches()` for validation (is this string a valid card expiry?). Use `find()` for extraction (find the expiry inside a larger string).

---

**Extracting matched parts — groups**

Wrap part of the pattern in `()` to capture it. After `find()` or `matches()`, call `matcher.group(1)` to get what was captured:

```java
Pattern p = Pattern.compile("expires (\\d{2})/(\\d{2})");
Matcher m = p.matcher("Card expires 09/28");

if (m.find()) {
    String month = m.group(1);  // "09"
    String year  = m.group(2);  // "28"
}
```

`group(0)` is the entire match. `group(1)` is the first `()`, `group(2)` is the second, and so on.

---

**Common patterns you'll actually use**

```java
"\\d+"          // one or more digits — amount, ID
"\\d{16}"       // exactly 16 digits — card number
"[A-Z]{2}\\d+"  // two uppercase letters followed by digits — IBAN prefix
"payment\\..*"  // starts with "payment." — webhook event routing
"\\+?\\d{7,15}" // optional + then 7-15 digits — phone number
```

---

## Spring Boot

---

### Naming conventions

**Method names must say what they return — not just what they do.**

A method name like `getById` is ambiguous — get what by ID? When a service grows to handle multiple entities (`Transaction`, `Merchant`, `Provider`), generic names become impossible to distinguish. Always include the entity name:

```java
// Bad — unclear what is being fetched
paymentServiceTest.getById(42)
paymentServiceTest.findByStatus(status)

// Good — clear from the name alone what is returned
paymentServiceTest.findTransactionById(42)
paymentServiceTest.findTransactionsByStatus(status)
paymentServiceTest.findMerchantByName("Acme")
```

This applies to all layers — controllers, services, and repositories. The name must read clearly when called from another class, without needing to look at the return type or surrounding context.

**General naming rules in production Java:**
- Methods that return a single object: `findTransactionById`, `getMerchantByName`
- Methods that return a list: `findTransactionsByStatus`, `getAllMerchants`
- Methods that save: `saveTransaction`, `updateMerchant`
- Methods that delete: `deleteTransactionById`
- Boolean methods: `isPaymentCompleted`, `hasActiveMerchant`

---

### Beans & the application context

**The full picture:**

Without Spring, you'd have to create and wire every object manually:

```java
StripeProvider stripe = new StripeProvider();
AdyenProvider adyen = new AdyenProvider();
ProviderRegistry registry = new ProviderRegistry(stripe, adyen);
PaymentService service = new PaymentService(registry);
```

Every time you add a provider, change a dependency, or create a new class, you'd have to update this wiring code. In a codebase with hundreds of classes this becomes unmanageable.

There's a more specific problem too. If `ProviderRegistry` creates its own providers with `new`, it's hardcoded to those specific classes. Adding Klarna means opening `ProviderRegistry` and adding `new KlarnaProvider()`. The registry that's supposed to be provider-agnostic becomes the place where all provider knowledge lives.

With `@Component` and Spring's injection, `ProviderRegistry` just declares it needs a `List<PaymentProvider>` in its constructor. Spring scans every class in the package at startup, collects every bean that implements `PaymentProvider`, and passes them all in as that list. `ProviderRegistry` never calls `new` on a provider. Adding Klarna means creating one new class annotated with `@Component` that implements `PaymentProvider` — nothing else changes.

For a provider to end up in that list, two conditions must both be true:
- It implements `PaymentProvider`
- It is annotated with `@Component`

Missing either one and Spring won't include it.

**Production scenario:** When the payment service starts up, Spring scans every class in the project. It finds `StripeProvider` and `AdyenProvider` annotated with `@Component`, creates one instance of each, and puts them in the application context. When `PaymentServiceTest` starts up and declares it needs a `ProviderRegistry`, Spring finds one in the context and hands it over automatically. None of this wiring is written by hand.

---

**Bean — what it is**

A bean is an object that Spring creates and manages. The word sounds fancy but it just means: Spring made this object, not you. Instead of `new StripeProvider()`, you put `@Component` on the class and Spring handles the rest.

```java
@Component
public class StripeProvider {
    // Spring creates one instance of this when the app starts
}
```

**Application context — what it is**

The application context is the container that holds all beans. Think of it as a map — Spring puts every bean into it at startup, keyed by type. When something needs a `StripeProvider`, Spring looks it up in the context and gives it.

When your app starts, this is what happens in order:
1. `@SpringBootApplication` triggers a scan of all classes in the package
2. Spring finds every class annotated with `@Component`, `@Service`, `@Repository`, `@RestController`, or `@Configuration`
3. Spring creates one instance of each and puts it in the application context
4. Spring wires dependencies — if `PaymentServiceTest` needs a `ProviderRegistry`, Spring finds it in the context and passes it in. This is true for any bean — if a class is annotated with `@Component`, `@Service`, `@Repository`, or `@RestController`, Spring will inject its dependencies automatically via the constructor
5. The app starts serving requests

**Singleton by default**

Spring creates exactly one instance of each bean and reuses it everywhere. If three different classes all need a `ProviderRegistry`, they all get the same instance — not three separate ones. This is called singleton scope and is the default.

This is why you should never store request-specific data in a bean's fields — the bean is shared across all requests. If two payment requests come in at the same time and both write to the same field, they overwrite each other:

```java
@Component
public class StripeProvider {
    private BigDecimal currentAmount;  // WRONG — shared across all requests

    public String charge(BigDecimal amount, PaymentMethod method) {
        this.currentAmount = amount;  // request 1 sets 100.00...
        // ...request 2 sets 200.00 before request 1 finishes
        // request 1 now uses 200.00 — silent bug
    }
}
```

Keep bean fields for things that are the same for every request: config values, injected dependencies, shared resources. Request-specific data belongs in method parameters and local variables — they're created fresh for every call and thrown away when the method returns.

**`@SpringBootApplication` — the entry point**

The generated `PaymentServiceApplication.java` has this annotation. It does three things in one:
- Marks this class as a configuration source
- Triggers the component scan — Spring looks at every class in `com.example.payment_service` and its sub-packages for annotations
- Enables Spring Boot's auto-configuration — Spring Boot automatically sets up common things (web server, database connection) based on what dependencies are on the classpath

You only ever have one of these in your app.

---

### Dependency injection

**The full picture:**

A bean rarely works alone. `PaymentServiceTest` needs a `ProviderRegistry` to find the right provider. `ProviderRegistry` needs a list of all providers. Without Spring, you'd wire these together manually with `new` — and every time something changes you'd have to update that wiring code by hand.

Dependency injection means Spring does the wiring for you. When `ProviderRegistry` declares it needs a `List<PaymentProvider>` in its constructor, Spring finds every bean that implements `PaymentProvider` and passes them all in. When `PaymentServiceTest` declares it needs a `ProviderRegistry`, Spring finds the one it already created and passes it in. You never call `new` on your own service classes.

**Production scenario:** A payment request comes in for "Stripe". `PaymentServiceTest` asks `ProviderRegistry` to find the Stripe provider. `ProviderRegistry` searches its list — which Spring filled at startup with `StripeProvider` and `AdyenProvider` — finds `StripeProvider`, and returns it. `PaymentServiceTest` calls `charge()` on it. None of this wiring was written by hand.

---

**Constructor injection — the preferred way**

There are multiple ways to inject dependencies in Spring, but constructor injection is the standard. You declare the dependency as a constructor parameter and Spring passes it in when creating the bean:

```java
@Component
public class ProviderRegistry {
    private final List<PaymentProvider> providers;

    public ProviderRegistry(List<PaymentProvider> providers) {
        this.providers = providers;  // Spring passes all PaymentProvider beans as a list
    }
}
```

```java
@Service
public class PaymentService {
    private final ProviderRegistry registry;

    public PaymentService(ProviderRegistry registry) {
        this.registry = registry;  // Spring injects the ProviderRegistry bean
    }
}
```

The other common approach is field injection — Spring injects directly into the field using `@Autowired`, no constructor needed:

```java
@Service
public class PaymentService {
    @Autowired
    private ProviderRegistry registry;  // Spring sets this after the object is created
}
```

This looks simpler but has drawbacks — which is why constructor injection is preferred:
- Dependencies are explicit — you can see exactly what a class needs just by looking at its constructor. With field injection they're hidden inside the class body.
- Fields can be `final` — with constructor injection the field is set during construction so `final` is possible. With field injection Spring sets it after construction, so `final` is not allowed.
- Easy to test — with constructor injection you call `new PaymentService(fakeRegistry)` directly. With field injection there's no constructor to use, so testing requires Spring or a mocking framework to inject the dependency.

**`private final` — why both**

`private` — nothing outside the class can access or replace the dependency directly. `final` — it's set once in the constructor and can never be reassigned. Together they mean: Spring sets this once at startup and it stays that way for the life of the app.

Without `final`, nothing stops another method from accidentally reassigning the dependency later:

```java
// without final — risky
private ProviderRegistry registry;

public void someMethod() {
    this.registry = null;  // compiles fine — silent bug
}

// with final — safe
private final ProviderRegistry registry;

public void someMethod() {
    this.registry = null;  // compile error — caught immediately
}
```

**Spring's special behaviour with `List<SomeInterface>`**

When Spring sees a constructor parameter of type `List<PaymentProvider>`, it doesn't look for a single `List` bean — it collects every bean that implements `PaymentProvider` and passes them all as a list. So `StripeProvider` and `AdyenProvider` both end up in the list automatically. Adding a new provider means creating one new `@Component` class — nothing else changes.

Without this, you'd have to build the list manually and update it every time a provider is added:

```java
// without Spring's list injection — manual and fragile
public ProviderRegistry() {
    this.providers = List.of(new StripeProvider(), new AdyenProvider());
    // adding Klarna means editing this line
}

// with Spring's list injection — automatic
public ProviderRegistry(List<PaymentProvider> providers) {
    this.providers = providers;
    // Spring passes [StripeProvider, AdyenProvider] automatically
    // adding Klarna means only creating KlarnaProvider — nothing else changes
}
```

For this to work, two conditions must both be true:
- The class implements `PaymentProvider`
- The class is registered as a Spring bean — any bean annotation works (`@Component`, `@Service`, etc.), but `@Component` is the right choice for provider classes

**Optional — used when a dependency might not be found**

`findByName()` on `ProviderRegistry` returns `Optional<PaymentProvider>` because the provider might not exist. Stream's `findFirst()` returns an `Optional` automatically — if nothing matches, the Optional is empty. The caller then decides what to do: throw an exception, return a fallback, or handle the empty case explicitly.

Without Optional, returning `null` means a NullPointerException somewhere downstream — with no indication of where it came from:

```java
// without Optional — returns null if not found
public PaymentProvider findByName(String name) {
    return providers.stream()
        .filter(p -> p.getName().equals(name))
        .findFirst()
        .orElse(null);  // caller might forget to check for null
}

// caller forgets null check — NullPointerException at runtime
PaymentProvider provider = registry.findByName("Klarna");
provider.charge(amount, method);  // crashes if Klarna doesn't exist
```

```java
// with Optional — forces the caller to handle the empty case
public Optional<PaymentProvider> findByName(String name) {
    return providers.stream()
        .filter(p -> p.getName().equals(name))
        .findFirst();
}

// caller is forced to decide what to do when it's not found
PaymentProvider provider = registry.findByName("Klarna")
    .orElseThrow(() -> new ProviderNotFoundException("Klarna"));
```

*Reuses: Streams, Optional*

---

### Core annotations

**The full picture:**

`@Component` is the generic annotation that registers any class as a Spring bean. But Spring also has annotations that say what *role* the bean plays in the application. Most of them do real work — they're not just labels:

- `@Repository` adds database exception translation on top of registering the bean. This means Spring catches low-level database exceptions (like JDBC `SQLException`) and converts them into Spring's own exception hierarchy — so instead of a cryptic driver-level error you get a meaningful `DataAccessException` that's easier to handle and log
- `@RestController` combines `@Controller` and `@ResponseBody`. Without `@ResponseBody`, Spring treats the return value as a view name (a template file to render) — not JSON. With `@RestController` it serializes the return value to JSON automatically. If you accidentally used `@Controller` alone, your endpoint would return a 404 or an error trying to find a template file instead of JSON
- `@Configuration` tells Spring this class contains `@Bean` methods to call at startup

The one case where two annotations are truly identical in behavior is `@Service` vs `@Component` — same outcome, different label. The label still matters for readability: when you see `@Service` you immediately know this class contains business logic, without reading a single method.

Using the right annotation makes the codebase readable — any developer can scan the class list and understand the architecture without reading the code.

**Production scenario:** A new developer joins the team and opens the payment service. They see `@RestController` on `PaymentController`, `@Service` on `PaymentServiceTest`, `@Repository` on `TransactionRepository`, and `@Component` on `ProviderRegistry`. Without reading a single method they already know what each class does and how they fit together.

---

**The annotations and when to use them:**

| Annotation | Use when |
|---|---|
| `@Component` | The class doesn't fit any of the categories below — a utility, a registry, a supporting component |
| `@Service` | The class contains business logic — orchestrates operations, makes decisions, coordinates other beans |
| `@Repository` | The class talks to the database — reads and writes data. Spring also adds database exception translation here |
| `@RestController` | The class handles HTTP requests and returns data (not a view). Combines `@Controller` and `@ResponseBody` |
| `@Configuration` | The class contains bean definitions — methods annotated with `@Bean` that create and configure objects |

**`@Bean` — registering third-party objects**

You can't put `@Component` on a class you don't own — like `RestTemplate` from Spring or `ObjectMapper` from Jackson. Instead you create a `@Configuration` class and define a `@Bean` method that returns the object. Spring calls that method at startup and puts the result in the application context:

```java
@Configuration
public class AppConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

Now any bean that needs a `RestTemplate` can declare it as a constructor parameter and Spring will inject it. The result is exactly the same as `@Component` — one singleton instance, managed by Spring, injectable via constructor, and swappable in tests by passing a different implementation.

**In your payment service:**
- `StripeProvider`, `AdyenProvider` → `@Component` — they're provider implementations, not services or repositories
- `ProviderRegistry` → `@Component` — a supporting component that manages the provider list
- `PaymentServiceTest` → `@Service` — contains the business logic for processing payments
- `TransactionRepository` → `@Repository` — talks to the database (Step 5)
- `PaymentController` → `@RestController` — handles HTTP requests (Step 6)
- `AppConfig` → `@Configuration` — defines the `RestTemplate` bean

---

### Spring Data JPA

**The full picture:**

Every payment attempt needs to be saved somewhere permanent. When a merchant calls your API, you process the payment and store the result as a row in the database — the amount, which provider handled it, whether it succeeded or failed, and when it happened. That record is the audit trail.

Without JPA you'd write SQL directly — `INSERT INTO transactions (amount, status...) VALUES (...)`. With JPA you annotate a Java class with `@Entity` and Hibernate (the JPA implementation Spring Boot uses) handles the SQL for you. You work with Java objects; Hibernate translates them to database rows and back.

Spring Data JPA takes it a step further — you define a repository interface and Spring generates all the standard database operations automatically. No SQL needed for `save()`, `findById()`, `findAll()`.

**Production scenario:** A payment request comes in. `PaymentServiceTest` creates a `Transaction` object, sets its status to `PENDING`, and saves it. It then calls the provider. On success it updates the status to `COMPLETED` and saves again. On failure it sets `FAILED` and saves.

In this project, only the final state of the transaction is stored — one row updated in place. In a real production system you'd also have an audit trail: either a separate table that records every state change with a timestamp, or a log aggregation tool like Elasticsearch (which is what PaymentIQ uses). That way a merchant support agent can see not just the final outcome but every step the transaction went through and when.

---

**`@Entity` — mapping a class to a database table**

Annotating a class with `@Entity` tells JPA this class maps to a table. Without it, JPA ignores the class completely — no table is created and calling `repository.save()` throws an error at runtime. Hibernate reads the class at startup and creates or updates the table to match:

```java
@Entity
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String providerName;
    private BigDecimal amount;
}
```

- `@Id` — marks the primary key field. Every entity must have one.
- `@GeneratedValue(strategy = GenerationType.IDENTITY)` — tells the database to auto-increment the ID. You never set it yourself — it's filled in after the row is saved.
- All other fields map to columns automatically by name — `providerName` becomes the `provider_name` column. No `@Column` annotation needed unless you want to customize the column name.

**`@Enumerated(EnumType.STRING)` — storing enums as text**

When you set `tx.setStatus(TransactionStatus.PENDING)` and save, JPA needs to write something to the database column. By default it calls `.ordinal()` on the enum value — which returns its position in the declaration order — and stores that number:

```java
enum TransactionStatus {
    PENDING,    // ordinal 0
    COMPLETED,  // ordinal 1
    FAILED      // ordinal 2
}

// without @Enumerated:
tx.setStatus(TransactionStatus.PENDING);
repository.save(tx);  // stores 0 in the database
```

This is fragile — if you ever reorder the enum or insert a new value between existing ones, all existing rows become wrong. `COMPLETED` was `1`, now it's `2`, but the database still has `1` everywhere.

`@Enumerated(EnumType.STRING)` tells JPA to call `.name()` instead of `.ordinal()` — storing the string `"PENDING"` rather than `0`. Reordering the enum never affects existing data:

```java
@Enumerated(EnumType.STRING)
private TransactionStatus status;  // stores "PENDING", "COMPLETED", "FAILED"
```

**`@PrePersist` — setting values automatically before saving**

Some fields should always be set at the moment a record is created — like a timestamp. You could set it manually every time you create a `Transaction`, but that's easy to forget and leads to missing data in the database:

```java
// without @PrePersist — easy to forget
Transaction tx = new Transaction();
tx.setStatus(TransactionStatus.PENDING);
tx.setAmount(new BigDecimal("100.00"));
// forgot to set createdAt — now it's null in the database
repository.save(tx);
```

`@PrePersist` solves this by running a method automatically just before the entity is saved for the first time — JPA calls it for you, no matter who creates the object or where in the code it happens:

```java
@Entity
public class Transaction {
    private LocalDateTime createdAt;

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();  // JPA calls this automatically before save
    }
}

// now you just create and save — createdAt is always set
Transaction tx = new Transaction();
tx.setStatus(TransactionStatus.PENDING);
tx.setAmount(new BigDecimal("100.00"));
repository.save(tx);  // onCreate() runs automatically, createdAt is set
```

You never call `onCreate()` yourself — JPA calls it automatically every time a new `Transaction` is about to be saved. The timestamp is always set, guaranteed.

---

**Spring Data JPA repositories**

A repository is the connection to the database. Without Spring Data JPA you'd write all database operations manually — JDBC boilerplate for every query:

```java
// without Spring Data JPA — manual JDBC for every operation
String sql = "SELECT * FROM transaction WHERE id = ?";
PreparedStatement stmt = connection.prepareStatement(sql);
stmt.setLong(1, id);
ResultSet rs = stmt.executeQuery();
Transaction tx = new Transaction();
tx.setId(rs.getLong("id"));
tx.setStatus(TransactionStatus.valueOf(rs.getString("status")));
// ... map every column manually
```

With Spring Data JPA you define an interface that extends `JpaRepository` — Spring generates all of this for you automatically at startup:

```java
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
```

The two type parameters: `Transaction` is the entity type, `Long` is the type of its ID. Spring generates `save()`, `findById()`, `findAll()`, `delete()` and more — no implementation needed.

**Custom query methods — derived from method names**

Spring reads the method name and generates the SQL for you. Without it you'd have to write the query manually using a `@Query` annotation:

```java
// without derived query — manual SQL
@Query("SELECT t FROM Transaction t WHERE t.status = :status")
List<Transaction> findByStatus(TransactionStatus status);

// with derived query — Spring reads the method name and generates the SQL
List<Transaction> findByStatus(TransactionStatus status);
```

Spring sees `findByStatus` and generates `SELECT * FROM transaction WHERE status = ?`. The method name is the query. No implementation needed.

**Common repository methods:**
- `repository.save(entity)` — insert or update. Returns the saved entity with the generated ID filled in.
- `repository.findById(id)` — returns `Optional<Transaction>` — might not exist
- `repository.findAll()` — returns all rows as a `List`
- `repository.delete(entity)` — delete a row

**JPA requires a no-argument constructor**

When reading a row from the database, JPA creates an empty object first and then fills in the fields one by one. To do this it needs to call a constructor with no arguments. Without it, JPA throws an exception at runtime when trying to load any row:

```java
// without no-arg constructor — JPA throws at runtime when loading from DB
public class Transaction {
    public Transaction(String providerName, BigDecimal amount) { ... }
    // JPA can't use this — it has parameters
}

// with no-arg constructor — JPA can create an empty instance and fill fields in
public class Transaction {
    public Transaction() {}  // required by JPA
    public Transaction(String providerName, BigDecimal amount) { ... }  // you can still have others
}
```

---

### JPA relationships

**The full picture:**

In a real payment system, entities don't exist in isolation. A merchant has many transactions. A transaction belongs to one merchant. Without JPA relationships you'd have to fetch them manually — load the merchant, then run a separate query for their transactions, then assign them to the merchant object yourself. JPA handles all of this for you by declaring the relationship with annotations — load the merchant and its transactions come with it automatically.

**Production scenario:** A support agent looks up a merchant. The app loads the `Merchant` entity and JPA automatically knows to also fetch all `Transaction` rows that belong to that merchant. The agent sees the full transaction history without any extra queries being written.

---

**`@ManyToOne` and `@OneToMany` — the most common relationship**

A merchant has many transactions. A transaction belongs to one merchant. This is a one-to-many relationship — one merchant, many transactions.

The foreign key (the column that links the two tables) lives on the `Transaction` side — each transaction row has a `merchant_id` column pointing to its merchant:

```java
@Entity
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "merchant_id")  // the foreign key column in the transaction table
    private Merchant merchant;
}
```

```java
@Entity
public class Merchant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "merchant")  // "merchant" is the field name in Transaction
    private List<Transaction> transactions;
}
```

- `@ManyToOne` on `Transaction` — many transactions can point to one merchant
- `@OneToMany` on `Merchant` — one merchant has many transactions
- `@JoinColumn(name = "merchant_id")` — tells JPA what the foreign key column is called in the database. Without this JPA would generate a name automatically which may not match your schema.
- `mappedBy = "merchant"` — tells JPA that `Transaction.merchant` is the field that owns the relationship (i.e. where the foreign key lives). This prevents JPA from creating a duplicate join table.

**`@OneToOne` — one entity linked to exactly one other**

A transaction linked to exactly one payment request — and that request linked back to exactly one transaction:

```java
@Entity
public class Transaction {
    @OneToOne
    @JoinColumn(name = "payment_request_id")  // foreign key in the transaction table
    private PaymentRequest paymentRequest;
}
```

Same as `@ManyToOne` in structure — `@JoinColumn` names the foreign key column. The difference is just the cardinality: one-to-one instead of many-to-one.

---

**`@JoinColumn` — what it does**

`@JoinColumn` tells JPA which column in the database table holds the foreign key for this relationship. Without it JPA generates a column name automatically — which is fine in a new project, but in an existing database with a fixed schema you need to specify the exact column name.

The column does not need to exist beforehand. When `ddl-auto: update` is set, Hibernate reads the annotation on startup and creates the column automatically — the same mechanism that creates the table itself. So adding `@JoinColumn(name = "merchant_id")` to `Transaction` is what causes a `merchant_id` column to appear in the `transaction` table after the next restart.

```java
@JoinColumn(name = "merchant_id")
// → JPA creates a column called "merchant_id" in the transaction table
// → that column holds the value of whatever field has @Id on Merchant
```

The name you give `@JoinColumn` only controls what the column is called in the database — it has no connection to what the primary key field on the related entity is named. JPA always links to the field annotated with `@Id` on the other side, regardless of what that field is called. You could write `@JoinColumn(name = "banana")` and it would create a `banana` column that still correctly holds the merchant's primary key value.

---

**Lazy vs eager loading — when JPA fetches related data**

When you load a `Merchant`, JPA doesn't automatically run a second query for its transactions unless you tell it to. This is controlled by the fetch strategy:

- **Lazy loading (default for `@OneToMany`)** — JPA only fetches the transactions when you actually access `merchant.getTransactions()`. If you never access it, no query is run. Good for performance when you don't always need the related data.
- **Eager loading (default for `@ManyToOne`)** — JPA fetches the related entity immediately when the parent is loaded. A transaction always loads its merchant at the same time.

```java
// lazy — transactions only loaded when accessed
@OneToMany(mappedBy = "merchant", fetch = FetchType.LAZY)
private List<Transaction> transactions;

// eager — transactions always loaded with the merchant
@OneToMany(mappedBy = "merchant", fetch = FetchType.EAGER)
private List<Transaction> transactions;
```

In practice: use lazy loading for collections (`@OneToMany`) — a merchant might have thousands of transactions and you don't want to load all of them every time you load the merchant. Eager is fine for single entities (`@ManyToOne`) since it's just one row.

Without lazy loading, loading one merchant with 10,000 transactions would load all 10,000 transaction rows every single time — even if you only needed the merchant's name.

---

### REST

**The full picture — how HTTP endpoints work in Spring Boot:**

A merchant's cashier system sends an HTTP request to your app — for example `POST /payments` with a JSON body containing the provider name, amount, and payment method. Your app needs to receive that request, read the data from it, call `PaymentServiceTest`, and send a response back. In Spring Boot, a `@RestController` class is what receives HTTP requests. Each method in it is mapped to a specific URL and HTTP method (POST, GET, etc.). Spring reads the incoming JSON and converts it into a Java object automatically — you just declare what type you expect. When you return a Java object, Spring converts it back to JSON automatically.

**Without Spring REST — what you'd have to do manually:**

Without Spring, you'd have to read raw HTTP request bytes, parse the JSON yourself, figure out which URL was called, route to the right method, and write the response headers and body manually. Spring handles all of that — you just write normal Java methods and annotate them.

**`@RestController` — marking a class as an HTTP handler**

`@RestController` tells Spring: "this class handles HTTP requests." Every public method in it can be mapped to an endpoint. Without this annotation, Spring ignores the class entirely — it's just a plain Java class that no HTTP request will ever reach.

```java
// Without @RestController — Spring ignores this class, no endpoints exposed
public class PaymentController {
    public Transaction processPayment(PaymentRequest request) { ... }
}

// With @RestController — Spring exposes this class's methods as HTTP endpoints
@RestController
public class PaymentController {
    public Transaction processPayment(PaymentRequest request) { ... }
}
```

**`@RequestMapping` — setting a base URL for all endpoints in the class**

`@RequestMapping("/payments")` on the class means every endpoint inside starts with `/payments`. So `@PostMapping` becomes `POST /payments`, and `@GetMapping("/{id}")` becomes `GET /payments/{id}`. You don't repeat the base path on every method.

**`@PostMapping` / `@GetMapping` — mapping a method to an HTTP verb and URL**

`@PostMapping` maps a method to `POST` requests. `@GetMapping` maps to `GET` requests. The annotation value is the path relative to the class-level `@RequestMapping`:

```java
@RestController
@RequestMapping("/payments")
public class PaymentController {

    @PostMapping           // handles POST /payments
    public Transaction create(...) { ... }

    @GetMapping("/{id}")   // handles GET /payments/42
    public Transaction getById(...) { ... }

    @GetMapping            // handles GET /payments?status=COMPLETED
    public List<Transaction> getByStatus(...) { ... }
}
```

**`@RequestBody` — reading the JSON body of a POST request**

When a cashier sends `POST /payments`, the payment details (provider, amount, method) come in the request body as JSON. `@RequestBody` tells Spring to read that JSON and convert it into the Java object type you declare. This conversion happens automatically — Spring uses Jackson (a JSON library included with Spring Boot) to do it. The Java class must have a no-arg constructor and getters/setters for Jackson to be able to fill in the fields.

```java
// Incoming JSON: {"providerName": "Stripe", "amount": 100.00, "paymentMethod": "CARD"}
@PostMapping
public Transaction create(@RequestBody PaymentRequest request) {
    // request.getProviderName() == "Stripe"
    // request.getAmount() == 100.00
    return paymentServiceTest.processPayment(request.getProviderName(), request.getAmount(), request.getPaymentMethod());
}
```

Without `@RequestBody`, the `request` parameter would be null — Spring wouldn't know to read the body.

**`@PathVariable` — reading a value from the URL path**

`GET /payments/42` — the `42` is part of the URL path. `@PathVariable` tells Spring to extract it and pass it as a method parameter. You declare the placeholder in the mapping with `{name}` and use the same name in `@PathVariable`:

```java
@GetMapping("/{id}")
public Transaction getById(@PathVariable Long id) {
    // id == 42 when the URL is GET /payments/42
    return transactionRepository.findById(id)
        .orElseThrow(() -> new TransactionNotFoundException(id));
}
```

Spring automatically converts the URL string `"42"` to a `Long` — you don't parse it yourself.

**The core difference: one vs many**

This is the rule that tells you which one to use:

- `@PathVariable` — you are identifying **one specific resource**. The caller already knows which one they want (e.g. transaction 42). The response is always one item or a 404.
- `@RequestParam` — you are **filtering**. The caller doesn't name a specific item — they describe a condition. The response can be zero, one, or many items depending on what matches.

Real example: `GET /payments/42` — the caller wants exactly transaction 42. That's a path variable.
Real example: `GET /payments?status=COMPLETED` — the caller wants all completed transactions. That's a query param — there might be 0, 5, or 500 results.

**`@RequestParam` — reading a query parameter**

`GET /payments?status=COMPLETED` — the `status=COMPLETED` part is a query parameter (everything after the `?`). `@RequestParam` tells Spring to read it and pass it as a method parameter. Spring also converts it to the right type automatically — including enums:

```java
@GetMapping
public List<Transaction> getByStatus(@RequestParam TransactionStatus status) {
    // status == TransactionStatus.COMPLETED when the URL is GET /payments?status=COMPLETED
    // Spring reads the string "COMPLETED" and converts it to the enum value automatically
    return transactionRepository.findByStatus(status);
}
```

**What Spring returns to the caller**

When you return a Java object from a `@RestController` method, Spring automatically converts it to JSON and sends it as the HTTP response body with a 200 status code. You don't write any response-handling code — just return the object.

```java
// You return this Java object:
return transaction; // Transaction{id=1, status=COMPLETED, amount=100.00, ...}

// Spring sends this JSON response:
// {"id": 1, "status": "COMPLETED", "amount": 100.00, ...}
```

**Production scenario:** A merchant's cashier sends `POST /payments` with the payment details. `PaymentController` receives it, calls `PaymentService.processPayment()`, gets a `Transaction` back, and returns it. Spring converts it to JSON. The cashier reads the JSON, checks `status`, and shows the customer "Payment successful" or "Payment failed." The cashier never talks directly to `PaymentServiceTest` — it only talks to `PaymentController` over HTTP.

---

### JSON & XML serialization

When your app talks to external payment providers, two things happen:

1. You send data **to** the provider — a charge request, an amount, a card token
2. You receive data **from** the provider — a response with a status, a transaction ID, an error message

Both directions involve converting between Java objects and a text format — either JSON or XML. This conversion is called **serialization** (Java → text) and **deserialization** (text → Java). In Spring Boot, a library called **Jackson** handles this automatically.

---

**What is `ObjectMapper`?**

`ObjectMapper` is Jackson's main class — it's the tool that does the actual converting. You call `objectMapper.readValue(jsonString, MyClass.class)` to turn a JSON string into a Java object, and `objectMapper.writeValueAsString(myObject)` to turn a Java object into a JSON string.

Spring Boot automatically creates an `ObjectMapper` bean for you — you don't need to define it in `AppConfig`. You just add it as a constructor parameter and Spring injects it. This is different from `RestTemplate`, which Spring Boot does NOT create automatically because there are many ways to configure it (timeouts, interceptors, etc.) — so you registered that one yourself in `AppConfig`.

The rule of thumb: if the app starts without errors when you inject something, Spring already has a bean for it. If it fails with "No qualifying bean of type X found", you need to create it in `AppConfig`.

---

**How Jackson works in your app**

You've already seen Jackson in action:

- When you return a `Transaction` from a controller method, Spring calls Jackson to convert it to JSON and send it as the response body
- When a POST request arrives with a JSON body, Spring calls Jackson to read it and fill in your `PaymentRequest` object

The same thing happens with provider responses — when `restTemplate.postForObject()` gets a response back from Stripe, Jackson reads the JSON and fills in your `StripeChargeResponse` object.

Jackson matches JSON keys to Java field names. If the JSON has `"status": "succeeded"` and your class has `private String status`, Jackson maps them automatically because the names match. No configuration needed.

---

**When names don't match — `@JsonProperty`**

Providers don't always use camelCase. Stripe's API returns snake_case:

```json
{
  "charge_id": "ch_abc123",
  "payment_status": "succeeded",
  "created_at": 1234567890
}
```

Your Java class should follow Java conventions — camelCase fields. But `chargeId` won't match `"charge_id"`. Jackson finds no match and leaves the field null.

`@JsonProperty` is the fix — it tells Jackson which JSON key maps to which Java field:

```java
public class StripeChargeResponse {
    @JsonProperty("charge_id")
    private String chargeId;

    @JsonProperty("payment_status")
    private String paymentStatus;
}
```

Now Jackson reads `"charge_id"` from the JSON and puts it into `chargeId`. Your Java code stays clean, the mapping is explicit, and anyone reading the class immediately knows what Stripe calls each field.

**Without `@JsonProperty`:** fields stay null silently — no error, just missing data. Hard to debug.

**With `@JsonProperty`:** explicit mapping, correct values, readable code.

---

**When providers send extra fields — `@JsonIgnoreProperties`**

Real provider APIs return many more fields than you need. Stripe's charge response has 30+ fields. You only care about 3. Without any configuration, Jackson throws an error when it sees a field in the JSON that has no matching field in your class.

`@JsonIgnoreProperties(ignoreUnknown = true)` on the class tells Jackson to silently skip any field it doesn't recognize:

```java
@JsonIgnoreProperties(ignoreUnknown = true)
public class StripeChargeResponse {
    private String id;
    private String status;
    private BigDecimal amount;
    // Jackson ignores the other 27 fields Stripe sends
}
```

**Without it:** Jackson throws `UnrecognizedPropertyException` the moment Stripe sends a field your class doesn't have.

**With it:** Jackson maps what it can, silently ignores the rest.

---

**Parsing XML — Adyen**

Not all providers use JSON. Older Adyen flows return XML:

```xml
<response>
    <id>adyen_abc123</id>
    <status>Authorised</status>
    <amount>100.00</amount>
</response>
```

Jackson can parse XML too, but you need a different mapper class — `XmlMapper` instead of `ObjectMapper`. `XmlMapper` is a subclass of `ObjectMapper` that understands XML structure instead of JSON.

You also need to tell the XML parser what the root tag is called — `@JacksonXmlRootElement(localName = "response")` matches the `<response>` tag in the XML above:

```java
@JacksonXmlRootElement(localName = "response")
public class AdyenChargeResponse {
    private String id;
    private String status;
    private BigDecimal amount;
}
```

Then to parse it:

```java
XmlMapper xmlMapper = new XmlMapper();
AdyenChargeResponse response = xmlMapper.readValue(responseBody, AdyenChargeResponse.class);
```

**Why create `XmlMapper` locally instead of injecting it?** Because it's only needed in one place — Adyen's XML flow. There's no reason to make it a shared bean.

---

**Provider-specific response classes — where they live**

Each provider has its own response format. `StripeChargeResponse` knows Stripe's field names. `AdyenChargeResponse` knows Adyen's XML structure. These classes are specific to their provider — they live in the provider's own sub-package, not in a shared `dto/` folder:

```
provider/stripe/StripeChargeResponse.java
provider/adyen/AdyenChargeResponse.java
```

This keeps each integration self-contained. If Stripe changes their response format, you update `StripeChargeResponse` — nothing else is affected.

---

**Production scenario:** Your service sends a charge request to Stripe. Stripe responds with JSON containing 30+ fields. Jackson reads it into `StripeChargeResponse` — which only declares 3 fields and ignores the rest via `@JsonIgnoreProperties`. You read `response.getStatus()`, get `"succeeded"`, set the transaction to `COMPLETED`, and save it. The provider's response format is fully contained in one class and never leaks into the rest of your app.

---

### Profiles & config

**The full picture:**

Your app runs in multiple environments — on your laptop during development, and on a server in production. The database in development is local. The database in production is a real server with real data. The Stripe API key in development is a test key that charges fake money. In production it's a live key that charges real money.

If you hardcode these values — database URL, API keys, provider URLs — into your code or a single config file, you have to manually change them every time you switch environments. You'll forget, use the wrong key, or accidentally commit a production secret to git.

Spring profiles solve this. A profile is a name you give to a configuration — `dev`, `prod`, `staging`. You create a separate config file per profile (`application-dev.yaml`, `application-prod.yml`) and activate the right one at startup. Only that profile's file is loaded. Your code doesn't change — just the config.

And for sensitive values like API keys and database passwords, profiles alone aren't enough. You still can't commit a prod password to a yml file — even in a private repo, secrets in version control is a security risk. The solution: use environment variables. Your yml file says `${STRIPE_API_KEY}` — Spring reads the actual value from the server's environment at startup. The value never lives in the codebase.

**Production scenario:** A payment service has three environments: dev (your laptop), staging (a test server that mirrors prod), and prod (real customers). The dev config points at a local Postgres database and uses Stripe's test API key. The prod config reads the database URL and API key from environment variables set on the server. The same code ships to all three environments — only the config files differ.

---

**`application.yml` — the base config**

This file is always loaded, regardless of which profile is active. It contains config that is the same in every environment — the app name, server port, defaults that don't change. Environment-specific config goes in the profile files.

---

**Profile config files — `application-{profile}.yml`**

Spring loads `application-dev.yaml` when the `dev` profile is active, `application-prod.yml` when `prod` is active. These files are loaded on top of the base `application.yml` — values in the profile file override values from the base if they overlap.

```yaml
# application-dev.yaml — only loaded when dev profile is active
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/payment_service
  jpa:
    hibernate:
      ddl-auto: update
providers:
  stripe:
    base-url: https://api.stripe.com
    api-key: sk_test_placeholder
  adyen:
    base-url: https://api.adyen.com
    api-key: adyen_test_placeholder
```

```yaml
# application-prod.yml — only loaded when prod profile is active
spring:
  datasource:
    url: ${DATABASE_URL}       # reads the DATABASE_URL environment variable
  jpa:
    hibernate:
      ddl-auto: validate       # in prod: only validate schema, never modify it
providers:
  stripe:
    api-key: ${STRIPE_API_KEY} # reads the STRIPE_API_KEY environment variable
  adyen:
    api-key: ${ADYEN_API_KEY}
```

`${VARIABLE_NAME}` is Spring's syntax for reading an environment variable. At startup, Spring looks for a system environment variable with that name. If it's not set, the app refuses to start — intentional in production, because running without a real API key would be worse than not running at all.

Note `ddl-auto: validate` in prod — this tells Hibernate to only check that the database schema matches your entity classes, but never change anything. In dev you use `update` which creates/modifies tables automatically. In production you never let the app modify the schema — database changes go through controlled migrations (covered later).

**How to activate a profile and set environment variables in IntelliJ:**

1. Click the dropdown next to the green run button at the top of IntelliJ → **Edit Configurations**
2. Find your Spring Boot run configuration in the left panel
3. In the **Active profiles** field: type `dev`
4. In the **Environment variables** field: add your variables separated by semicolons:
   ```
   STRIPE_API_KEY=sk_test_yourkeyhere;ADYEN_API_KEY=adyen_test_yourkeyhere
   ```
5. Click OK and run the app normally

Spring reads `${STRIPE_API_KEY}` in the yml, looks for an environment variable with that name, finds the value you set in the run config, and injects it into `ProviderProperties`. If the variable is not set and the yml references it with `${}`, the app fails to start with a binding error.

---

**`@ConfigurationProperties` — mapping yml values into a Java class**

Reading config values one at a time with `@Value("${providers.stripe.api-key}")` works, but it scales poorly. If a provider has 10 config values, you'd have 10 separate annotations. And there's no grouping — you can't tell which values belong together.

`@ConfigurationProperties` maps an entire block of yml into a typed Java class. You annotate the class with the prefix that matches the yml block, and Spring fills in all the fields automatically at startup:

```java
@Component
@ConfigurationProperties(prefix = "providers")
public class ProviderProperties {

    private Stripe stripe = new Stripe();
    private Adyen adyen = new Adyen();

    public static class Stripe {
        private String baseUrl;
        private String apiKey;

        public String getBaseUrl() { return baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }

    public static class Adyen {
        private String baseUrl;
        private String apiKey;

        public String getBaseUrl() { return baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }

    public Stripe getStripe() { return stripe; }
    public Adyen getAdyen() { return adyen; }
}
```

Spring reads the `providers:` block from the active yml file and maps:
- `providers.stripe.base-url` → `stripe.baseUrl`
- `providers.stripe.api-key` → `stripe.apiKey`

Note the name conversion: yml uses `kebab-case` (`base-url`), Java uses camelCase (`baseUrl`). Spring handles this automatically — you don't need to do anything.

**Without `@ConfigurationProperties`:** you'd scatter `@Value` annotations across every class that needs config, with no central place to see all config values for a provider.

**With `@ConfigurationProperties`:** one class holds all config for that block, it's typed (not strings scattered everywhere), and injecting it into a provider class is just one constructor parameter.

---

**`@PostConstruct` — running code after Spring has finished wiring a bean**

Sometimes you need to verify or initialize something after Spring has injected all dependencies — but before the app starts serving requests. `@PostConstruct` marks a method that Spring calls automatically once the bean is fully set up:

```java
@Component
public class StripeProvider extends AbstractPaymentProvider {

    private final ProviderProperties properties;

    public StripeProvider(RestTemplate restTemplate, ObjectMapper objectMapper, ProviderProperties properties) {
        this.properties = properties;
        this.apiUrl = properties.getStripe().getBaseUrl();
    }

    @PostConstruct
    public void logConfig() {
        String apiKey = properties.getStripe().getApiKey();
        String masked = apiKey.substring(0, 8) + "***";
        System.out.println("Stripe config loaded — baseUrl: " + properties.getStripe().getBaseUrl() + ", apiKey: " + masked);
    }
}
```

When the app starts you'll see the value printed in the console — confirming the environment variable was read correctly. Remove the log once you've verified it, since printing API keys in logs is a security risk in production.

**Without `@PostConstruct`:** you'd have no easy way to verify config was loaded correctly without sending a real request.

**With `@PostConstruct`:** Spring calls the method automatically at startup — before any requests arrive.

---

**Wiring config into providers**

Once `ProviderProperties` is a bean, inject it into `StripeProvider` and `AdyenProvider` via the constructor. Then replace the hardcoded `apiUrl` with the value from config:

```java
@Component
public class StripeProvider extends AbstractPaymentProvider {

    public StripeProvider(RestTemplate restTemplate, ObjectMapper objectMapper, ProviderProperties properties) {
        this.apiUrl = properties.getStripe().getBaseUrl();  // from yml, not hardcoded
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
}
```

Now changing the Stripe base URL in `application-dev.yaml` takes effect immediately — no code change needed.

---

### Spring Retry & error handling

**The full picture:**

External APIs fail. Stripe returns a 503. Adyen times out. The network drops for a second. These are not programming errors — they're expected failures in distributed systems. Your code can't prevent them, but it can handle them gracefully.

Two tools cover this in Spring Boot:

1. **Spring Retry** — automatically retries a failed method call a set number of times before giving up. You annotate the method and Spring handles the retry logic for you. Without it, you'd have to write retry loops manually in every provider class.

2. **`@ControllerAdvice`** — catches exceptions that escape your controllers and maps them to the correct HTTP response. Without it, any unhandled exception returns a generic 500 to the caller. With it, `ProviderNotFoundException` returns a 404, `TransactionNotFoundException` returns a 404, and any other exception returns a 500 — all defined in one place, applied to every endpoint automatically.

**Production scenario:** Your service calls Stripe. Stripe returns 503. Without retry, the transaction fails immediately and the merchant sees an error. With `@Retryable`, Spring calls `charge()` again automatically — up to 3 times. If all 3 fail, the `@Recover` method runs and marks the transaction as `FAILED` with a clear message. The controller never sees an exception — it gets a result either way. The global exception handler then ensures any unhandled errors return the right HTTP status to the caller.

---

**Dependencies — what's needed**

Spring Retry is not included in Spring Boot by default. Two dependencies are needed:

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

`spring-retry` is the retry library. `spring-aspects` provides the AOP (Aspect-Oriented Programming) support that Spring Retry uses under the hood to intercept method calls and retry them. Without `spring-aspects`, `@Retryable` is silently ignored — no error, no retry, no warning. Both are required.

You also need to enable retry globally by adding `@EnableRetry` to your main application class — the one annotated with `@SpringBootApplication`. Without this, `@Retryable` does nothing even if the dependencies are present.

---

**`@Retryable` — automatically retry a method on failure**

Add `@Retryable` above a method to make Spring retry it automatically when it throws an exception:

```java
@Retryable(retryFor = RuntimeException.class, maxAttempts = 3)
public String charge(BigDecimal amount, PaymentMethod method) {
    // if this throws RuntimeException, Spring calls it again
    // up to 3 times total before giving up
}
```

- `retryFor` — which exception type triggers a retry. `RuntimeException.class` covers all unchecked exceptions.
- `maxAttempts` — total number of attempts including the first one. `3` means: try once, if it fails try again, if it fails try once more, then give up.

Spring intercepts the method call, catches the exception, and calls the method again automatically. Your code inside `charge()` doesn't change — it just throws as normal. Spring handles the retry loop around it.

**Without `@Retryable`:** a single network hiccup fails the transaction permanently.

**With `@Retryable`:** transient failures are retried automatically. Only persistent failures (all 3 attempts fail) are treated as real errors.

---

**`@Recover` — what to do when all retries are exhausted**

When all retry attempts fail, Spring calls the method annotated with `@Recover` automatically. Without it, Spring rethrows the last exception after the retries run out.

The recover method must:
- Be in the same class as the `@Retryable` method
- Have the same return type as the retried method
- Take the exception as the first parameter, followed by the same parameters as the retried method in the same order

```java
@Recover
public String recover(RuntimeException e, BigDecimal amount, PaymentMethod method) {
    return "Stripe charge failed after retries: " + e.getMessage();
}
```

Spring matches `@Recover` to `@Retryable` by return type and parameter signature — if they don't match exactly, Spring can't find the recover method and will throw an error at runtime.

**Without `@Recover`:** after all retries fail, the last exception is rethrown and travels up to the caller.

**With `@Recover`:** after all retries fail, your recover method runs and returns a controlled result instead of throwing.

---

**`@ControllerAdvice` — global exception handling**

Without `@ControllerAdvice`, every unhandled exception that escapes a controller method becomes a 500 response — regardless of what the exception actually means. `ProviderNotFoundException` (a logical 404) returns 500. `TransactionNotFoundException` (also a 404) returns 500. The caller has no idea what went wrong.

`@ControllerAdvice` is a class that Spring watches. When an exception escapes any controller, Spring checks this class for a matching `@ExceptionHandler` method and calls it to build the response instead:

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProviderNotFoundException.class)
    public ResponseEntity<String> handleProviderNotFound(ProviderNotFoundException e) {
        return ResponseEntity.status(404).body(e.getMessage());
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<String> handleTransactionNotFound(TransactionNotFoundException e) {
        return ResponseEntity.status(404).body(e.getMessage());
    }
}
```

- `@ControllerAdvice` — marks this class as a global exception handler. Spring applies it to all controllers automatically.
- `@ExceptionHandler(SomeException.class)` — tells Spring to call this method when that specific exception type is thrown from any controller.
- `ResponseEntity<String>` — Spring's class for building an HTTP response with a specific status code and body. `ResponseEntity.status(404).body(e.getMessage())` sends a 404 with the exception message as the body.

**Without `@ControllerAdvice`:** all exceptions → 500, caller can't distinguish between "provider not found" and "database down".

**With `@ControllerAdvice`:** each exception maps to the correct HTTP status, defined once, applied everywhere.

---

**How the two work together**

```
charge() throws RuntimeException
    → @Retryable retries up to 3 times
    → all 3 fail → @Recover runs → returns "charge failed" string
    → PaymentService catches the failure, sets transaction to FAILED, rethrows
    → Exception escapes the controller
    → @ControllerAdvice catches it → returns correct HTTP status to caller
```

The retry layer handles transient failures. The exception handler layer handles what the caller sees when everything fails.

---

---

### Unit testing

A unit test tests one class in isolation. "Isolation" means: no real database, no real HTTP calls, no real Spring context. You give the class the inputs, it runs its logic, and you assert the output or behaviour.

**Why unit tests exist**

Imagine `PaymentService.processPayment()`. It has branching logic: success sets status to COMPLETED, failure sets it to FAILED and rethrows. You want to verify both branches work. If you test this by starting the full app and sending real HTTP requests, the test depends on a database being up, Stripe being reachable, Spring wiring correctly — any of those things failing kills your test, even if the logic itself is fine. A unit test removes all of that noise. It is fast (milliseconds, not seconds), deterministic, and tells you exactly what broke.

**Production scenario:** A new developer changes `processPayment()` and accidentally removes the `transactionRepository.save(transaction)` call on the failure path. Without a unit test, this goes unnoticed until someone checks the database manually. A unit test catches it immediately.

---

**Where test files live**

Tests go in `src/test/java/` — a separate source folder that Maven created when you generated the project. It mirrors the same package structure as `src/main/java/`. So a test for `PaymentServiceTest` (which lives in `com.example.payment_service.service`) goes here:

```
src/test/java/com/example/payment_service/service/PaymentServiceTest.java
```

Spring Boot includes JUnit 5 and Mockito in the test scope automatically — no extra dependencies needed.

---

**What a test class looks like — a full example**

Here is a complete unit test for `PaymentService.processPayment()` on the success path. Read through it once before writing anything — this is the structure you will follow for all three unit test assignments.

```java
package com.example.payment_service.service;

import com.example.payment_service.Repository.TransactionRepository;
import com.example.payment_service.model.PaymentMethod;
import com.example.payment_service.model.Transaction;
import com.example.payment_service.model.TransactionStatus;
import com.example.payment_service.provider.PaymentProvider;
import com.example.payment_service.provider.ProviderRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)   // activates Mockito — without this, @Mock and @InjectMocks are ignored
class PaymentServiceTest {

    @Mock
    ProviderRegistry providerRegistry;       // fake — you control what findProvider() returns

    @Mock
    TransactionRepository transactionRepository;  // fake — no real database

    @Mock
    PaymentProvider paymentProvider;         // fake — no real HTTP calls

    @InjectMocks
    PaymentService paymentServiceTest;           // the REAL class being tested — mocks above are injected into it

    @Test
    void processPayment_success_returnsCompletedTransaction() {
        // arrange — tell the mocks what to return when called
        when(providerRegistry.findProvider("Stripe")).thenReturn(Optional.of(paymentProvider));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentProvider.charge(any(), any())).thenReturn("ch_abc123");

        // act — call the real method
        Transaction result = paymentServiceTest.processPayment("Stripe", new BigDecimal("100"), PaymentMethod.CARD);

        // assert — verify the result
        assertEquals(TransactionStatus.COMPLETED, result.getStatus());
        verify(transactionRepository, times(2)).save(any(Transaction.class)); // PENDING save + COMPLETED save
    }
}
```

- `@ExtendWith(MockitoExtension.class)` — a JUnit plugin. Activates Mockito so `@Mock` and `@InjectMocks` work.
- `@Mock` — creates a fake version of that class. By default all its methods return null. You control what they return with `when(...).thenReturn(...)`.
- `@InjectMocks` — creates a real instance of `PaymentServiceTest` and automatically passes all `@Mock` fields into its constructor. This is why constructor injection matters — Mockito uses the constructor to inject the fakes.
- `thenAnswer(i -> i.getArgument(0))` — return whatever was passed as the first argument. Used for `save()` because it takes a transaction and should return it back (mimicking how the real repository works).
- `verify(transactionRepository, times(2)).save(...)` — asserts that `save()` was called exactly twice: once for PENDING, once for COMPLETED.

**What NOT to test**

- Don't test getters and setters — they contain no logic.
- Don't test Spring's own behaviour (`@GetMapping`, `@Autowired`) — the framework is already tested by its authors.
- Don't test things that cannot fail — a method that always returns a hardcoded string has nothing to verify.

**What to test**

- Business logic with branches — `if/else`, status transitions, exception throwing
- Controller responses — correct status codes and JSON shapes

---

### Mocking

The problem with unit testing `PaymentService` is that it depends on `ProviderRegistry`, `TransactionRepository`, and a `PaymentProvider`. Running those for real requires a wired Spring context and a database. That turns your unit test into something much slower and fragile.

A **mock** is a fake stand-in for a dependency. You replace the real `TransactionRepository` with a fake one that you fully control — you tell it "when `findById(1L)` is called, return this transaction object". The class under test never knows the difference.

**Mockito** is the standard mocking library in Java. Spring Boot includes it in the test scope.

**Setting up a test class with mocks**

```java
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.mockito.InjectMocks;

@ExtendWith(MockitoExtension.class)  // activates Mockito for this test class
class PaymentServiceTest {

    @Mock
    ProviderRegistry providerRegistry;  // a fake ProviderRegistry — you control what it returns

    @Mock
    TransactionRepository transactionRepository;  // a fake repository — no real database

    @Mock
    PaymentProvider paymentProvider;  // a fake provider — no real HTTP calls

    @InjectMocks
    PaymentService paymentService;  // the REAL class being tested — Mockito injects the mocks above into it
}
```

- `@ExtendWith(MockitoExtension.class)` — a JUnit plugin. Without this, the `@Mock` and `@InjectMocks` annotations are ignored.
- `@Mock` — creates a fake object of that type. By default, all its methods return null (or 0 for primitives). You tell it what to return using `when(...).thenReturn(...)`.
- `@InjectMocks` — creates a real instance of `PaymentService` and injects all `@Mock` fields into it automatically via the constructor.

---

**`when(...).thenReturn(...)` — controlling what a mock returns**

When `processPayment` calls `providerRegistry.findByName("Stripe")`, the mock doesn't know what to return — unless you tell it. `when(...).thenReturn(...)` is how you configure that.

```java
// "When findByName is called with 'Stripe', return the fake provider wrapped in Optional"
when(providerRegistry.findByName("Stripe")).thenReturn(Optional.of(paymentProvider));

// "When save() is called with any Transaction, return whatever was passed in"
when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
```

- `any(Transaction.class)` — an argument matcher meaning "any Transaction object, I don't care which one". Import: `import static org.mockito.ArgumentMatchers.*;`
- `thenAnswer(i -> i.getArgument(0))` — instead of returning a fixed value, return whatever was passed as argument 0. Used for `save()` because it receives a transaction and should give the same one back — mimicking how the real JPA save works.
- Without these `when` lines, the mock returns null — and `processPayment` would crash trying to call methods on null before it even reaches the logic you want to test.

**Why `Optional.of(paymentProvider)` and not just `paymentProvider`**

`findByName()` in `ProviderRegistry` uses `stream().filter(...).findFirst()`. `findFirst()` always returns an `Optional` — that's how it works: either `Optional.of(result)` if something matched, or `Optional.empty()` if nothing did. So `findByName()` has no choice but to return `Optional<PaymentProvider>`.

In `processPayment`, the code is:
```java
PaymentProvider provider = providerRegistry.findByName(providerName).orElseThrow(...);
```

`.orElseThrow()` is called on whatever `findByName()` returns. It checks: is there a value inside this Optional? If yes, unwrap and return it. If no, throw. So even in the success case, `.orElseThrow()` runs — it just unwraps and moves on without throwing.

That means the mock must return an `Optional`, not a bare `PaymentProvider` — because `processPayment` calls `.orElseThrow()` on the result. `Optional.of(paymentProvider)` creates the "found" case: an Optional that contains the fake provider, so `.orElseThrow()` unwraps it successfully.

---

**`thenThrow(...)` — making a mock throw an exception**

For the failure test, you want `charge()` to fail. `thenThrow` tells the mock to throw an exception instead of returning a value.

```java
// "When charge() is called, throw this exception instead of returning"
when(paymentProvider.charge(any(), any())).thenThrow(new RuntimeException("Connection timed out"));
```

This is how you test the catch block in `processPayment` — you force the failure artificially so you can verify what happens when things go wrong, without needing a real network error.

**The failure test still needs the same `when` setups for `findByName` and `save`**

`processPayment` calls `findByName` and `save` before it ever calls `charge`. So the failure test needs the same mock setup for those two — only `charge` changes from `thenReturn` to `thenThrow`. Without the `findByName` setup, `processPayment` crashes before it even reaches `charge`, and you never test the failure path.

---

**`Optional.empty()` — the not-found case**

For assignment 4 you test `findTransactionById` when the transaction doesn't exist. `findById` on the repository returns an `Optional<Transaction>` — either `Optional.of(transaction)` if found, or `Optional.empty()` if not. For the not-found test you configure the mock to return the empty case:

```java
when(transactionRepository.findById(anyLong())).thenReturn(Optional.empty());
```

- `anyLong()` — matches any `Long` argument. Use it when you don't care which ID is passed in.
- `Optional.empty()` — an Optional with nothing inside it. When `findTransactionById` calls `.orElseThrow()` on it, it throws `TransactionNotFoundException` — which is exactly what you're asserting.

---

**Strict stubbing — every `when` must be used**

`@ExtendWith(MockitoExtension.class)` runs Mockito in strict mode. This means: if you set up a `when(...)` for a method that never actually gets called during the test, Mockito fails the test with "unnecessary stubbing". 

This is a feature, not a bug — it stops you from writing mock setups that have no effect and give you false confidence. Only set up `when(...)` for methods that `processPayment` actually calls in that specific test scenario.

---

**`verify(...)` — asserting that a method was actually called**

`assertEquals` checks what came back. `verify` checks what happened along the way — specifically, whether a method on a mock was called, and how many times.

In `processPayment`, `save()` should be called twice: once to save PENDING at the start, once to save COMPLETED (or FAILED) at the end. You can't see that from the return value alone — `verify` is how you confirm it happened.

```java
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

// Assert that save() was called exactly twice
verify(transactionRepository, times(2)).save(any(Transaction.class));

// Assert that save() was called at least once
verify(transactionRepository, atLeastOnce()).save(any(Transaction.class));
```

`verify` takes two parameters:
1. The mock object you want to check — `transactionRepository` in this case
2. How many times you expect the method to have been called — `times(2)` means exactly twice, `atLeastOnce()` means one or more times

After `verify(...)` you chain the method call you're asserting — `.save(any(Transaction.class))` — exactly as if you were calling it for real. Mockito intercepts it and checks whether it was actually called that way.

Without `verify`: you could delete the first `save()` call from `processPayment` and the test would still pass, because the return value looks the same. With `verify`: the test fails immediately — it proves the save actually happened, not just that the final result looks right.

---

**`assertThrows(...)` — asserting that an exception is thrown**

For the failure case, you want to verify that `processPayment` rethrows the exception after setting the status to FAILED. `assertThrows` is how you test that a method throws instead of trying to catch the exception yourself.

```java
import static org.junit.jupiter.api.Assertions.assertThrows;

// Assert that processPayment throws a RuntimeException
RuntimeException ex = assertThrows(RuntimeException.class, () ->
    paymentService.processPayment("Stripe", new BigDecimal("100"), PaymentMethod.CARD)
);
```

- The first argument is the exception type you expect.
- The second argument is a lambda — the code that should throw. If it does not throw, the test fails. If it throws a different type, the test fails.
- `assertThrows` returns the thrown exception so you can inspect its message if needed: `ex.getMessage()`.

Without `assertThrows`: if you just called `processPayment` directly and it threw, JUnit would mark the test as errored — not as a deliberate assertion. `assertThrows` turns "it threw" into a deliberate pass/fail check.

---

### Integration testing

An integration test starts the full Spring context — the same way the real app starts — and tests multiple layers working together. The database must be up. HTTP routing, service logic, and repository queries all run for real.

**When to use integration tests vs unit tests**

Unit tests verify logic inside a single class — they're fast and precise. Integration tests verify that the layers connect correctly — they're slower but catch problems unit tests miss, like a misconfigured `@RequestBody` that causes Spring to pass null into the service, or a JPA query that works in code but produces wrong SQL.

**Production scenario:** You want to verify that when a merchant sends `POST /payments`, the transaction is actually persisted to the database with status COMPLETED and the JSON response contains the `id` field. That's a full end-to-end flow — controller → service → repository → database — and only an integration test can confirm all four steps work together.

**`@SpringBootTest`**

```java
@SpringBootTest
class PaymentControllerTest { ... }
```

Starts the full Spring application context. Same bean wiring as running the app. Database must be up (the `application-dev.yaml` datasource config is used).

**`@AutoConfigureMockMvc`**

```java
@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    MockMvc mockMvc;
    
    @Autowired
    TransactionRepository transactionRepository;
}
```

- `@AutoConfigureMockMvc` — tells Spring to create a `MockMvc` bean and inject it. Without this, you can't inject `MockMvc`.
- `MockMvc` — lets you send HTTP requests to your controllers without starting a real HTTP server. The requests are processed in-process, exactly as they would be on a real server.
- You can also inject `TransactionRepository` to query the database directly in your test — useful for asserting that a save actually happened.

**Sending requests with MockMvc**

```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Test
void postPayment_validRequest_returns200() throws Exception {
    String body = """
        {
            "providerName": "Stripe",
            "amount": 100.00,
            "paymentMethod": "CARD"
        }
        """;

    mockMvc.perform(
            post("/payments")
                .contentType(APPLICATION_JSON)
                .content(body)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FAILED"));  // FAILED because Stripe URL is a placeholder
}
```

- `mockMvc.perform(...)` — sends a request and returns a result object you can chain assertions onto.
- `.contentType(APPLICATION_JSON)` — sets the `Content-Type` header so Spring knows to parse the body as JSON.
- `.content(body)` — the raw JSON request body as a String.
- `.andExpect(status().isOk())` — asserts HTTP 200. Use `status().isNotFound()` for 404, `status().is5xxServerError()` for 5xx.
- `.andExpect(jsonPath("$.status").value("FAILED"))` — asserts a specific field in the JSON response. `$.status` means "the `status` field at the top level of the JSON object". `$.id` is the id field, `$.amount` is the amount, and so on.

**Note on this project's integration test:** Because the Stripe and Adyen URLs are placeholders (`https://api.stripe.com` hits a real server that rejects the request), `charge()` will always throw. So the integration test for `POST /payments` will see a `FAILED` transaction, not COMPLETED. That is expected — the test still proves that the full request-to-database flow works, including the failure path.

**Cleaning up between tests**

Integration tests run against a real database, so each test can leave data behind that affects the next. Add a `@BeforeEach` or `@AfterEach` method to clear the repository:

```java
@BeforeEach
void setup() {
    transactionRepository.deleteAll();
}
```

`@BeforeEach` — JUnit calls this method before every `@Test` method in the class. Use it for setup that must run fresh each time.

---

### Mocked HTTP servers

When you build the real Stripe integration (a later step), `charge()` will call `https://api.stripe.com` using `RestTemplate`. In integration tests, you can't call the real Stripe API — you have no API key in the test environment, and tests should be deterministic and offline.

**`MockRestServiceServer`** is Spring's built-in solution. It intercepts `RestTemplate` calls before they go out over the network, and returns a fake response you define in the test.

```java
@Autowired
RestTemplate restTemplate;

MockRestServiceServer mockServer;

@BeforeEach
void setup() {
    mockServer = MockRestServiceServer.createServer(restTemplate);
}

@Test
void stripeCharge_success_returnsCompletedTransaction() throws Exception {
    // Tell the mock server: when a POST to this URL is received, return this JSON
    mockServer.expect(requestTo("https://api.stripe.com"))
              .andExpect(method(HttpMethod.POST))
              .andRespond(withSuccess(
                  """{"id":"ch_abc123","status":"succeeded","amount":10000}""",
                  MediaType.APPLICATION_JSON
              ));

    // Now send the real HTTP request — RestTemplate hits the mock, not the real Stripe
    mockMvc.perform(post("/payments").contentType(APPLICATION_JSON).content(body))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("COMPLETED"));

    mockServer.verify();  // asserts all expected calls were actually made
}
```

- `MockRestServiceServer.createServer(restTemplate)` — wraps the `RestTemplate` with an interceptor. From this point on, `restTemplate` does not make real HTTP calls.
- `.expect(requestTo(...))` — define an expected outbound request.
- `.andRespond(withSuccess(...))` — return this body and status when that request comes in.
- `mockServer.verify()` — assert that all expected requests were actually triggered. Fails if your code never called `RestTemplate`.

**This is relevant for you** because when you build the real Stripe integration, every test that exercises the charge flow will need `MockRestServiceServer` to avoid hitting the real API.

**WireMock** is a more powerful alternative that starts an actual local HTTP server your app calls. It supports more realistic scenarios (delays, error sequences, partial failures) but requires an extra dependency. Use `MockRestServiceServer` first — it covers most cases without extra setup.

---

## Webhooks & async patterns

---

### Callbacks & webhooks

---

### Request/response vs event-driven

---

## Cryptography basics

---

### HMAC signatures

---

### AES encryption & RSA

---

## Build & tooling

---

### Git

---

### Maven

Maven is the build tool for your project. It handles two main things: building your code into a runnable app, and managing dependencies — the external libraries your project needs.

**`pom.xml`** is the project configuration file. Everything Maven needs to know about your project lives here — its name, Java version, Spring Boot version, and the list of dependencies.

**Dependencies** — external libraries you add to your project. Instead of downloading jar files manually, you declare what you need in `pom.xml` and Maven downloads it automatically from the internet:

```xml
<dependency>
    <groupId>org.postgresql</groupId>  <!-- organisation that publishes the library -->
    <artifactId>postgresql</artifactId>  <!-- the specific library -->
    <scope>runtime</scope>  <!-- when it's needed — see below -->
</dependency>
```

**Scope** — controls when the dependency is available:
- `runtime` — only needed when the app is running, not during compilation. Use for drivers and libraries your code never imports directly (e.g. the PostgreSQL driver — Spring uses it behind the scenes, your code never references it).
- No scope (default) — available at both compile time and runtime. Use for libraries your code imports directly (e.g. Spring, Jackson).

**Common Maven commands:**
- `mvn spring-boot:run` — build and start the app
- `mvn clean install` — clean previous build output, compile, run tests, and package the app

---

### Docker

---

### CI/CD

---

### Database migrations

---

## Observability

---

### Logging

---

---

## Payment service — flow reference

A quick-reference map of how the payment service works. All classes, all flows, one place.

---

### Component map

```
HTTP request
     │
     ▼
┌─────────────────────────────────┐
│  PaymentController              │  receives HTTP, delegates to service, returns JSON
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│  PaymentService                 │  orchestrates: find provider, save transaction, call charge
└───────┬─────────────────┬───────┘
        │                 │
        ▼                 ▼
┌───────────────┐  ┌──────────────────────┐
│ ProviderReg-  │  │ TransactionRepository │  Spring Data JPA — save/findById/findByStatus
│ istry         │  └──────────┬───────────┘
│               │             │
│ finds the     │             ▼
│ right provider│      ┌─────────────┐
└───────┬───────┘      │  PostgreSQL │
        │              └─────────────┘
        ▼
┌───────────────────────────────────────────┐
│  AbstractPaymentProvider                  │  shared: apiUrl field
│  ├── StripeProvider   (JSON via Jackson)  │  @Retryable, @Recover, @PostConstruct
│  └── AdyenProvider    (XML via XmlMapper) │
└───────────────────────────────────────────┘

Config loaded at startup:
ProviderProperties  ←  application-dev.yaml  ←  env vars (STRIPE_API_KEY, ADYEN_API_KEY)
```

---

### POST /payments — success

```
POST /payments  {"providerName":"Stripe","amount":100,"paymentMethod":"CARD"}
  │
  ▼ PaymentController.processPayment(@RequestBody)
  │
  ▼ PaymentService.processPayment("Stripe", 100, CARD)
  │
  ├─ ProviderRegistry.findProvider("Stripe")  →  StripeProvider
  │
  ├─ TransactionRepository.save(status=PENDING)  →  transaction saved, id assigned
  │
  ├─ StripeProvider.charge(100, CARD)
  │     └─ RestTemplate.postForObject(stripeUrl, 100)  →  JSON response body
  │         └─ ObjectMapper.readValue(body)  →  StripeChargeResponse
  │
  ├─ TransactionRepository.save(status=COMPLETED)
  │
  └─ returns Transaction  →  Spring converts to JSON  →  HTTP 200
```

---

### POST /payments — failure + retry

```
POST /payments  {"providerName":"Stripe", ...}
  │
  ▼ PaymentService.processPayment(...)
  │
  ├─ TransactionRepository.save(status=PENDING)
  │
  ├─ StripeProvider.charge(...)          ← @Retryable(maxAttempts=3)
  │     └─ RestTemplate throws RuntimeException
  │          │
  │          ├─ retry 2  →  throws again
  │          ├─ retry 3  →  throws again
  │          │
  │          └─ @Recover runs  →  throws RuntimeException("Stripe charge failed after retries: ...")
  │
  ├─ PaymentService catch block
  │     ├─ TransactionRepository.save(status=FAILED)
  │     └─ rethrows RuntimeException
  │
  └─ Exception escapes controller
        └─ @ControllerAdvice has no handler for plain RuntimeException  →  HTTP 500

  If the error was ProviderNotFoundException (unknown provider name):
  └─ GlobalExceptionHandler.handleProviderNotFound()  →  HTTP 404
```

---

### GET /payments/{id}

```
GET /payments/42
  │
  ▼ PaymentController.findTransactionById(42)
  │
  ▼ PaymentService.findTransactionById(42)
  │
  ▼ TransactionRepository.findById(42)  →  Optional<Transaction>
  │
  ├─ found:      returns Transaction  →  HTTP 200 + JSON
  │
  └─ not found:  throws TransactionNotFoundException("Transaction not found: 42")
                   └─ GlobalExceptionHandler.handleTransactionNotFound()  →  HTTP 404
```

---

### GET /payments?status=COMPLETED

```
GET /payments?status=COMPLETED
  │
  ▼ PaymentController.getTransactionsByStatus(COMPLETED)
  │    Spring converts "COMPLETED" string → TransactionStatus.COMPLETED enum automatically
  │
  ▼ PaymentService.findTransactionsByStatus(COMPLETED)
  │
  ▼ TransactionRepository.findByTransactionStatus(COMPLETED)
  │    Spring generates SQL: SELECT * FROM transaction WHERE transaction_status = 'COMPLETED'
  │
  └─ returns List<Transaction>  →  Spring converts to JSON array  →  HTTP 200
```

---

### App startup — config loading

```
Application starts
  │
  ▼ Spring reads application.yaml (base config: app name only)
  │
  ▼ spring.profiles.active=dev  →  Spring also reads application-dev.yaml
  │    providers.stripe.base-url  =  https://api.stripe.com
  │    providers.stripe.api-key   =  ${STRIPE_API_KEY}  ←  read from IntelliJ env vars
  │    providers.adyen.base-url   =  https://api.adyen.com
  │    providers.adyen.api-key    =  ${ADYEN_API_KEY}
  │
  ▼ @ConfigurationProperties(prefix="providers") on ProviderProperties
  │    Spring maps yml values into ProviderProperties.stripe.baseUrl / apiKey etc.
  │    (uses setters on the nested Stripe and Adyen classes to populate the fields)
  │
  ▼ Spring finishes wiring all beans
  │
  ▼ @PostConstruct on StripeProvider fires
       logs: "Stripe masked apiKey sk_test***"
```
