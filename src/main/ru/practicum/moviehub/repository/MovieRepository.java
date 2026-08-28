package ru.practicum.moviehub.repository;

import ru.practicum.moviehub.model.Movie;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class MovieRepository {
    private final Map<Integer, Movie> movies = new HashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    public List<Movie> findAll() {
        return new ArrayList<>(movies.values());
    }

    public Optional<Movie> findById(int id) {
        return Optional.ofNullable(movies.get(id));
    }

    public Movie save(Movie movie) {
        int id = idGenerator.getAndIncrement();
        movie.setId(id);
        movies.put(id, movie);
        return movie;
    }

    public boolean deleteById(int id) {
        return movies.remove(id) != null;
    }

    public List<Movie> findByYear(int year) {
        return movies.values().stream()
                .filter(movie -> movie.getYear() == year)
                .toList();
    }

    public void clear() {
        movies.clear();
        idGenerator.set(1);
    }
}