import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class ErrorResponseTest extends ApiTestBase {
    static Stream<Arguments> cases() {
        Stream<Arguments> explicit = Stream.of(400, 401, 403, 404, 405, 409, 415, 422, 429)
                .map(code -> Arguments.of("GET", "/errors/" + code, null, code, "error_" + code));
        Stream<Arguments> invalidIds = IntStream.rangeClosed(1, 8)
                .mapToObj(i -> Arguments.of("GET", "/items/not-numeric-" + i, null, 400, "invalid_id"));
        Stream<Arguments> invalidPages = IntStream.rangeClosed(1, 8)
                .mapToObj(i -> Arguments.of("GET", "/items?page=-" + i + "&size=10", null, 400, "invalid_pagination"));
        Stream<Arguments> invalidBodies = Stream.of(
                "{}", "{\"status\":\"active\"}", "{\"name\":\"\"}", "{\"name\":\"   \"}",
                "{\"name\":null}", "{\"name\":42}", "{\"name\":true}", "{\"name\":[]}")
                .map(body -> Arguments.of("POST", "/items", body, 400, "invalid_name"));
        Stream<Arguments> protocol = Stream.of(
                Arguments.of("GET", "/headers", null, 400, "missing_header"),
                Arguments.of("POST", "/items/77", "{}", 405, "method_not_allowed"));
        return Stream.of(explicit, invalidIds, invalidPages, invalidBodies, protocol)
                .flatMap(stream -> stream);
    }

    @ParameterizedTest(name = "{0} {1} returns {3}/{4}")
    @MethodSource("cases")
    void errorResponsesExposeExactStatusAndMachineReadableCode(
            String method, String endpoint, String body, int status, String error) {
        var request = given().contentType("application/json");
        if (body != null) request.body(body);
        request.when().request(method, endpoint).then()
                .statusCode(status)
                .contentType("application/json")
                .body("status", equalTo(status))
                .body("error", equalTo(error));
    }
}
