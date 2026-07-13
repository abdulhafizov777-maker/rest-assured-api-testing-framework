import io.restassured.http.ContentType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

class DdtApiTest extends ApiTestBase {
    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/testdata/requests.csv", numLinesToSkip = 1)
    void apiDdt(String step, String method, String endpoint, int expectedStatus, String body) {
        var request = given().accept(ContentType.JSON).contentType(ContentType.JSON);
        if (body != null && !body.isBlank() && !body.equalsIgnoreCase("null")) {
            request.body(body);
        }

        var response = request.when().request(method, endpoint);
        response.then().statusCode(expectedStatus);
        if (expectedStatus != 204) {
            response.then().contentType("application/json").body(notNullValue());
        }
    }
}
