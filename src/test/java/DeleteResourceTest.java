import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class DeleteResourceTest extends ApiTestBase {
    static Stream<Integer> cases() {
        return IntStream.rangeClosed(3001, 3026).boxed();
    }

    @ParameterizedTest(name = "DELETE makes item {0} unavailable")
    @MethodSource("cases")
    void deleteIsObservableThroughSubsequentGet(int id) {
        given().pathParam("id", id).when().delete("/items/{id}").then().statusCode(204);
        given().pathParam("id", id).when().get("/items/{id}").then()
                .statusCode(404)
                .body("error", equalTo("item_not_found"))
                .body("status", equalTo(404));
    }
}
