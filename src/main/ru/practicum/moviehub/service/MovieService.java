package ru.practicum.moviehub.service;

import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.repository.MovieRepository;
import ru.practicum.moviehub.store.MoviesStore;

import java.util.List;

public class MovieService {
    private final MovieRepository repository;
    private final MoviesStore store;

    public MovieService(MoviesStore store) {
        this.store = store;
        this.repository = new MovieRepository();
    }

    public MoviesStore getStore() {
        return store;
    }

    public List<Movie> getAllMovies() {
        return repository.findAll();
    }

    public Movie addMovie(String title, int year) {
        // Валидация теперь в MovieServer, но можно оставить и здесь
        Movie movie = new Movie(0, title, year);
        return repository.save(movie);
    }

    public Movie getMovieById(int id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Фильм не найден"));
    }

    public boolean deleteMovie(int id) {
        return repository.deleteById(id);
    }

    public List<Movie> getMoviesByYear(int year) {
        return repository.findByYear(year);
    }

    public MovieRepository getRepository() {
        return repository;
    }
}