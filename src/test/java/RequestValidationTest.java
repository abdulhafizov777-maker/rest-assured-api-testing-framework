import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class RequestValidationTest extends ApiTestBase {
    static Stream<Integer> cases() {
        return IntStream.rangeClosed(1, 36).boxed();
    }

    @ParameterizedTest(name = "validation boundary case {0}")
    @MethodSource("cases")
    void requestBodyLengthIsStrictlyValidated(int number) {
        boolean valid = number <= 30;
        int length = valid ? number + 1 : number + 21;
        String value = "v".repeat(length);
        var assertion = given().contentType("application/json").body(Map.of("value", value))
                .when().post("/validate").then();
        if (valid) {
            assertion.statusCode(200)
                    .body("valid", equalTo(true))
                    .body("value", equalTo(value))
                    .body("length", equalTo(length));
        } else {
            assertion.statusCode(422)
                    .body("error", equalTo("invalid_length"))
                    .body("status", equalTo(422));
        }
    }
}
