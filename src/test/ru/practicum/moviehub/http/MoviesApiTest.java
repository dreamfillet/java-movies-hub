package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.model.Movie;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.GsonBuilder;

public class MoviesApiTest {
    private static MoviesServer server;
    private static HttpClient client;
    private static Gson gson;
    private static final String BASE = "http://localhost:8080";

    @BeforeAll
    static void beforeAll() {

        server = new MoviesServer(8080);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    @BeforeEach
    void beforeEach() {
        server.getHandler().getStore().clear();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());

        assertNotNull(movies);
        assertTrue(movies.isEmpty(), "Список должен быть пустым");
    }

    @Test
    void getMovies_whenMoviesExist_returnsMoviesList() throws Exception {

        addMovie("Movie 1", 2023);
        addMovie("Movie 2", 2024);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());

        assertNotNull(movies);
        assertEquals(2, movies.size(), "Должно быть 2 фильма");

        Movie movie1 = movies.get(0);
        Movie movie2 = movies.get(1);

        assertEquals("Movie 1", movie1.getTitle());
        assertEquals(2023, movie1.getYear());

        assertEquals("Movie 2", movie2.getTitle());
        assertEquals(2024, movie2.getYear());
    }

    @Test
    void postMovies_whenValid_returns201() throws Exception {
        Movie newMovie = new Movie(0, "The Matrix", 1999);
        String json = gson.toJson(newMovie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode());

        Movie created = gson.fromJson(resp.body(), Movie.class);

        assertNotNull(created);
        assertTrue(created.getId() > 0, "ID должен быть больше 0");
        assertEquals("The Matrix", created.getTitle());
        assertEquals(1999, created.getYear());
    }

    @Test
    void postMovies_whenEmptyTitle_returns422() throws Exception {
        Movie movie = new Movie(0, "", 2024);
        String json = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());

        String body = resp.body();
        assertTrue(body.contains("Ошибка валидации"));
        assertTrue(body.contains("details"));
    }

    @Test
    void postMovies_whenTitleTooLong_returns422() throws Exception {
        String longTitle = "a".repeat(101);
        Movie movie = new Movie(0, longTitle, 2024);
        String json = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
    }

    @Test
    void postMovies_whenYearTooLow_returns422() throws Exception {
        Movie movie = new Movie(0, "Test", 1800);
        String json = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
    }

    @Test
    void postMovies_whenWrongContentType_returns415() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("{}", StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, resp.statusCode());
    }

    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {
        Movie newMovie = new Movie(0, "Test Movie", 2023);
        String postJson = gson.toJson(newMovie);

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(postJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> postResp = client.send(postReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Movie created = gson.fromJson(postResp.body(), Movie.class);

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + created.getId()))
                .GET()
                .build();

        HttpResponse<String> getResp = client.send(getReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, getResp.statusCode());

        Movie movie = gson.fromJson(getResp.body(), Movie.class);

        assertEquals(created.getId(), movie.getId());
        assertEquals("Test Movie", movie.getTitle());
        assertEquals(2023, movie.getYear());
    }

    @Test
    void getMovieById_whenNotFound_returns404() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/99999"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());
    }

    @Test
    void getMovieById_whenIdNotNumber_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
    }

    @Test
    void deleteMovie_whenExists_returns204() throws Exception {
        Movie newMovie = new Movie(0, "To Delete", 2024);
        String postJson = gson.toJson(newMovie);

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(postJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> postResp = client.send(postReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Movie created = gson.fromJson(postResp.body(), Movie.class);

        HttpRequest deleteReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + created.getId()))
                .DELETE()
                .build();

        HttpResponse<String> deleteResp = client.send(deleteReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, deleteResp.statusCode());
    }

    @Test
    void deleteMovie_whenNotFound_returns404() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/99999"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());
    }

    @Test
    void getMoviesByYear_whenExists_returnsFiltered() throws Exception {
        addMovie("Movie 2023", 2023);
        addMovie("Movie 2024", 2024);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2024"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());

        assertNotNull(movies);
        assertEquals(1, movies.size(), "Должен быть 1 фильм 2024 года");

        Movie movie = movies.get(0);
        assertEquals("Movie 2024", movie.getTitle());
        assertEquals(2024, movie.getYear());
    }

    @Test
    void getMoviesByYear_whenNoMovies_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1900"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());

        assertNotNull(movies);
        assertTrue(movies.isEmpty(), "Список должен быть пустым");
    }

    @Test
    void getMoviesByYear_whenYearNotNumber_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=abc"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
    }

    private void addMovie(String title, int year) throws Exception {
        Movie movie = new Movie(0, title, year);
        String json = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}