# E-Commerce Microservices Demo: Price Service + Tax Service

Two independently owned Spring Boot microservices built from `bosch_manager_demo_gst_dataset_20.xlsx`.

| | Price Service | Tax Service |
|---|---|---|
| Question it answers | "How much does the product cost?" | "How much tax applies?" |
| Port | 8083 | 8084 |
| Package | `com.bosch.ecommerce.price` | `com.bosch.ecommerce.tax` |
| Database | `./data/price-service-db` (file H2) | `./data/tax-service-db` (file H2) |
| Swagger UI | http://localhost:8083/swagger-ui/index.html | http://localhost:8084/swagger-ui/index.html |
| Health | http://localhost:8083/actuator/health | http://localhost:8084/actuator/health |
| H2 Console | http://localhost:8083/h2-console | http://localhost:8084/h2-console |

> **Data disclaimer.** All prices are **synthetic demo prices**, not official Bosch prices. HSN values are 4-digit
> **headings/candidates** from the dataset, not verified 8-digit product codes. Do not use for real invoicing.

## 1. Architecture

```
                E-COMMERCE PLATFORM (today)

   Thunder Client / Postman
        |                    \
        v                     v
 +----------------+     +----------------+
 | Price Service  |     | Tax Service    |
 |    :8083       |     |    :8084       |
 +-------+--------+     +-------+--------+
         |                      |
         v                      v
   price-service-db       tax-service-db
      (H2 file)              (H2 file)
```

The two services never call each other, share no database, no entity, no repository and no business logic.

**Future architecture** (NOT built here): an Order/Checkout service would orchestrate both.

```
                Order / Checkout Service
                   /              \
                  v                v
          Price Service       Tax Service
               :8083              :8084
                 |                  |
              Price DB            Tax DB
```

Checkout asks Price Service for the selling price, then passes that amount to Tax Service as `taxableAmount`,
and finally computes the final order amount. Neither service needs to know the other exists.

Details: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). Request/response examples: [docs/API_AND_DEMO_GUIDE.md](docs/API_AND_DEMO_GUIDE.md).

## 2. Technology stack and version choice

* **Spring Boot 4.1.1** (latest stable 4.1.x I could confirm), Spring Framework 7, Hibernate 7, Jackson 3, JUnit 6
* Spring Web MVC, Spring Data JPA, Bean Validation, Actuator, Flyway, H2 (file mode), SpringDoc OpenAPI **3.1.0**
* JUnit + Mockito + AssertJ; no Lombok (avoids annotation-processor risk on a brand-new JDK)

**Java 27 compatibility.** Spring Boot 4.1.x officially lists Java 17 through 26; Java 27 support is being documented
for 4.1.2 / 4.2. To stay safe the build uses `maven.compiler.release=21`: your Java 27 JDK compiles **Java 21 bytecode**,
which runs unchanged on the Java 27 JVM. This avoids "unsupported class file major version" problems in
Spring/Hibernate/ByteBuddy and means you do **not** need to downgrade or install any JDK. Surefire also passes
`-Dnet.bytebuddy.experimental=true` so Mockito works on a JVM newer than ByteBuddy has been tested with.

## 3. Prerequisites (you already have these)

* Windows 11, Java 27 (`java -version`), Maven 3.9.16 on PATH (`mvn -v`)
* No PostgreSQL / Docker needed. First build needs internet access to Maven Central (or your office Nexus mirror).

## 4. Project structure

```
ecommerce-microservices/
+-- README.md
+-- docs/  (ARCHITECTURE.md, API_AND_DEMO_GUIDE.md)
+-- postman/ecommerce-demo.postman_collection.json     (23 requests; imports into Thunder Client too)
+-- price-service/
|   +-- pom.xml, run.bat, reset-data.bat
|   +-- src/main/java/com/bosch/ecommerce/price/
|   |   +-- PriceServiceApplication.java
|   |   +-- controller/PriceController.java
|   |   +-- service/PriceService.java
|   |   +-- repository/PriceRepository.java
|   |   +-- entity/ProductPrice.java, PriceStatus.java
|   |   +-- dto/CreatePriceRequest, PriceResponse, PriceHistoryResponse, ErrorResponse
|   |   +-- mapper/PriceMapper.java
|   |   +-- exception/ (PriceNotFound, InvalidPrice, PriceConflict, GlobalExceptionHandler)
|   |   +-- config/ClockConfig.java, OpenApiConfig.java
|   +-- src/main/resources/application.yml
|   +-- src/main/resources/db/migration/V1__create_price_tables.sql, V2__seed_price_data.sql
|   +-- src/test/...  (service, controller, integration tests + application-test.yml)
+-- tax-service/
    +-- pom.xml, run.bat, reset-data.bat
    +-- src/main/java/com/bosch/ecommerce/tax/
    |   +-- TaxServiceApplication.java
    |   +-- controller/TaxController.java
    |   +-- service/TaxService.java, IndianStates.java
    |   +-- repository/TaxRuleRepository.java, ProductTaxMappingRepository.java
    |   +-- entity/TaxRule.java, ProductTaxMapping.java
    |   +-- dto/TaxCalculationRequest, TaxCalculationResponse, TaxRuleResponse, TaxClassificationResponse, ErrorResponse
    |   +-- mapper/TaxMapper.java
    |   +-- exception/ (6 exceptions + GlobalExceptionHandler)
    |   +-- config/ClockConfig.java, OpenApiConfig.java
    +-- src/main/resources/application.yml
    +-- src/main/resources/db/migration/V1__create_tax_tables.sql, V2__seed_tax_rules.sql, V3__seed_product_tax_mapping.sql
    +-- src/test/...
```

There is deliberately **no parent POM and no shared module**. Each folder builds and runs on its own.

## 5. How to run

Open two VS Code terminals (Terminal > New Terminal, then the split icon).

**Terminal 1 - Price Service**
```
cd price-service
mvn clean install
mvn spring-boot:run
```
**Terminal 2 - Tax Service**
```
cd tax-service
mvn clean install
mvn spring-boot:run
```
Wait for `Started PriceServiceApplication` / `Started TaxServiceApplication`. If a test fails during `install`, you can
still run: `mvn clean install -DskipTests` and then `mvn spring-boot:run` (and please tell me which test failed).

Run each `mvn` command from inside its own service folder: the `./data/...` database path is relative to that folder.

## 6. H2 (file-based) and H2 Console

| | Price Service | Tax Service |
|---|---|---|
| Console URL | http://localhost:8083/h2-console | http://localhost:8084/h2-console |
| JDBC URL | `jdbc:h2:file:./data/price-service-db` | `jdbc:h2:file:./data/tax-service-db` |
| Driver class | `org.h2.Driver` | `org.h2.Driver` |
| User name | `sa` | `sa` |
| Password | `price123` | `tax123` |

Files appear in `price-service/data/` and `tax-service/data/` and survive restarts. Flyway creates and seeds the
schema on first start only. To start from scratch: stop the service, run `reset-data.bat` in that folder, start again.
(These are local demo credentials; the console only accepts connections from localhost.)

Useful queries: `SELECT * FROM PRODUCT_PRICES ORDER BY PRODUCT_ID, PRICE_VERSION;`,
`SELECT * FROM TAX_RULES;`, `SELECT * FROM PRODUCT_TAX_MAPPING;`, `SELECT * FROM "flyway_schema_history";`

## 7. Database structure

**Price Service - `product_prices`**: `price_id`, `product_id`, `currency`, `base_price DECIMAL(12,2)`,
`selling_price DECIMAL(12,2)`, `discount_percent DECIMAL(5,2)`, `valid_from`, `valid_to`, `price_version`, `status`
(ACTIVE / SUPERSEDED), `created_at`, `updated_at`. Unique `(product_id, price_version)`; CHECK constraints on amounts,
discount range and validity window.

**Tax Service - `tax_rules`**: `tax_rule_id`, `hsn_code`, `hsn_level`, `hsn_description`, `cgst_rate`, `sgst_rate`,
`igst_rate`, `cess_rate`, `effective_from`, `effective_to`, `status`, `source_reference`.
**`product_tax_mapping`**: `mapping_id`, `product_id`, `hsn_code`, `hsn_level`, `hsn_description`,
`verification_status`, `tax_source`, `effective_from`, `effective_to`, `status`.
The mapping points to a rule through `hsn_code` + the transaction date (a *logical* reference, not a foreign key,
because one HSN can have several effective-dated rules).

## 8. API summary

Price Service: `GET /api/v1/prices/{productId}`, `GET /api/v1/prices/{productId}?date=YYYY-MM-DD`,
`POST /api/v1/prices`, `GET /api/v1/prices/{productId}/history`.

Tax Service: `POST /api/v1/taxes/calculate`, `GET /api/v1/tax-rules/{hsnCode}` (optional `?date=`),
`GET /api/v1/tax-classifications/product/{productId}` (optional `?date=`).

Every error has the same shape: `timestamp`, `status`, `error`, `message`, `path`, `details`.

## 9. Testing with Thunder Client / Postman

1. Thunder Client: Collections > menu (...) > **Import** > choose `postman/ecommerce-demo.postman_collection.json`
   (Postman: File > Import, same file).
2. Run the requests in numeric order. Every request, body and expected response is also written out in
   [docs/API_AND_DEMO_GUIDE.md](docs/API_AND_DEMO_GUIDE.md).
3. `Content-Type: application/json` is the only header needed.

Automated tests: `mvn test` inside each service (unit tests with Mockito, standalone MockMvc controller tests, and
full-stack tests on an in-memory H2 that run the real Flyway migrations).

## 10. Assumptions and limits (please read before the demo)

* The dataset has **no cess column**; cess is seeded as `0.00` for the verified rules. Heading 8507 (BOS-020 battery)
  has no verified rate in the dataset, so its rates are `NULL` and `POST /taxes/calculate` for BOS-020 returns
  **HTTP 422 `TAX_RATE_NOT_VERIFIED`** rather than guessing.
* For products flagged `VERIFY EXACT HSN` (BOS-007 to BOS-010) the 4-digit heading's verified rate is used, and the
  response carries a `warnings` entry saying the product-level HSN must still be verified. No 8-digit HSN is invented.
* Seller/buyer states must be 2-letter Indian state/UT codes (KA, MH, DL, TN, ...).
* The dataset's Excel `selling_price` values like `8549.049999999999` are stored rounded to `8549.05`.
* The sample numbers in the task description (BOS-001 at 12,000) differ from the dataset (BOS-001 at 6,999). The
  database follows the **dataset**; the demo guide uses dataset values.
* **This code was written and reviewed without being compiled or run in my environment** (no Maven Central access
  there). It was carefully cross-checked, but you are the first to run it; see Troubleshooting if anything breaks.

## 11. Troubleshooting

| Symptom | Fix |
|---|---|
| `Port 8083/8084 was already in use` | Close the other instance, or run `netstat -ano` and look for :8083, then `taskkill /PID <pid> /F` |
| `Could not resolve dependencies` | Check internet/proxy or your company Maven mirror in `~/.m2/settings.xml` |
| `Could not find artifact org.springframework.boot:spring-boot-h2console` | Replace that dependency with `spring-boot-starter-h2-console` (the name differs between Boot 4 docs); or delete it and use only Flyway + app (console is optional) |
| `Database may be already in use` | Another instance of the same service is running; stop it |
| `Flyway checksum mismatch` / odd schema state | Stop the service and run `reset-data.bat` in that folder |
| Wrong data after editing a migration | Never edit an applied migration: add `V3__...` or reset the data folder |
| Mockito errors about unsupported Java version | Already mitigated (`-Dnet.bytebuddy.experimental=true`); otherwise `-DskipTests` |
| 409 on the demo POST | The new version already exists. Use a later `validFrom`, or `reset-data.bat` |
| `GET /prices/BOS-001` still shows version 1 after the POST | Your PC date is before the new version's `validFrom`; query with `?date=` |
| Swagger UI 404 | Use `/swagger-ui/index.html` (and `/v3/api-docs` for the JSON) |
