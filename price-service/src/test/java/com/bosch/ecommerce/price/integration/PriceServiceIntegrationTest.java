package com.bosch.ecommerce.price.integration;

import com.bosch.ecommerce.price.dto.CreatePriceRequest;
import com.bosch.ecommerce.price.dto.PriceResponse;
import com.bosch.ecommerce.price.entity.ProductPrice;
import com.bosch.ecommerce.price.repository.PriceRepository;
import com.bosch.ecommerce.price.service.PriceService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack test: real Flyway migrations (schema + seed) on an in-memory H2, real HTTP calls.
 * Each test uses its own product id so tests do not depend on each other.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PriceServiceIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private PriceRepository priceRepository;

    @Autowired
    private PriceService priceService;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String json) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void seededProductIsReturnedFromDatabase() throws Exception {
        HttpResponse<String> response = get("/api/v1/prices/BOS-001");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(response.body(), "$.productId")).isEqualTo("BOS-001");
        assertThat(((Number) JsonPath.read(response.body(), "$.sellingPrice")).doubleValue()).isEqualTo(6999.00);
        assertThat((Integer) JsonPath.read(response.body(), "$.priceVersion")).isEqualTo(1);
        assertThat((String) JsonPath.read(response.body(), "$.status")).isEqualTo("ACTIVE");
    }

    @Test
    void allTwentySeededProductsExist() {
        long count = priceRepository.findAll().stream().filter(p -> p.getProductId().startsWith("BOS-")).count();
        assertThat(count).isGreaterThanOrEqualTo(20);
    }

    @Test
    void effectiveDateBeforeFirstVersion_returns404() throws Exception {
        HttpResponse<String> response = get("/api/v1/prices/BOS-002?date=2026-09-30");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat((String) JsonPath.read(response.body(), "$.error")).isEqualTo("PRICE_NOT_FOUND");
    }

    @Test
    void unknownProduct_returnsStructuredNotFound() throws Exception {
        HttpResponse<String> response = get("/api/v1/prices/DOES-NOT-EXIST");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat((String) JsonPath.read(response.body(), "$.error")).isEqualTo("PRICE_NOT_FOUND");
        assertThat((String) JsonPath.read(response.body(), "$.message"))
                .isEqualTo("No active price found for product DOES-NOT-EXIST");
        assertThat((String) JsonPath.read(response.body(), "$.path")).isEqualTo("/api/v1/prices/DOES-NOT-EXIST");
    }

    @Test
    void versioningPreservesOldRecordAndClosesItsValidity() throws Exception {
        String first = """
                {"productId":"IT-VERSION-1","currency":"INR","basePrice":10000.00,"sellingPrice":10000.00,
                 "discountPercent":0.00,"validFrom":"2026-01-01"}
                """;
        String second = """
                {"productId":"IT-VERSION-1","currency":"INR","basePrice":11000.00,"sellingPrice":11000.00,
                 "discountPercent":0.00,"validFrom":"2026-07-01"}
                """;

        assertThat(post("/api/v1/prices", first).statusCode()).isEqualTo(201);
        HttpResponse<String> created = post("/api/v1/prices", second);
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat((Integer) JsonPath.read(created.body(), "$.priceVersion")).isEqualTo(2);

        HttpResponse<String> history = get("/api/v1/prices/IT-VERSION-1/history");
        assertThat(history.statusCode()).isEqualTo(200);
        List<Integer> versions = JsonPath.read(history.body(), "$[*].priceVersion");
        assertThat(versions).containsExactly(1, 2);
        assertThat((String) JsonPath.read(history.body(), "$[0].status")).isEqualTo("SUPERSEDED");
        assertThat((String) JsonPath.read(history.body(), "$[0].validTo")).contains("2026-06-30");
        assertThat((String) JsonPath.read(history.body(), "$[1].status")).isEqualTo("ACTIVE");
    }

    @Test
    void effectiveDatedLookupReturnsVersionValidOnDate_notJustLatest() {
        String productId = "IT-EFFECTIVE-1";
        priceService.createPrice(new CreatePriceRequest(productId, "INR", new BigDecimal("10000.00"),
                new BigDecimal("10000.00"), new BigDecimal("0.00"), LocalDate.of(2026, 1, 1)));
        priceService.createPrice(new CreatePriceRequest(productId, "INR", new BigDecimal("11000.00"),
                new BigDecimal("11000.00"), new BigDecimal("0.00"), LocalDate.of(2026, 7, 1)));

        PriceResponse inMarch = priceService.getPriceOn(productId, LocalDate.of(2026, 3, 1));
        PriceResponse lastDayOfV1 = priceService.getPriceOn(productId, LocalDate.of(2026, 6, 30));
        PriceResponse firstDayOfV2 = priceService.getPriceOn(productId, LocalDate.of(2026, 7, 1));

        assertThat(inMarch.priceVersion()).isEqualTo(1);
        assertThat(inMarch.sellingPrice()).isEqualByComparingTo("10000.00");
        assertThat(lastDayOfV1.priceVersion()).isEqualTo(1);
        assertThat(firstDayOfV2.priceVersion()).isEqualTo(2);
        assertThat(firstDayOfV2.sellingPrice()).isEqualByComparingTo("11000.00");

        List<ProductPrice> hits = priceRepository.findEffectiveOn(productId, LocalDate.of(2026, 12, 31));
        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getPriceVersion()).isEqualTo(2);
    }

    @Test
    void invalidRequest_returnsStructuredValidationError() throws Exception {
        String invalid = """
                {"productId":"","currency":"INR","basePrice":-1.00,"sellingPrice":10.00,
                 "discountPercent":150.00,"validFrom":"2026-10-03"}
                """;

        HttpResponse<String> response = post("/api/v1/prices", invalid);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat((String) JsonPath.read(response.body(), "$.error")).isEqualTo("VALIDATION_FAILED");
        List<String> details = JsonPath.read(response.body(), "$.details");
        assertThat(details).isNotEmpty();
    }

    @Test
    void businessRuleViolation_returns422() throws Exception {
        String invalid = """
                {"productId":"IT-INVALID-1","currency":"INR","basePrice":1000.00,"sellingPrice":1500.00,
                 "discountPercent":0.00,"validFrom":"2026-10-03"}
                """;

        HttpResponse<String> response = post("/api/v1/prices", invalid);

        assertThat(response.statusCode()).isEqualTo(422);
        assertThat((String) JsonPath.read(response.body(), "$.error")).isEqualTo("INVALID_PRICE");
    }
}
