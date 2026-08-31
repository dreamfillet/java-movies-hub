package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private final HttpServer server;
    private final MoviesHandler handler;


    public MoviesServer(MoviesStore store, int port) {
        this.handler = new MoviesHandler(store);
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/movies", handler);
            server.setExecutor(null);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер на порту " + port, e);
        }
    }


    public MoviesServer(int port) {
        this(new MoviesStore(), port);
    }


    public MoviesServer() {
        this(8080);
    }


    public void start() {
        server.start();
        System.out.println("Сервер запущен на http://localhost:8080");
        System.out.println("Доступные эндпоинты:");
        System.out.println("  GET    /movies");
        System.out.println("  GET    /movies?year=YYYY");
        System.out.println("  POST   /movies");
        System.out.println("  GET    /movies/{id}");
        System.out.println("  DELETE /movies/{id}");
    }

    public void stop() {
        server.stop(0);
        System.out.println("Сервер остановлен");
    }

    public MoviesHandler getHandler() {
        return handler;
    }

    public MoviesStore getStore() {
        return getStore();
    }
}