import com.example.Book;
import com.example.LibraryApiClient;
import com.example.LibraryApiException;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@WireMockTest
public class LibraryApiClientTest {

    public static final String AFTER_FIRST_FAILURE = "first failure";
    public static final String AFTER_SECOND_FAILURE = "second failure";

    /**
     * When the server returns a list of books.
     */
    @Test
    public void testGetAllBooks_ok(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        // given
        List<Book> givenResult = List.of(
                Book.builder().isbn("345435435").author("Hubert Meier").title("Was auch immer.").build(),
                Book.builder().isbn("786778676").author("Dagmar Huber").title("Testen für Anfänger.").build()
        );
        stubFor(get("/api/books").willReturn(jsonResponse(givenResult, HttpStatus.SC_OK)));
        LibraryApiClient client = new LibraryApiClient(wmRuntimeInfo.getHttpBaseUrl());

        // when
        List<Book> result = client.getAllBooks();

        // then
        Assertions.assertEquals(givenResult, result);
    }

    /**
     * When the server returns an empty list.
     */
    @Test
    public void testGetAllBooks_emptyList(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        // given
        List<Book> givenResult = Collections.emptyList();
        stubFor(get("/api/books").willReturn(jsonResponse(givenResult, HttpStatus.SC_OK)));
        LibraryApiClient client = new LibraryApiClient(wmRuntimeInfo.getHttpBaseUrl());

        // when
        List<Book> result = client.getAllBooks();

        // then
        Assertions.assertEquals(Collections.emptyList(), result);
    }


    /**
     * When the server has an internal problem and responds with 500
     */
    @Test
    public void testGetAllBooks_500(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        // given
        final String httpBaseUrl = wmRuntimeInfo.getHttpBaseUrl();
        stubFor(get("/api/books").willReturn(aResponse().withBody("Internal server error.").withStatus(500)));
        LibraryApiClient client = new LibraryApiClient(httpBaseUrl);

        // when
        LibraryApiException thrown = Assertions.assertThrows(LibraryApiException.class, () -> client.getAllBooks());

        // then
        Assertions.assertEquals("Http request to %s/api/books failed with status code 500".formatted(httpBaseUrl), thrown.getMessage());
    }


    /**
     * Simulate 2x503, then 200, using the method getAllBooks() without retries
     */
    @Test
    public void testGetAllBooks_successfulRetry(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        // given
        final String wmScenario = "getAllBook retries";
        List<Book> givenResult = List.of(
                Book.builder().isbn("345435435").author("Hubert Meier").title("Was auch immer.").build(),
                Book.builder().isbn("786778676").author("Dagmar Huber").title("Testen für Anfänger.").build()
        );

        final String httpBaseUrl = wmRuntimeInfo.getHttpBaseUrl();
        stubFor(get("/api/books")
                .inScenario(wmScenario)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withBody("Temporary server error.").withStatus(503))
                .willSetStateTo(AFTER_FIRST_FAILURE));
        stubFor(get("/api/books")
                .inScenario(wmScenario)
                .whenScenarioStateIs(AFTER_FIRST_FAILURE)
                .willReturn(aResponse().withBody("Temporary server error.").withStatus(503))
                .willSetStateTo(AFTER_SECOND_FAILURE));
        stubFor(get("/api/books")
                .inScenario(wmScenario)
                .whenScenarioStateIs(AFTER_SECOND_FAILURE)
                .willReturn(jsonResponse(givenResult, HttpStatus.SC_OK)));

        // when
        final LibraryApiClient client = new LibraryApiClient(httpBaseUrl);
        // first try:
        LibraryApiException thrown = Assertions.assertThrows(LibraryApiException.class, () -> client.getAllBooks());
        Assertions.assertEquals("Http request to %s/api/books failed with status code 503".formatted(httpBaseUrl), thrown.getMessage());
        // second try:
        LibraryApiException thrown2 = Assertions.assertThrows(LibraryApiException.class, () -> client.getAllBooks());
        Assertions.assertEquals("Http request to %s/api/books failed with status code 503".formatted(httpBaseUrl), thrown2.getMessage());
        // successful try:
        List<Book> result = client.getAllBooks();

        // then
        Assertions.assertEquals(givenResult, result);
    }


    /**
     * Simulate 2x503, then 200, using the method getAllBooks_withRetries()
     */
    @Test
    public void testGetAllBooksWithRetries_successfulRetry(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        // given
        final String wmScenario = "getAllBook retries";
        List<Book> givenResult = List.of(
                Book.builder().isbn("345435435").author("Hubert Meier").title("Was auch immer.").build(),
                Book.builder().isbn("786778676").author("Dagmar Huber").title("Testen für Anfänger.").build()
        );

        final String httpBaseUrl = wmRuntimeInfo.getHttpBaseUrl();
        stubFor(get("/api/books")
                .inScenario(wmScenario)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withBody("Temporary server error.").withStatus(503))
                .willSetStateTo(AFTER_FIRST_FAILURE));
        stubFor(get("/api/books")
                .inScenario(wmScenario)
                .whenScenarioStateIs(AFTER_FIRST_FAILURE)
                .willReturn(aResponse().withBody("Temporary server error.").withStatus(503))
                .willSetStateTo(AFTER_SECOND_FAILURE));
        stubFor(get("/api/books")
                .inScenario(wmScenario)
                .whenScenarioStateIs(AFTER_SECOND_FAILURE)
                .willReturn(jsonResponse(givenResult, HttpStatus.SC_OK)));

        // when
        final LibraryApiClient client = new LibraryApiClient(httpBaseUrl);
        List<Book> result = client.getAllBooks_withRetries(3);

        // then
        Assertions.assertEquals(givenResult, result);
    }


    /**
     * Simulate 2x503, then 500
     */
    @Test
    public void testGetAllBooksWithRetries_failureAfterRetry(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        // given
        final String wmScenario = "getAllBook retries";
        List<Book> givenResult = List.of(
                Book.builder().isbn("345435435").author("Hubert Meier").title("Was auch immer.").build(),
                Book.builder().isbn("786778676").author("Dagmar Huber").title("Testen für Anfänger.").build()
        );

        final String httpBaseUrl = wmRuntimeInfo.getHttpBaseUrl();
        stubFor(get("/api/books")
                .inScenario(wmScenario)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withBody("Temporary server error.").withStatus(503))
                .willSetStateTo(AFTER_FIRST_FAILURE));
        stubFor(get("/api/books")
                .inScenario(wmScenario)
                .whenScenarioStateIs(AFTER_FIRST_FAILURE)
                .willReturn(aResponse().withBody("Temporary server error.").withStatus(503))
                .willSetStateTo(AFTER_SECOND_FAILURE));
        stubFor(get("/api/books")
                .inScenario(wmScenario)
                .whenScenarioStateIs(AFTER_SECOND_FAILURE)
                .willReturn(aResponse().withStatus(500)));

        // when
        final LibraryApiClient client = new LibraryApiClient(httpBaseUrl);
        LibraryApiException thrown = Assertions.assertThrows(LibraryApiException.class, () -> client.getAllBooks_withRetries(3));

        // then
        Assertions.assertEquals("Http request to %s/api/books failed with status code 500".formatted(httpBaseUrl), thrown.getMessage());
    }


    /**
     * Simulate 2x503, then 200, but only 2 tries
     * @param wmRuntimeInfo
     * @throws Exception
     */
    @Test
    public void testGetAllBooksWithRetries_notEnoughTries(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        // given
        final String wmScenario = "getAllBook retries";
        List<Book> givenResult = List.of(
                Book.builder().isbn("345435435").author("Hubert Meier").title("Was auch immer.").build(),
                Book.builder().isbn("786778676").author("Dagmar Huber").title("Testen für Anfänger.").build()
        );

        final String httpBaseUrl = wmRuntimeInfo.getHttpBaseUrl();
        stubFor(get("/api/books")
                .inScenario(wmScenario)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withBody("Temporary server error.").withStatus(503))
                .willSetStateTo(AFTER_FIRST_FAILURE));
        stubFor(get("/api/books")
                .inScenario(wmScenario)
                .whenScenarioStateIs(AFTER_FIRST_FAILURE)
                .willReturn(aResponse().withBody("Temporary server error.").withStatus(503))
                .willSetStateTo(AFTER_SECOND_FAILURE));
        stubFor(get("/api/books")
                .inScenario(wmScenario)
                .whenScenarioStateIs(AFTER_SECOND_FAILURE)
                .willReturn(jsonResponse(givenResult, HttpStatus.SC_OK)));

        // when
        final LibraryApiClient client = new LibraryApiClient(httpBaseUrl);
        LibraryApiException thrown = Assertions.assertThrows(LibraryApiException.class, () -> client.getAllBooks_withRetries(2));

        // then
        Assertions.assertEquals("Http request to %s/api/books failed with status code 503".formatted(httpBaseUrl), thrown.getMessage());
    }
}
