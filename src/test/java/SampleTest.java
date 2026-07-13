import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class SampleTest extends ApiTestBase {
    @Test
    void localApiHealthIncludesServiceIdentity() {
        given()
                .when().get("/health")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .header("X-Mock-API", "local")
                .body("status", equalTo("UP"))
                .body("service", equalTo("local-mock-api"));
    }
}
