package ru.practicum.moviehub;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.http.BaseHttpHandler;
import ru.practicum.moviehub.http.MoviesServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;

public class MovieHubApp {
    public static void main(String[] args) throws IOException {
        final MoviesServer server = new MoviesServer(new MoviesStore(), 8080);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();

    }
}