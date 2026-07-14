import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class PutResourceTest extends ApiTestBase {
    static Stream<Integer> cases() {
        return IntStream.rangeClosed(1001, 1031).boxed();
    }

    @ParameterizedTest(name = "PUT fully replaces item {0}")
    @MethodSource("cases")
    void putReplacesEveryRequiredField(int id) {
        String name = "replacement-" + id;
        String status = id % 2 == 0 ? "active" : "inactive";
        given().contentType("application/json").pathParam("id", id)
                .body(Map.of("name", name, "status", status))
                .when().put("/items/{id}")
                .then().statusCode(200)
                .body("id", equalTo(id))
                .body("name", equalTo(name))
                .body("status", equalTo(status))
                .body("replaced", equalTo(true));
    }
}
