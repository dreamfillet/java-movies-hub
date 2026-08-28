package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.time.Year;
import java.util.List;
import java.util.Map;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    public MoviesStore getStore() {
        return store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            String query = ex.getRequestURI().getQuery();

            // GET/movies
            if (method.equalsIgnoreCase("GET") && path.equals("/movies")) {
                handleGetMovies(ex, query);
                return;
            }

            // POST/movies
            if (method.equalsIgnoreCase("POST") && path.equals("/movies")) {
                handlePostMovie(ex);
                return;
            }

            // GET/movies/{id}
            if (method.equalsIgnoreCase("GET") && path.matches("/movies/\\d+")) {
                handleGetMovieById(ex);
                return;
            }

            // DELETE/movies/{id}
            if (method.equalsIgnoreCase("DELETE") && path.matches("/movies/\\d+")) {
                handleDeleteMovie(ex);
                return;
            }

            // Если путь начинается с /movies/, ID не число
            if (path.startsWith("/movies/")) {
                sendError(ex, 400, "Некорректный ID");
                return;
            }

            sendError(ex, 405, "Метод не поддерживается");
        } catch (Exception e) {
            sendError(ex, 500, "Внутренняя ошибка сервера");
        }
    }

    //GET/movies
    private void handleGetMovies(HttpExchange ex, String query) throws IOException {

        if (query != null && query.contains("year=")) {
            handleGetMoviesByYear(ex, query);
            return;
        }

        List<Movie> movies = store.findAll();
        String json = moviesToJson(movies);
        sendJson(ex, 200, json);
    }

    //GET/movies?year=YYYY
    private void handleGetMoviesByYear(HttpExchange ex, String query) throws IOException {
        Map<String, String> params = parseQueryParams(query);
        String yearStr = params.get("year");

        if (yearStr == null || yearStr.isEmpty()) {
            sendError(ex, 400, "Некорректный параметр запроса - 'year'");
            return;
        }

        try {
            int year = Integer.parseInt(yearStr);
            List<Movie> movies = store.findByYear(year);
            String json = moviesToJson(movies);
            sendJson(ex, 200, json);
        } catch (NumberFormatException e) {
            sendError(ex, 400, "Некорректный параметр запроса - 'year'");
        }
    }

    //POST/movies
    private void handlePostMovie(HttpExchange ex) throws IOException {

        String contentType = ex.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase().startsWith("application/json")) {
            sendError(ex, 415, "Unsupported Media Type");
            return;
        }

        try {
            String body = getBody(ex);
            Movie movie = parseJson(body);

            validateMovie(movie);

            Movie created = store.save(movie);
            sendJson(ex, 201, created.toJson());

        } catch (IllegalArgumentException e) {
            sendValidationError(ex, e.getMessage());
        } catch (Exception e) {
            sendError(ex, 422, "Ошибка валидации");
        }
    }

    //GET/movies/{id}
    private void handleGetMovieById(HttpExchange ex) throws IOException {
        String idStr = extractIdFromPath(ex.getRequestURI().getPath());

        if (idStr == null) {
            sendError(ex, 400, "Некорректный ID");
            return;
        }

        try {
            int id = Integer.parseInt(idStr);
            Movie movie = store.findById(id);

            if (movie == null) {
                sendError(ex, 404, "Фильм не найден");
            } else {
                sendJson(ex, 200, movie.toJson());
            }
        } catch (NumberFormatException e) {
            sendError(ex, 400, "Некорректный ID");
        }
    }

    //DELETE /movies/{id}
    private void handleDeleteMovie(HttpExchange ex) throws IOException {
        String idStr = extractIdFromPath(ex.getRequestURI().getPath());

        if (idStr == null) {
            sendError(ex, 400, "Некорректный ID");
            return;
        }

        try {
            int id = Integer.parseInt(idStr);
            boolean deleted = store.deleteById(id);

            if (deleted) {
                sendNoContent(ex);
            } else {
                sendError(ex, 404, "Фильм не найден");
            }
        } catch (NumberFormatException e) {
            sendError(ex, 400, "Некорректный ID");
        }
    }

    private void validateMovie(Movie movie) {
        String title = movie.getTitle();
        int year = movie.getYear();

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("название не должно быть пустым");
        }
        if (title.length() > 100) {
            throw new IllegalArgumentException("название не должно превышать 100 символов");
        }

        int currentYear = Year.now().getValue();
        if (year < 1888 || year > currentYear + 1) {
            throw new IllegalArgumentException("год должен быть между 1888 и " + (currentYear + 1));
        }
    }

    private String moviesToJson(List<Movie> movies) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < movies.size(); i++) {
            sb.append(movies.get(i).toJson());
            if (i < movies.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private Movie parseJson(String json) {
        try {
            Movie movie = new Movie();
            String content = json.replace("{", "").replace("}", "").trim();
            if (content.isEmpty()) return movie;

            String[] parts = content.split(",");
            for (String part : parts) {
                String[] kv = part.split(":");
                if (kv.length == 2) {
                    String key = kv[0].trim().replace("\"", "");
                    String value = kv[1].trim().replace("\"", "");
                    if (key.equals("title")) {
                        movie.setTitle(value);
                    } else if (key.equals("year")) {
                        movie.setYear(Integer.parseInt(value));
                    }
                }
            }
            return movie;
        } catch (Exception e) {
            throw new RuntimeException("Некорректный JSON");
        }
    }
}
