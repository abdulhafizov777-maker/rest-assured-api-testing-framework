import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class DuplicateIdempotencyTest extends ApiTestBase {
    static Stream<Integer> cases() {
        return IntStream.rangeClosed(1, 24).boxed();
    }

    @ParameterizedTest(name = "duplicate/idempotency contract {0}")
    @MethodSource("cases")
    void duplicateCreationAndIdempotentOperationsHaveDistinctContracts(int number) {
        if (number <= 14) {
            String name = "duplicate-item-" + number;
            given().contentType("application/json").body(Map.of("name", name))
                    .when().post("/items").then().statusCode(409)
                    .body("error", equalTo("duplicate_item"))
                    .body("status", equalTo(409));
        } else {
            String key = "idempotency-key-" + number;
            long firstId = given().header("Idempotency-Key", key)
                    .when().post("/idempotency").then().statusCode(201)
                    .body("key", equalTo(key)).extract().jsonPath().getLong("operationId");
            long secondId = given().header("Idempotency-Key", key)
                    .when().post("/idempotency").then().statusCode(201)
                    .body("key", equalTo(key)).extract().jsonPath().getLong("operationId");
            org.junit.jupiter.api.Assertions.assertEquals(firstId, secondId,
                    "The same idempotency key must return the same operation ID");
        }
    }
}
