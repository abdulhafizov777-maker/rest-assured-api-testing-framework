import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class QueryPathParameterTest extends ApiTestBase {
    static Stream<Integer> cases() {
        return IntStream.rangeClosed(1, 40).boxed();
    }

    @ParameterizedTest(name = "path and query parameters remain distinct in case {0}")
    @MethodSource("cases")
    void pathAndQueryParametersAreBoundCorrectly(int number) {
        int id = 10_000 + number;
        int page = number;
        int size = number % 10 + 1;
        given().pathParam("id", id).when().get("/items/{id}").then()
                .statusCode(200).body("id", equalTo(id));
        given().queryParam("page", page).queryParam("size", size)
                .when().get("/items").then().statusCode(200)
                .body("page", equalTo(page))
                .body("size", equalTo(size))
                .body("offset", equalTo((page - 1) * size));
    }
}
