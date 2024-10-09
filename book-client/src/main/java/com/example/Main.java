package com.example;

import com.github.javafaker.Faker;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");

        var client = new LibraryApiClient();
        try {
            // create some books
            System.out.println("----- create some books ----------");
            Faker faker = new Faker();
            for (int i = 0; i < 10; i++) {
                Book newBook = createSomeBook(faker);
                client.createBook(newBook);
            }

            // get all books
            System.out.println("----- get all books ----------");
            List<Book> allBooks = client.getAllBooks();
            System.out.println("All books: \n  " + allBooks.stream().limit(5).map(Book::toString).collect(Collectors.joining("\n  ")));

            System.out.println("----- get book by id ----------");
            long someKnownBookId = Optional.of(client.getAllBooks()).stream().flatMap(Collection::stream).findAny().map(Book::getId).orElse(0L);
            Book bookById = client.getBookById(someKnownBookId).orElseThrow();
            System.out.println("Book by id " + someKnownBookId + ": " + bookById);

            System.out.println("----- update book ----------");
            bookById.setTitle(">Updated book title<");
            client.updateBook(someKnownBookId, bookById);

            Book bookById_version2 = client.getBookById(someKnownBookId).orElseThrow();
            System.out.println("Book by id " + someKnownBookId + " after update: " + bookById_version2);

            System.out.println("----- search books by author ----------");
            List<Book> foundBooks = client.searchBooksByAuthor(bookById.getAuthor());
            System.out.println("Found books: " + foundBooks);

            System.out.println("----- delete a book by id ----------");
            client.deleteBook(someKnownBookId);
            client.getBookById(someKnownBookId)
                    .ifPresentOrElse(b -> {
                                throw new RuntimeException("Found book that is supposed to be deleted!: " + b);
                            },
                            () -> System.out.println("deleted."));


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Book createSomeBook(Faker faker) {
        com.github.javafaker.Book book = faker.book();
        Book fakedBook = Book.builder()
                .title(book.title())
                .author(book.author())
                .isbn(faker.numerify("##########"))
                .build();
        return fakedBook;
    }
}