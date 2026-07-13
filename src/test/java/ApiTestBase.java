import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;

abstract class ApiTestBase {
    @BeforeAll
    static void configureLocalApi() {
        RestAssured.baseURI = LocalMockApi.baseUri();
    }
}
