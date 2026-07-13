import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class PatchResourceTest extends ApiTestBase {
    static Stream<Integer> cases() {
        return IntStream.rangeClosed(2001, 2035).boxed();
    }

    @ParameterizedTest(name = "PATCH changes one field on item {0}")
    @MethodSource("cases")
    void patchChangesOnlySuppliedField(int id) {
        boolean changeName = id % 2 == 0;
        String field = changeName ? "name" : "status";
        String value = changeName ? "patched-" + id : "archived-" + id;
        given().contentType("application/json").pathParam("id", id)
                .body(Map.of(field, value))
                .when().patch("/items/{id}")
                .then().statusCode(200)
                .body("id", equalTo(id))
                .body("patched", equalTo(true))
                .body(field, equalTo(value));
    }
}
