package com.example;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LibraryApiClient {
    public static final String DEFAULT_BASE_URL = "http://localhost:8080/api/books";
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LibraryApiClient() {
        this(DEFAULT_BASE_URL);
    }

    public LibraryApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String getBooksUrl() {
        return baseUrl + "/api/books";
    }

    public List<Book> getAllBooks() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(getBooksUrl()))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), new TypeReference<List<Book>>() {
            });
        }
        throw exceptionForStatus(response);
    }


    public List<Book> getAllBooks_withRetries(int tries) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(getBooksUrl()))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();

        for (int tryNumber = 0; tryNumber < tries; tryNumber++) {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            switch (response.statusCode()) {
                case 200: {
                    return objectMapper.readValue(response.body(), new TypeReference<List<Book>>() {
                    });
                }
                case 503:  // retriable
                    if (tryNumber < tries-1)
                        continue;
                    // fall through to default:
                default:
                    throw exceptionForStatus(response);
            }

        }
        throw new RuntimeException("This code should not be reached!");
    }


    private Exception exceptionForStatus(HttpResponse<?> response) {
        return new LibraryApiException("Http request to %s failed with status code %s".formatted(response.uri(), response.statusCode()));
    }

    public Optional<Book> getBookById(long id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(getBooksUrl() + "/" + id))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return Optional.of(objectMapper.readValue(response.body(), Book.class));
        }
        return Optional.empty();
    }

    public Book createBook(Book book) throws Exception {
        String requestBody = objectMapper.writeValueAsString(book);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(getBooksUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readValue(response.body(), Book.class);
    }

    public Book updateBook(long id, Book book) throws Exception {
        String requestBody = objectMapper.writeValueAsString(book);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(getBooksUrl() + "/" + id))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readValue(response.body(), Book.class);
    }

    public void deleteBook(long id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(getBooksUrl() + "/" + id))
                .DELETE()
                .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public List<Book> searchBooksByAuthor(String author) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(getBooksUrl() + "/search?author=" + URLEncoder.encode(author, "UTF-8")))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readValue(response.body(), new TypeReference<List<Book>>() {
        });
    }
}
