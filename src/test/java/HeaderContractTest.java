import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class HeaderContractTest extends ApiTestBase {
    static Stream<Integer> cases() {
        return IntStream.rangeClosed(1, 26).boxed();
    }

    @ParameterizedTest(name = "headers are echoed for request {0}")
    @MethodSource("cases")
    void requiredAndNegotiationHeadersAreValidated(int number) {
        String correlationId = "correlation-" + number;
        String language = number % 3 == 0 ? "uz-UZ" : number % 3 == 1 ? "en-US" : "de-DE";
        given().header("X-Correlation-ID", correlationId).header("Accept-Language", language)
                .when().get("/headers")
                .then().statusCode(200)
                .header("X-Mock-API", "local")
                .body("correlationId", equalTo(correlationId))
                .body("language", equalTo(language));
    }
}
