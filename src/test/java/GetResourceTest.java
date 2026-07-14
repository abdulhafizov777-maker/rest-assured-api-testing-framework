import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class GetResourceTest extends ApiTestBase {
    @ParameterizedTest(name = "GET returns item {0}")
    @ValueSource(ints = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,
            21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36})
    void getReturnsRequestedResource(int id) {
        given().pathParam("id", id)
                .when().get("/items/{id}")
                .then().statusCode(200)
                .body("id", equalTo(id))
                .body("name", equalTo("item-" + id))
                .body("status", equalTo("active"));
    }
}
