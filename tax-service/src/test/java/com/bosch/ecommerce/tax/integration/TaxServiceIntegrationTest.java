package com.bosch.ecommerce.tax.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Full-stack test: real Flyway migrations (schema + seed) on an in-memory H2, real HTTP calls. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TaxServiceIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> calculate(String json) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/taxes/calculate"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static String body(String productId, String amount, String seller, String buyer, String date) {
        return "{\"productId\":\"" + productId + "\",\"taxableAmount\":" + amount + ",\"sellerState\":\"" + seller
                + "\",\"buyerState\":\"" + buyer + "\",\"transactionDate\":\"" + date + "\"}";
    }

    private static double num(String json, String path) {
        return ((Number) JsonPath.read(json, path)).doubleValue();
    }

    @Test
    void interStateCalculationUsesIgstFromDatabaseRule() throws Exception {
        HttpResponse<String> r = calculate(body("BOS-001", "6999.00", "KA", "MH", "2026-10-03"));

        assertThat(r.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(r.body(), "$.taxType")).isEqualTo("IGST");
        assertThat(num(r.body(), "$.igstRate")).isEqualTo(18.0);
        assertThat(num(r.body(), "$.taxAmount")).isEqualTo(1259.82);
        assertThat(num(r.body(), "$.totalAmount")).isEqualTo(8258.82);
        assertThat((String) JsonPath.read(r.body(), "$.hsnCode")).isEqualTo("8467");
    }

    @Test
    void intraStateCalculationUsesCgstAndSgst() throws Exception {
        HttpResponse<String> r = calculate(body("BOS-001", "6999.00", "KA", "KA", "2026-10-03"));

        assertThat(r.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(r.body(), "$.taxType")).isEqualTo("CGST_SGST");
        assertThat(num(r.body(), "$.cgstRate")).isEqualTo(9.0);
        assertThat(num(r.body(), "$.sgstRate")).isEqualTo(9.0);
        assertThat(num(r.body(), "$.igstRate")).isEqualTo(0.0);
        assertThat(num(r.body(), "$.cgstAmount")).isEqualTo(629.91);
        assertThat(num(r.body(), "$.sgstAmount")).isEqualTo(629.91);
        assertThat(num(r.body(), "$.taxAmount")).isEqualTo(1259.82);
    }

    @Test
    void productWithUnverifiedRate_isRefusedWith422() throws Exception {
        HttpResponse<String> r = calculate(body("BOS-020", "4249.15", "KA", "MH", "2026-10-03"));

        assertThat(r.statusCode()).isEqualTo(422);
        assertThat((String) JsonPath.read(r.body(), "$.error")).isEqualTo("TAX_RATE_NOT_VERIFIED");
    }

    @Test
    void unknownProduct_returns404() throws Exception {
        HttpResponse<String> r = calculate(body("NOPE-1", "100.00", "KA", "MH", "2026-10-03"));

        assertThat(r.statusCode()).isEqualTo(404);
        assertThat((String) JsonPath.read(r.body(), "$.error")).isEqualTo("TAX_CLASSIFICATION_NOT_FOUND");
    }

    @Test
    void transactionBeforeEffectiveDate_returns404() throws Exception {
        HttpResponse<String> r = calculate(body("BOS-001", "100.00", "KA", "MH", "2026-09-30"));

        assertThat(r.statusCode()).isEqualTo(404);
        assertThat((String) JsonPath.read(r.body(), "$.error")).isEqualTo("TAX_CLASSIFICATION_NOT_FOUND");
    }

    @Test
    void invalidAmount_returns400() throws Exception {
        HttpResponse<String> r = calculate(body("BOS-001", "-5.00", "KA", "MH", "2026-10-03"));

        assertThat(r.statusCode()).isEqualTo(400);
        assertThat((String) JsonPath.read(r.body(), "$.error")).isEqualTo("INVALID_TAXABLE_AMOUNT");
    }

    @Test
    void invalidState_returns400() throws Exception {
        HttpResponse<String> r = calculate(body("BOS-001", "100.00", "ZZ", "MH", "2026-10-03"));

        assertThat(r.statusCode()).isEqualTo(400);
        assertThat((String) JsonPath.read(r.body(), "$.error")).isEqualTo("INVALID_STATE");
    }

    @Test
    void invalidDate_returns400() throws Exception {
        HttpResponse<String> r = calculate(body("BOS-001", "100.00", "KA", "MH", "2010-01-01"));

        assertThat(r.statusCode()).isEqualTo(400);
        assertThat((String) JsonPath.read(r.body(), "$.error")).isEqualTo("INVALID_TRANSACTION_DATE");
    }

    @Test
    void missingFields_returnValidationError() throws Exception {
        HttpResponse<String> r = calculate("{}");

        assertThat(r.statusCode()).isEqualTo(400);
        assertThat((String) JsonPath.read(r.body(), "$.error")).isEqualTo("VALIDATION_FAILED");
        List<String> details = JsonPath.read(r.body(), "$.details");
        assertThat(details).isNotEmpty();
    }

    @Test
    void taxRuleEndpointReturnsSeededRule() throws Exception {
        HttpResponse<String> r = get("/api/v1/tax-rules/8467?date=2026-10-03");

        assertThat(r.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(r.body(), "$.taxRuleId")).isEqualTo("TR-8467");
        assertThat(num(r.body(), "$.igstRate")).isEqualTo(18.0);
    }

    @Test
    void taxClassificationEndpointReturnsSeededMapping() throws Exception {
        HttpResponse<String> r = get("/api/v1/tax-classifications/product/BOS-001?date=2026-10-03");

        assertThat(r.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(r.body(), "$.hsnCode")).isEqualTo("8467");
        assertThat((String) JsonPath.read(r.body(), "$.hsnLevel")).isEqualTo("HEADING");
    }

    @Test
    void unknownHsn_returns404() throws Exception {
        HttpResponse<String> r = get("/api/v1/tax-rules/0000?date=2026-10-03");

        assertThat(r.statusCode()).isEqualTo(404);
        assertThat((String) JsonPath.read(r.body(), "$.error")).isEqualTo("TAX_RULE_NOT_FOUND");
    }
}
