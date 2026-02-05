package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final Pattern MOVIE_ID_PATTERN = Pattern.compile("/movies/(\\d+)");
    protected Gson gson;
    protected final MoviesStore moviesStore;

    public BaseHttpHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            switch (method) {
            }
        } catch (Exception e) {
        }
    }

    void handleGetRequest(HttpExchange exchange, String path) throws IOException {
    }

    void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    void sendErrorResponse(HttpExchange exchange, int statusCode, String error, String details) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(error, details);
        String response = gson.toJson(errorResponse);
        sendResponse(exchange, statusCode, response);
    }

    void sendErrorResponse(HttpExchange exchange, int statusCode, String error) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(error);
        String response = gson.toJson(errorResponse);
        sendResponse(exchange, statusCode, response);
    }


    void handlePostRequest(HttpExchange exchange, String path) throws IOException {

    }


    String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder requestBody = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
            return requestBody.toString();
        }
    }

    Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();

        if (query == null || query.isEmpty()) {
            return params;
        }

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                try {
                    String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8.toString());
                    String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.toString());
                    params.put(key, value);
                } catch (Exception e) {
                    params.put(keyValue[0], keyValue[1]);
                }
            } else if (keyValue.length == 1) {
                params.put(keyValue[0], "");
            }
        }

        return params;
    }

    void handleDeleteRequest(HttpExchange exchange, String path) throws IOException {
    }
}