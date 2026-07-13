import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class SortingFilteringTest extends ApiTestBase {
    static Stream<Arguments> cases() {
        return Stream.of("id", "name", "status", "category").flatMap(sort ->
                Stream.of("asc", "desc").flatMap(order ->
                        Stream.of("active", "draft", "inactive", "archived", "pending")
                                .map(status -> Arguments.of(sort, order, status))));
    }

    @ParameterizedTest(name = "sort={0}, order={1}, status={2}")
    @MethodSource("cases")
    void sortingAndFilteringOptionsArePreserved(String sort, String order, String status) {
        String category = "category-" + sort + "-" + order + "-" + status;
        given().queryParam("sort", sort).queryParam("order", order)
                .queryParam("status", status).queryParam("category", category)
                .when().get("/items").then().statusCode(200)
                .body("sort", equalTo(sort))
                .body("order", equalTo(order))
                .body("status", equalTo(status))
                .body("category", equalTo(category));
    }
}
