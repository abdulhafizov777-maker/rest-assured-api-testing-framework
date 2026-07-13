import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostResourceTest extends ApiTestBase {
    static Stream<Integer> cases() {
        return IntStream.rangeClosed(1, 35).boxed();
    }

    @ParameterizedTest(name = "POST creates distinct item {0}")
    @MethodSource("cases")
    void postCreatesResourceWithValidatedResponse(int number) {
        String name = "post-item-" + number;
        String status = number % 2 == 0 ? "active" : "draft";
        var response = given().contentType("application/json").body(Map.of("name", name, "status", status))
                .when().post("/items")
                .then().statusCode(201)
                .body("name", equalTo(name))
                .body("status", equalTo(status))
                .extract().response();
        Number id = response.jsonPath().get("id");
        assertTrue(id.longValue() > 0, "Created resource ID must be positive");
    }
}
