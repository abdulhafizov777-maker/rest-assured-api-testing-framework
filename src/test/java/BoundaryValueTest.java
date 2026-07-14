import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class BoundaryValueTest extends ApiTestBase {
    static Stream<Integer> cases() {
        return Stream.concat(IntStream.rangeClosed(1, 21).boxed(),
                Stream.of(0, -1, -2, 101, 102, 103, 104, 105, 106, 107));
    }

    @ParameterizedTest(name = "page-size boundary {0}")
    @MethodSource("cases")
    void pageSizeAcceptsOnlyValuesFromOneThroughOneHundred(int size) {
        var assertion = given().queryParam("page", 99).queryParam("size", size)
                .when().get("/items").then();
        if (size >= 1 && size <= 100) {
            assertion.statusCode(200).body("size", equalTo(size));
        } else {
            assertion.statusCode(400)
                    .body("error", equalTo("invalid_pagination"))
                    .body("status", equalTo(400));
        }
    }
}
