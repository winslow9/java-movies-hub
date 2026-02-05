package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private MoviesStore moviesStore;
    private Integer port;
    private HttpServer server;

    public MoviesServer(MoviesStore moviesStore, Integer port) {
        this.moviesStore = moviesStore;
        this.port = port;

    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/movies", new MoviesHandler(moviesStore));
        server.start();

        System.out.println("Сервак стартанул");
    }


    public void stop() {
        server.stop(0);
        System.out.println("Сервак отключен");
    }

}