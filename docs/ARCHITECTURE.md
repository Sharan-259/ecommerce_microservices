# Architecture notes

## Service ownership

| Concern | Owner | Not owned by |
|---|---|---|
| Base price, selling price, discount, currency, validity, version, history, status | **Price Service** | Tax Service |
| HSN classification, GST rules (CGST/SGST/IGST/cess), effective dates, tax calculation | **Tax Service** | Price Service |
| "What does the customer finally pay?" | future **Order/Checkout Service** (not built) | both |

Tax Service takes `taxableAmount` as an input in its request. It does not look the price up. Price Service never
mentions tax. A checkout service would call Price first, then pass the selling price to Tax.

## High cohesion

Every class in a service exists for that service's single business capability. `PriceService` contains only pricing
rules (versioning, validity windows, discount/selling-price consistency). `TaxService` contains only tax rules
(classification lookup, effective-dated rule lookup, intra/inter-state decision, rounding). Changing GST logic never
touches Price code and vice versa.

## Low coupling

* No `PriceService -> TaxService` or `TaxService -> PriceService` call, no REST client, no shared library.
* Separate Maven projects with no parent/shared module; separate databases (`./data/price-service-db`,
  `./data/tax-service-db`); separate entities, repositories, DTOs, exceptions and migrations. Even the `ErrorResponse`
  class is duplicated on purpose, so neither service depends on the other's code.
* The only coupling is the **business identifier** `productId` (a string like `BOS-001`) and the HTTP contract that a
  caller (Checkout) will use. That is the intended, loose kind of coupling.
* `product_id` in the Tax database is a reference value, not a foreign key into the Price database (impossible and
  undesirable across service boundaries).

## Failure isolation

Stop Price Service and Tax Service still answers tax questions (and the reverse). Each has its own database file,
its own health endpoint (`/actuator/health`), its own port and its own failure modes. A broken migration or locked
database in one service cannot affect the other.

## Why each design choice

* **Separate services / databases** - independent ownership, deployment, scaling and failure; each team can change its schema without coordination.
* **DTOs (Java records)** - API contracts are decoupled from JPA entities; internal columns (`price_id`, audit fields)
  can change without breaking clients, and entities are never serialized by accident.
* **Controller -> Service -> Repository** - controllers only translate HTTP; services hold business rules and
  transactions; repositories only talk to the database. Each layer is testable in isolation (the unit tests mock the repository).
* **Flyway** - schema and seed data are versioned, repeatable SQL (`V1` schema, `V2/V3` seed). Hibernate is set to
  `ddl-auto: none`, so the database never changes silently. Each service has its own migration history.
* **H2 file mode** - zero-install persistence for the demo; data survives restarts. Switching to PostgreSQL later means
  changing the datasource and driver, not the code (the SQL used is standard).
* **API versioning (`/api/v1`)** - the contract can evolve (`/api/v2`) without breaking existing callers.
* **Validation + central exception handling** - Bean Validation rejects malformed input at the edge (400);
  business rules throw typed exceptions (404/409/422); one `@RestControllerAdvice` turns every failure into the same
  JSON error shape with a stable machine-readable `error` code.
* **Price versioning** - history is never overwritten. Creating a price closes the current version
  (`valid_to = newValidFrom - 1 day`, status `SUPERSEDED`), inserts version N+1, and a unique `(product_id, price_version)`
  constraint guards against concurrent writers (the loser gets 409). The client cannot choose the version.
* **Effective dates** - both services answer "as of date X": `valid_from <= X AND (valid_to IS NULL OR valid_to >= X)`.
  Orders placed under an old price/tax rule can therefore be reproduced, and future-dated changes can be scheduled.
* **BigDecimal / DECIMAL** - money is never `float`/`double`. Tax uses `HALF_UP` rounding to 2 decimals per component.
* **Rates from the database** - GST rates live only in `tax_rules`. A unit test runs a 12% rule and a cess rule to
  prove nothing is hard-coded at 18%.
* **Honest data handling** - a rule without a verified rate (heading 8507) is refused with HTTP 422 instead of guessed;
  4-digit HSN headings are labelled `hsn_level = HEADING` and every calculation response carries a warning.
* **Clock bean** - "today" is injected, so date logic is deterministic in tests.

## Version rules in one picture

```
BOS-001 v1  valid 2026-10-01 .. 2026-10-02   SUPERSEDED   (closed by the POST, record kept)
BOS-001 v2  valid 2026-10-03 .. (open)       ACTIVE
GET ?date=2026-10-02 -> v1      GET ?date=2026-10-03 -> v2      GET ?date=2026-09-30 -> 404
```
