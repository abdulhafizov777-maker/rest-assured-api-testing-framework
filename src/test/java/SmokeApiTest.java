import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class SmokeApiTest extends ApiTestBase {
    @Test
    void smokeShouldReturnHealthyLocalApiResponse() {
        given()
                .when().get("/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }
}
