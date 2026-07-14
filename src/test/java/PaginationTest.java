import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class PaginationTest extends ApiTestBase {
    static Stream<Integer> cases() {
        return IntStream.rangeClosed(1, 31).boxed();
    }

    @ParameterizedTest(name = "pagination calculates offset for page {0}")
    @MethodSource("cases")
    void paginationMetadataUsesRequestedPageAndSize(int page) {
        int size = page % 20 + 1;
        given().queryParam("page", page).queryParam("size", size)
                .when().get("/items").then().statusCode(200)
                .body("page", equalTo(page))
                .body("size", equalTo(size))
                .body("offset", equalTo((page - 1) * size));
    }
}
