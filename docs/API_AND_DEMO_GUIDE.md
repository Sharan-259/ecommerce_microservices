# API reference, expected outputs and demo script

**Reading the outputs.** Everything below is an *example of the expected response*. The values come from the seeded
**synthetic demo dataset** (`bosch_manager_demo_gst_dataset_20.xlsx`), not from official Bosch data. Fields such as
`timestamp`, `createdAt`, `updatedAt` will differ on your machine. Header for every POST: `Content-Type: application/json`.

Assumes a **fresh database** and a PC date on or after 2026-10-03.

---
## PRICE SERVICE (http://localhost:8083)

### 1. GET current price
`GET http://localhost:8083/api/v1/prices/BOS-001`  -> 200
```json
{
  "productId": "BOS-001",
  "currency": "INR",
  "basePrice": 6999.00,
  "sellingPrice": 6999.00,
  "discountPercent": 0.00,
  "priceVersion": 1,
  "validFrom": "2026-10-01",
  "validTo": null,
  "status": "ACTIVE"
}
```

### 2. GET price history (before update)
`GET http://localhost:8083/api/v1/prices/BOS-001/history`  -> 200
```json
[
  {
    "priceId": 10001, "productId": "BOS-001", "currency": "INR",
    "basePrice": 6999.00, "sellingPrice": 6999.00, "discountPercent": 0.00,
    "priceVersion": 1, "validFrom": "2026-10-01", "validTo": null, "status": "ACTIVE",
    "createdAt": "2026-10-03T10:15:30.123", "updatedAt": "2026-10-03T10:15:30.123"
  }
]
```

### 3. POST new price (creates version 2)
`POST http://localhost:8083/api/v1/prices`  -> 201 (also returns a `Location` header)
```json
{
  "productId": "BOS-001",
  "currency": "INR",
  "basePrice": 7999.00,
  "sellingPrice": 7199.10,
  "discountPercent": 10.00,
  "validFrom": "2026-10-03"
}
```
Response:
```json
{
  "productId": "BOS-001", "currency": "INR",
  "basePrice": 7999.00, "sellingPrice": 7199.10, "discountPercent": 10.00,
  "priceVersion": 2, "validFrom": "2026-10-03", "validTo": null, "status": "ACTIVE"
}
```
The client sent no version: the service generated `2`.

### 4. GET history again - old record preserved
`GET http://localhost:8083/api/v1/prices/BOS-001/history` -> 200, two rows (oldest first):
version 1 now has `"validTo": "2026-10-02"` and `"status": "SUPERSEDED"` with its original 6999.00 prices intact;
version 2 has `"validTo": null`, `"status": "ACTIVE"`.

### 5. GET effective-dated price
* `GET .../api/v1/prices/BOS-001?date=2026-10-02` -> version 1 (selling price 6999.00)
* `GET .../api/v1/prices/BOS-001?date=2026-10-03` -> version 2 (selling price 7199.10)
* `GET .../api/v1/prices/BOS-001?date=2026-09-30` -> 404 `PRICE_NOT_FOUND`

### 6. Error examples
Unknown product `GET .../api/v1/prices/DOES-NOT-EXIST` -> 404
```json
{
  "timestamp": "2026-10-03T10:20:00.456",
  "status": 404,
  "error": "PRICE_NOT_FOUND",
  "message": "No active price found for product DOES-NOT-EXIST",
  "path": "/api/v1/prices/DOES-NOT-EXIST",
  "details": []
}
```
Validation: `POST /api/v1/prices` with
`{"productId":"","currency":"INR","basePrice":-1.00,"sellingPrice":10.00,"discountPercent":150.00,"validFrom":"2026-10-03"}`
-> 400 `VALIDATION_FAILED`, `details` lists e.g. `basePrice: basePrice must be >= 0`,
`discountPercent: discountPercent must be <= 100`, `productId: productId is required`.

Business rule: `basePrice 1000.00, sellingPrice 1500.00` -> 422 `INVALID_PRICE`
("sellingPrice cannot be greater than basePrice"). Selling price inconsistent with discount -> 422 with the expected value.
Re-posting a `validFrom` that is not after the current version's -> 409 `PRICE_VERSION_CONFLICT`.

---
## TAX SERVICE (http://localhost:8084)

### 7. GET product tax classification
`GET http://localhost:8084/api/v1/tax-classifications/product/BOS-001` -> 200
```json
{
  "productId": "BOS-001",
  "hsnCode": "8467",
  "hsnLevel": "HEADING",
  "hsnDescription": "Tools for working in the hand with self-contained motor",
  "verificationStatus": "HEADING/RATE VERIFIED; EXACT SUBHEADING CHECK",
  "taxSource": "CBIC GST rate schedule; GSTN Search HSN",
  "effectiveFrom": "2026-10-01",
  "effectiveTo": null,
  "status": "ACTIVE"
}
```

### 8. GET tax rule
`GET http://localhost:8084/api/v1/tax-rules/8467` -> 200
```json
{
  "taxRuleId": "TR-8467",
  "hsnCode": "8467",
  "hsnLevel": "HEADING",
  "hsnDescription": "Tools for working in the hand with self-contained motor",
  "cgstRate": 9.00, "sgstRate": 9.00, "igstRate": 18.00, "cessRate": 0.00,
  "effectiveFrom": "2026-10-01", "effectiveTo": null,
  "status": "HEADING_RATE_VERIFIED",
  "sourceReference": "CBIC GST rate schedule"
}
```

### 9. POST inter-state calculation (KA -> MH) -> IGST
`POST http://localhost:8084/api/v1/taxes/calculate`
```json
{
  "productId": "BOS-001",
  "taxableAmount": 7199.10,
  "sellerState": "KA",
  "buyerState": "MH",
  "transactionDate": "2026-10-03"
}
```
Response 200:
```json
{
  "productId": "BOS-001",
  "taxableAmount": 7199.10,
  "hsnCode": "8467",
  "taxType": "IGST",
  "cgstRate": 0.00, "sgstRate": 0.00, "igstRate": 18.00, "cessRate": 0.00,
  "cgstAmount": 0.00, "sgstAmount": 0.00, "igstAmount": 1295.84, "cessAmount": 0.00,
  "taxAmount": 1295.84,
  "totalAmount": 8494.94,
  "taxRuleId": "TR-8467",
  "verificationStatus": "HEADING/RATE VERIFIED; EXACT SUBHEADING CHECK",
  "warnings": [
    "HSN 8467 is a 4-digit heading/candidate; the exact product-level HSN must be verified before real invoicing.",
    "Demonstration data only - not for real invoicing or tax compliance."
  ]
}
```
(7199.10 x 18% = 1295.838, rounded HALF_UP to 1295.84.)

### 10. POST intra-state calculation (KA -> KA) -> CGST + SGST
Same body with `"buyerState": "KA"`. Response: `"taxType": "CGST_SGST"`, `cgstRate 9.00`, `sgstRate 9.00`,
`igstRate 0.00`, `cgstAmount 647.92`, `sgstAmount 647.92`, `igstAmount 0.00`, `taxAmount 1295.84`, `totalAmount 8494.94`.

### 11. Invalid request -> structured error
`POST .../api/v1/taxes/calculate` with `"taxableAmount": -100.00` -> 400
```json
{
  "timestamp": "2026-10-03T10:30:00.789",
  "status": 400,
  "error": "INVALID_TAXABLE_AMOUNT",
  "message": "taxableAmount must be greater than 0",
  "path": "/api/v1/taxes/calculate",
  "details": []
}
```
Other error cases:

| Request | Status | `error` |
|---|---|---|
| unknown product `NOPE-1` | 404 | `TAX_CLASSIFICATION_NOT_FOUND` |
| `transactionDate` 2026-09-30 (before the seeded effective date) | 404 | `TAX_CLASSIFICATION_NOT_FOUND` |
| `GET /tax-rules/0000` | 404 | `TAX_RULE_NOT_FOUND` |
| `sellerState: "XX"` | 400 | `INVALID_STATE` |
| `transactionDate: 2010-01-01` | 400 | `INVALID_TRANSACTION_DATE` |
| missing fields / `{}` | 400 | `VALIDATION_FAILED` (with `details`) |
| bad JSON or `03/10/2026` date | 400 | `MALFORMED_REQUEST` |
| product `BOS-020` (battery, heading 8507) | 422 | `TAX_RATE_NOT_VERIFIED` |

---
## Manager demonstration script (13 steps)

1. Terminal 1: `cd price-service`, `mvn spring-boot:run`. Wait for "Started PriceServiceApplication".
2. Thunder Client: `GET http://localhost:8083/api/v1/prices/BOS-001` - price (6999.00) comes from H2.
3. `GET .../prices/BOS-001/history` - one version.
4. `POST .../prices` (request 3 above) - note the service generated version 2.
5. `GET .../history` again - version 1 is still there, closed (`SUPERSEDED`); version 2 is `ACTIVE`.
   Optionally show `?date=2026-10-02` (v1) vs `?date=2026-10-03` (v2): effective-dated pricing.
6. Terminal 2: `cd tax-service`, `mvn spring-boot:run`. Both services are now running side by side.
7. `POST http://localhost:8084/api/v1/taxes/calculate` (inter-state) - 1295.84 tax computed by Tax Service from a rule in its own H2.
   Point out that `taxableAmount 7199.10` is the new selling price returned by Price Service: the caller carries it across, the services never talk.
8. Intra-state (`KA`/`KA`) - CGST 9% + SGST 9%.
9. Inter-state (`KA`/`MH`) - IGST 18%.
10. Invalid request (negative amount, or product `BOS-020`) - structured error JSON.
11. H2 Console on each service (use the JDBC URLs/credentials in the README): show `PRODUCT_PRICES` (both versions) and `TAX_RULES` / `PRODUCT_TAX_MAPPING`.
    Show that the two databases are separate files and separate consoles.
12. Swagger: http://localhost:8083/swagger-ui/index.html and http://localhost:8084/swagger-ui/index.html.
13. Actuator: http://localhost:8083/actuator/health and http://localhost:8084/actuator/health (status `UP`).
    Bonus: stop Price Service and show Tax Service still calculates - failure isolation.

## What to tell your manager

"I separated Price and Tax into independently owned microservices. Price Service owns price and price history, while
Tax Service owns tax rules and calculations. Each service has its own database and API contract, so they remain loosely
coupled. I used effective-dated data for pricing and tax rules, DTOs to separate API contracts from persistence models,
Flyway for schema migrations, and validation with centralized exception handling. Both services can be independently
started and tested through REST APIs. A future Order/Checkout service would orchestrate them: fetch the selling price
from Price Service, send it to Tax Service as the taxable amount, and compute the final order amount. The data is a
synthetic demo dataset; HSN codes are headings/candidates and one product (the battery) is deliberately refused because
its GST rate is not verified in the dataset."
