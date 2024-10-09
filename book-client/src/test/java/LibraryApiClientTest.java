import com.example.Book;
import com.example.LibraryApiClient;
import com.example.LibraryApiException;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

public class LibraryApiClientTest {

    /**
     * When the server returns a list of books.
     */
    @Test
    public void testGetAllBooks_ok() throws Exception {
        // given
        List<Book> givenResult = List.of(
                Book.builder().isbn("345435435").author("Hubert Meier").title("Was auch immer.").build(),
                Book.builder().isbn("786778676").author("Dagmar Huber").title("Testen für Anfänger.").build()
        );

        // when
        LibraryApiClient client = new LibraryApiClient();
        List<Book> result = client.getAllBooks();

        // then
        Assertions.assertEquals(givenResult, result);
    }

    /**
     * When the server returns an empty list.
     */
    @Test
    public void testGetAllBooks_emptyList() throws Exception {
        // given
        List<Book> givenResult = Collections.emptyList();

        // when
        LibraryApiClient client = new LibraryApiClient();
        List<Book> result = client.getAllBooks();

        // then
        Assertions.assertEquals(Collections.emptyList(), result);
    }


    /**
     * When the server has an internal problem and responds with 500
     */
    @Test
    public void testGetAllBooks_500() throws Exception {
        // given
        final String httpBaseUrl = LibraryApiClient.DEFAULT_BASE_URL;

        // when
        LibraryApiClient client = new LibraryApiClient(httpBaseUrl);
        LibraryApiException thrown = Assertions.assertThrows(LibraryApiException.class, () -> client.getAllBooks());

        // then
        Assertions.assertEquals("Http request to %s failed with status code 500".formatted(httpBaseUrl), thrown.getMessage());
    }


    /**
     * Simulate 2x503, then 200, using the method getAllBooks() without retries
     */
    @Test
    public void testGetAllBooks_successfulRetry() throws Exception {
        // given
        List<Book> givenResult = List.of(
                Book.builder().isbn("345435435").author("Hubert Meier").title("Was auch immer.").build(),
                Book.builder().isbn("786778676").author("Dagmar Huber").title("Testen für Anfänger.").build()
        );

        final String httpBaseUrl = LibraryApiClient.DEFAULT_BASE_URL;

        // when
        final LibraryApiClient client = new LibraryApiClient(httpBaseUrl);
        // first try:
        LibraryApiException thrown = Assertions.assertThrows(LibraryApiException.class, () -> client.getAllBooks());
        Assertions.assertEquals("Http request to %s failed with status code 503".formatted(httpBaseUrl), thrown.getMessage());
        // second try:
        LibraryApiException thrown2 = Assertions.assertThrows(LibraryApiException.class, () -> client.getAllBooks());
        Assertions.assertEquals("Http request to %s failed with status code 503".formatted(httpBaseUrl), thrown2.getMessage());
        // successful try:
        List<Book> result = client.getAllBooks();

        // then
        Assertions.assertEquals(givenResult, result);
    }


    /**
     * Simulate 2x503, then 200, using the method getAllBooks_withRetries()
     */
    @Test
    public void testGetAllBooksWithRetries_successfulRetry() throws Exception {
        // given
        final String wmScenario = "getAllBook retries";
        List<Book> givenResult = List.of(
                Book.builder().isbn("345435435").author("Hubert Meier").title("Was auch immer.").build(),
                Book.builder().isbn("786778676").author("Dagmar Huber").title("Testen für Anfänger.").build()
        );

        final String httpBaseUrl = LibraryApiClient.DEFAULT_BASE_URL;

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
    public void testGetAllBooksWithRetries_failureAfterRetry() throws Exception {
        // given
        final String wmScenario = "getAllBook retries";
        List<Book> givenResult = List.of(
                Book.builder().isbn("345435435").author("Hubert Meier").title("Was auch immer.").build(),
                Book.builder().isbn("786778676").author("Dagmar Huber").title("Testen für Anfänger.").build()
        );

        final String httpBaseUrl = LibraryApiClient.DEFAULT_BASE_URL;

        // when
        final LibraryApiClient client = new LibraryApiClient(httpBaseUrl);
        LibraryApiException thrown = Assertions.assertThrows(LibraryApiException.class, () -> client.getAllBooks_withRetries(3));

        // then
        Assertions.assertEquals("Http request to %s failed with status code 500".formatted(httpBaseUrl), thrown.getMessage());
    }



}
