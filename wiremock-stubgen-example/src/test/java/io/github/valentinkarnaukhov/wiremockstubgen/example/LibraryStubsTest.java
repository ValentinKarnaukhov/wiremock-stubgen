package io.github.valentinkarnaukhov.wiremockstubgen.example;

import com.example.library.model.Book;
import com.example.library.stubs.books.GetBookStub;
import com.example.library.stubs.books.SearchBooksStub;
import com.example.library.stubs.loans.BorrowBookStub;
import com.example.library.stubs.loans.ListEventsStub;
import com.example.library.stubs.loans.ListNotificationsStub;
import com.example.library.stubs.loans.ReturnLoanStub;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.valentinkarnaukhov.wiremockstubgen.runtime.StubTarget;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The generated stubs, used the way a test in a real project would use them.
 *
 * <p>The only place where the whole chain runs: the Maven plugin reads the specification
 * during the build, the stubs it writes are compiled against models a real
 * openapi-generator produced, and what they register is served by a real WireMock over a
 * real socket.
 *
 * <p>Hence the assertions are about responses, not about generated source; how a stub is
 * spelled is the goldens' business.
 */
class LibraryStubsTest {

    private static WireMockServer wireMock;
    private static HttpClient http;

    /**
     * Bodies are read as a tree rather than as text. WireMock pretty-prints what it
     * serialises, so asserting on the string would be asserting on its formatting.
     */
    private static final ObjectMapper JSON = new ObjectMapper();

    private StubTarget target;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
        http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void resetStubs() {
        wireMock.resetAll();
        target = StubTarget.of(wireMock);
    }

    /**
     * A response body described field by field. Nothing names {@code Author}: the builder
     * creates it because {@code authorName} needs it to exist.
     */
    @Test
    void servesABookDescribedFieldByField() throws Exception {
        new GetBookStub(target)
                .pathBookId("978-0201616224")
                .code200()
                    .title("The Pragmatic Programmer")
                    .authorName("Andrew Hunt")
                    .authorCountry("US")
                .mock();

        HttpResponse<String> response = get("/books/978-0201616224");

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = json(response);
        assertThat(body.at("/title").asText()).isEqualTo("The Pragmatic Programmer");
        assertThat(body.at("/author/name").asText()).isEqualTo("Andrew Hunt");
        assertThat(body.at("/author/country").asText()).isEqualTo("US");
        assertThat(body.has("id")).as("a field nobody described is not sent").isFalse();
    }

    /**
     * The same operation, its other declared status code. Both are typed because the
     * specification says 404 answers with a Problem.
     */
    @Test
    void servesTheDeclaredErrorForAnUnknownBook() throws Exception {
        new GetBookStub(target)
                .pathBookId("nothing")
                .code404()
                    .code("NOT_FOUND")
                    .message("No book with that identifier.")
                .mock();

        HttpResponse<String> response = get("/books/nothing");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(json(response).at("/code").asText()).isEqualTo("NOT_FOUND");
    }

    /**
     * Query parameters narrow the match, and a list of objects inside the body is
     * described element by element. {@code addNew()} is what says which element a value
     * belongs to.
     */
    @Test
    void servesASearchResultBuiltElementByElement() throws Exception {
        new SearchBooksStub(target)
                .queryAuthor("Andrew Hunt")
                .queryAvailableOnly(true)
                .code200()
                    .total(2)
                    .books()
                        .addNew().title("The Pragmatic Programmer")
                        .addNew().title("Pragmatic Unit Testing")
                .mock();

        HttpResponse<String> matching = get("/books?author=Andrew%20Hunt&availableOnly=true");
        assertThat(matching.statusCode()).isEqualTo(200);
        JsonNode page = json(matching);
        assertThat(page.at("/total").asInt()).isEqualTo(2);
        assertThat(page.at("/books/0/title").asText()).isEqualTo("The Pragmatic Programmer");
        assertThat(page.at("/books/1/title").asText()).isEqualTo("Pragmatic Unit Testing");

        assertThat(get("/books?author=Someone%20Else&availableOnly=true").statusCode())
                .isEqualTo(404);
    }

    /**
     * A request body matched field by field. The stub answers only requests whose body
     * carries these values, and says nothing about the rest of it — {@code days} is not
     * mentioned, so any value will do.
     */
    @Test
    void answersOnlyRequestsWhoseBodyMatches() throws Exception {
        new BorrowBookStub(target)
                .requestBody()
                    .bookId("978-0201616224")
                    .borrowerEmail("reader@example.com")
                .exit()
                .code201()
                    .id("loan-1")
                    .bookTitle("The Pragmatic Programmer")
                    .dueDate("2026-09-14")
                    .notifyByEmail(true)
                .mock();

        HttpResponse<String> created = post("/loans", """
                {
                  "bookId": "978-0201616224",
                  "borrower": { "name": "A Reader", "email": "reader@example.com" },
                  "days": 14
                }
                """);

        assertThat(created.statusCode()).isEqualTo(201);
        JsonNode loan = json(created);
        assertThat(loan.at("/id").asText()).isEqualTo("loan-1");
        assertThat(loan.at("/book/title").asText()).isEqualTo("The Pragmatic Programmer");
        assertThat(loan.at("/dueDate").asText()).isEqualTo("2026-09-14");
        // Reached through a scope the stub had to call _notify(), because notify() is
        // taken by every Java class. The name the caller uses is unaffected.
        assertThat(loan.at("/notify/byEmail").asBoolean()).isTrue();

        HttpResponse<String> unmatched = post("/loans", """
                {
                  "bookId": "978-0201616224",
                  "borrower": { "email": "someone.else@example.com" }
                }
                """);

        assertThat(unmatched.statusCode()).isEqualTo(404);
    }

    /**
     * A wire name is not an identifier: a dot in it belongs to the name and must not be
     * read as a step down the path. A list is matched by asking whether any element equals
     * the value, through a filter the value is written into, so a quote in it has to be
     * escaped rather than allowed to end the expression.
     */
    @Test
    void matchesOnAwkwardWireNamesAndOnListsOfNumbers() throws Exception {
        new BorrowBookStub(target)
                .requestBody()
                    .bookId("978-0201616224")
                    .loanReference("ref-1")
                    // A quote inside the value would otherwise end the filter expression
                    // early, and the stub would fail at request time rather than match.
                    .notes("O'Reilly")
                    .editions(3)
                .exit()
                .code201()
                    .id("loan-2")
                .mock();

        HttpResponse<String> created = post("/loans", """
                {
                  "bookId": "978-0201616224",
                  "loan.reference": "ref-1",
                  "notes": ["O'Reilly", "other"],
                  "editions": [1, 3, 7]
                }
                """);

        assertThat(created.statusCode()).isEqualTo(201);

        HttpResponse<String> nested = post("/loans", """
                {
                  "bookId": "978-0201616224",
                  "loan": { "reference": "ref-1" },
                  "notes": ["O'Reilly"],
                  "editions": [1, 3, 7]
                }
                """);

        assertThat(nested.statusCode())
                .describedAs("loan.reference is one name, not an object with a field in it")
                .isEqualTo(404);

        HttpResponse<String> otherEdition = post("/loans", """
                {
                  "bookId": "978-0201616224",
                  "loan.reference": "ref-1",
                  "notes": ["O'Reilly"],
                  "editions": [1, 7]
                }
                """);

        assertThat(otherEdition.statusCode())
                .describedAs("the list has to contain the edition asked for")
                .isEqualTo(404);
    }

    /**
     * A response written as {@code default} names no status of its own, so the stub takes
     * one from the caller rather than inventing 200 for a body that describes a failure.
     */
    @Test
    void servesTheCatchAllResponseUnderAStatusTheCallerChooses() throws Exception {
        new GetBookStub(target)
                .pathBookId("978-0201616224")
                .codeDefault(503)
                    .code("UNAVAILABLE")
                    .message("The catalogue is down.")
                .mock();

        HttpResponse<String> response = get("/books/978-0201616224");

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(json(response).at("/code").asText()).isEqualTo("UNAVAILABLE");
    }

    /**
     * The form that takes a body the caller already has, for the case the builders cannot
     * serve: a body assembled somewhere else.
     */
    @Test
    void servesABodyTheCallerAlreadyHas() throws Exception {
        Book book = new Book()
                .id("978-0132350884")
                .title("Clean Code");

        new GetBookStub(target)
                .pathBookId("978-0132350884")
                .code200(book)
                .mock();

        assertThat(json(get("/books/978-0132350884")).at("/title").asText()).isEqualTo("Clean Code");
    }

    /**
     * The shapes a large specification is written in, all reached through the same
     * accessors as everything else. Checked live, because none of these failures was
     * visible from the generated source alone.
     */
    @Test
    void servesTheShapesLargeSpecificationsAreWrittenIn() throws Exception {
        new GetBookStub(target)
                .pathBookId("978-0134685991")
                .code200()
                    .title("Effective Java")
                    // Written inside Book, so the model nests it and the stub says so too.
                    .status(Book.StatusEnum.ON_LOAN)
                    // Read-only: the model has a getter and no setter, and a stub playing
                    // the server still has to be able to send it.
                    .addedAt("2019-01-06")
                .mock();

        JsonNode book = json(get("/books/978-0134685991"));
        assertThat(book.at("/status").asText()).isEqualTo("ON_LOAN");
        assertThat(book.at("/addedAt").asText()).isEqualTo("2019-01-06");
    }

    /**
     * A composed resource reads as one flat set of accessors, because the models have no
     * inheritance: openapi-generator merges every member of an allOf into one class.
     */
    @Test
    void servesAComposedResourceThroughOneFlatSetOfAccessors() throws Exception {
        new BorrowBookStub(target)
                .code201()
                    // id and href come from Reference, dueDate from the member written in
                    // place. Nothing in the generated API says which.
                    .id("loan-1")
                    .href("/loans/loan-1")
                    .dueDate("2024-03-01")
                    .bookTitle("Effective Java")
                .mock();

        JsonNode loan = json(post("/loans", "{\"bookId\":\"978-0134685991\"}"));
        assertThat(loan.at("/id").asText()).isEqualTo("loan-1");
        assertThat(loan.at("/href").asText()).isEqualTo("/loans/loan-1");
        assertThat(loan.at("/dueDate").asText()).isEqualTo("2024-03-01");
        assertThat(loan.at("/book/title").asText()).isEqualTo("Effective Java");
    }

    /**
     * A body written out in place instead of declared, and an error body the specification
     * mentions only by reference. Neither is named anywhere the caller can see.
     */
    @Test
    void servesAnInlineBodyAndASharedErrorBody() throws Exception {
        new ReturnLoanStub(target)
                .pathLoanId("loan-1")
                .code200()
                    .returnedAt("2024-02-20")
                    .lateFeeAmount(new java.math.BigDecimal("2.50"))
                    .lateFeeCurrency("EUR")
                .mock();

        JsonNode returned = json(delete("/loans/loan-1"));
        assertThat(returned.at("/returnedAt").asText()).isEqualTo("2024-02-20");
        assertThat(returned.at("/lateFee/currency").asText()).isEqualTo("EUR");

        wireMock.resetAll();
        new ReturnLoanStub(StubTarget.of(wireMock))
                .pathLoanId("gone")
                // Reachable only because the reference to components/responses was
                // followed; before it was, this response had no body and no method.
                .code404()
                    .code("NOT_FOUND")
                .mock();

        assertThat(json(delete("/loans/gone")).at("/code").asText()).isEqualTo("NOT_FOUND");
    }

    /**
     * A body a specification describes with {@code oneOf}.
     *
     * <p>Served live because the two generator versions disagree about what such a schema
     * is called: a composition naming one alternative has that alternative's shape, and
     * reading it as the member is the only reading that compiles under both. Hence the
     * accessors below are the member's, flattening on through the reference to a borrower.
     */
    @Test
    void servesABodyDescribedAsOneOfASingleAlternative() throws Exception {
        new ListEventsStub(target)
                .code200()
                    .eventType("LOAN_RETURNED")
                    .loanId("loan-1")
                    .borrowerName("Ada")
                .mock();

        JsonNode event = json(get("/events"));
        assertThat(event.at("/eventType").asText()).isEqualTo("LOAN_RETURNED");
        assertThat(event.at("/borrower/name").asText()).isEqualTo("Ada");
    }

    /**
     * A cookie is not a header under another name: WireMock matches it through a call of
     * its own, and a stub that put it among the headers would match nothing.
     */
    @Test
    void answersOnlyRequestsCarryingTheCookie() throws Exception {
        new GetBookStub(target)
                .pathBookId("978-0201616224")
                .cookieSession("abc")
                .code200()
                    .title("The Pragmatic Programmer")
                .mock();

        HttpResponse<String> withCookie = get("/books/978-0201616224", "session=abc");
        assertThat(withCookie.statusCode()).isEqualTo(200);

        assertThat(get("/books/978-0201616224").statusCode())
                .as("no cookie, no match").isEqualTo(404);
        assertThat(get("/books/978-0201616224", "session=other").statusCode())
                .as("the wrong cookie is no better than none").isEqualTo(404);
    }

    /**
     * Values that are not strings. Two formats the model generator gives types of their
     * own, a schema whose only rule is about the properties that turn up, and a list whose
     * elements are themselves lists — which has no position to open, so it is set whole.
     */
    @Test
    void servesFormattedMappedAndDoublyNestedValues() throws Exception {
        new GetBookStub(target)
                .pathBookId("978-0201616224")
                .code200()
                    .isbn(UUID.fromString("11111111-2222-3333-4444-555555555555"))
                    .publishedOn(LocalDate.of(1999, 10, 20))
                    .ratings(Map.of("clarity", 5))
                    .chapters(List.of(List.of("Preface"), List.of("Tools", "Debugging")))
                    .coverUrl(URI.create("https://example.com/covers/978-0201616224.png"))
                    .publisherSite("https://example.com")
                .mock();

        JsonNode book = json(get("/books/978-0201616224"));
        assertThat(book.at("/isbn").asText()).isEqualTo("11111111-2222-3333-4444-555555555555");
        assertThat(book.at("/publishedOn").asText()).isEqualTo("1999-10-20");
        assertThat(book.at("/ratings/clarity").asInt()).isEqualTo(5);
        assertThat(book.at("/chapters/1/1").asText()).isEqualTo("Debugging");
        assertThat(book.at("/coverUrl").asText())
                .isEqualTo("https://example.com/covers/978-0201616224.png");
        assertThat(book.at("/publisherSite").asText()).isEqualTo("https://example.com");
    }

    /**
     * The same {@code oneOf} under both readings, which is the whole point of having two.
     *
     * <p>Merging folds both alternatives into one class, so the stub offers accessors over
     * every member's properties. Reading it as opaque goes with
     * {@code useOneOfInterfaces=true}, where the model is an empty interface with nothing
     * to set, and the only thing a stub can do is take the body whole.
     */
    @Test
    void servesAOneOfBothWaysRound() throws Exception {
        new ListNotificationsStub(target)
                .code200()
                    .address("reader@example.com")
                    .subject("Your book is due")
                .mock();

        JsonNode merged = json(get("/notifications"));
        assertThat(merged.at("/address").asText()).isEqualTo("reader@example.com");
        assertThat(merged.at("/subject").asText()).isEqualTo("Your book is due");

        wireMock.resetAll();

        com.example.library.opaque.model.EmailNotification whole =
                new com.example.library.opaque.model.EmailNotification()
                        .address("reader@example.com")
                        .subject("Your book is due");
        new com.example.library.opaque.stubs.loans.ListNotificationsStub(target)
                .code200(whole)
                .mock();

        JsonNode opaque = json(get("/notifications"));
        assertThat(opaque.at("/address").asText()).isEqualTo("reader@example.com");
    }

    private static JsonNode json(HttpResponse<String> response) throws IOException {
        return JSON.readTree(response.body());
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(uri(path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String cookie) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(uri(path)).header("Cookie", cookie).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(uri(path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> delete(String path) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(uri(path)).DELETE().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create(wireMock.baseUrl() + path);
    }
}
