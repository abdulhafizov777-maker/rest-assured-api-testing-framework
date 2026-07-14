import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class AuthenticationTest extends ApiTestBase {
    static Stream<Integer> cases() {
        return IntStream.rangeClosed(1, 20).boxed();
    }

    @ParameterizedTest(name = "authentication case {0}")
    @MethodSource("cases")
    void bearerAuthenticationDistinguishesValidMissingAndInvalidTokens(int number) {
        if (number <= 15) {
            String token = "token-" + number;
            given().header("Authorization", "Bearer " + token)
                    .when().get("/auth").then().statusCode(200)
                    .body("authenticated", equalTo(true))
                    .body("subject", equalTo(token));
        } else if (number == 16) {
            given().when().get("/auth").then().statusCode(401)
                    .body("error", equalTo("missing_token"));
        } else {
            String token = "invalid-token-" + number;
            given().header("Authorization", "Bearer " + token)
                    .when().get("/auth").then().statusCode(403)
                    .body("error", equalTo("invalid_token"));
        }
    }
}
