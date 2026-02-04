package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class BaseHttpHandler implements HttpHandler {
    private static final Pattern MOVIE_ID_PATTERN = Pattern.compile("/movies/(\\d+)");
    private Gson gson;
    private final MoviesStore moviesStore;

    public BaseHttpHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            switch (method) {
                case "GET":
                    handleGetRequest(exchange, path);
                    break;
                case "POST":
                    handlePostRequest(exchange, path);
                    break;
                case "DELETE":
                    handleDeleteRequest(exchange, path);
                    break;
                default:
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal Server Error", e.getMessage());
        }
    }

    private void handleGetRequest(HttpExchange exchange, String path) throws IOException {
        if ("/movies".equals(path)) {
            String query = exchange.getRequestURI().getQuery();

            // Если нет параметров - возвращаем все фильмы
            if (query == null || query.isEmpty()) {
                String response = gson.toJson(moviesStore.getAllMovies());
                sendResponse(exchange, 200, response);
                return;
            }

            // Парсим параметры запроса
            Map<String, String> queryParams = parseQueryParams(query);

            // Проверяем наличие параметра year
            if (queryParams.containsKey("year")) {
                String yearParam = queryParams.get("year");
                try {
                    int year = Integer.parseInt(yearParam);

                    // Валидируем год
                    if (year < 1888 || year > (Year.now().getValue() + 1)) {
                        throw new IllegalArgumentException("Год должен быть в диапазоне от 1888 до " + (Year.now().getValue() + 1));
                    }

                    // Получаем фильмы по году
                    List<Movie> moviesByYear = moviesStore.getMoviesByYear(year);

                    // Пустой массив, если фильмов нет
                    if (moviesByYear.isEmpty()) {
                        sendResponse(exchange, 200, "[]");
                    } else {
                        sendResponse(exchange, 200, gson.toJson(moviesByYear));
                    }

                } catch (NumberFormatException e) {
                    sendErrorResponse(exchange, 400, "Неверный формат года", "Год должен быть целым числом");
                } catch (IllegalArgumentException e) {
                    sendErrorResponse(exchange, 400, "Неверный формат года", e.getMessage());
                }
            } else {
                sendErrorResponse(exchange, 400, "Невалидный параметр в запросе", "после = доступен только год");
            }
        } else {
            // Получить фильм по ID
            Integer movieId = extractMovieId(path);
            if (movieId != null) {
                Movie movie = moviesStore.getMovie(movieId);
                if (movie != null) {
                    Map<String, Object> movieData = new HashMap<>();
                    movieData.put("id", movieId);
                    movieData.put("title", movie.getTitle());
                    movieData.put("year", movie.getYear());
                    String response = gson.toJson(movieData);
                    sendResponse(exchange, 200, response);
                } else {
                    sendErrorResponse(exchange, 404, "Фильм не найден", "Фильм с ID " + movieId + " не найден");
                }
            } else {
                sendErrorResponse(exchange, 400, "Невалидный формат ID", "ID должен быть целым числом");
            }
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String error, String details) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(error, details);
        String response = gson.toJson(errorResponse);
        sendResponse(exchange, statusCode, response);
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String error) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(error);
        String response = gson.toJson(errorResponse);
        sendResponse(exchange, statusCode, response);
    }

    private Integer extractMovieId(String path) {
        var matcher = MOVIE_ID_PATTERN.matcher(path);
        if (matcher.matches()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private void handlePostRequest(HttpExchange exchange, String path) throws IOException {
        // Проверяем Content-Type
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            sendErrorResponse(exchange, 415, "Некоректный Content-Type", "Content-Type должен иметь значение application/json");
            return;
        }

        // Читаем тело запроса
        String requestBody = readRequestBody(exchange);
        if (requestBody == null || requestBody.trim().isEmpty()) {
            sendErrorResponse(exchange, 400, "Пустое тело запроса", "Тело не может быть пустым");
            return;
        }

        try {
            // Парсим JSON
            Movie movie = gson.fromJson(requestBody, Movie.class);
            validateMovieData(movie);
            Integer movieId = moviesStore.addMovie(movie);

            // Возвращаем успешный ответ
            if (movieId != null) {
                Movie movieInStore = moviesStore.getMovie(movieId);
                Map<String, Object> movieData = new HashMap<>();
                movieData.put("id", movieId);
                movieData.put("title", movieInStore.getTitle());
                movieData.put("year", movieInStore.getYear());
                String response = gson.toJson(movieData);
                sendResponse(exchange, 201, response);
            } else {
                sendErrorResponse(exchange, 500, "Ошибка на стороне сервера", "Фильм не добавлен");
            }

        } catch (com.google.gson.JsonSyntaxException e) {
            sendErrorResponse(exchange, 400, "Невалидный JSON", e.getMessage());
        } catch (IllegalArgumentException e) {
            sendErrorResponse(exchange, 400, "Ошибка валидации", e.getMessage());
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Ошибка на стороне сервера", e.getMessage());
        }
    }

    private void validateMovieData(Movie movie) {
        // Проверка title
        if (movie.getTitle() == null) {
            throw new IllegalArgumentException("Необходимо название");
        }

        String title = movie.getTitle().trim();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("Название не может быть пустым");
        }

        if (title.length() > 100) {
            throw new IllegalArgumentException("Название должно быть менее 100 символов");
        }

        // Проверка year
        if (movie.getYear() == null) {
            throw new IllegalArgumentException("Необходимо указать год");
        }

        int year = movie.getYear();
        int currentYear = Year.now().getValue();
        int maxAllowedYear = currentYear + 1;

        if (year < 1888) {
            throw new IllegalArgumentException("Год должен быть больше 1888");
        }

        if (year > maxAllowedYear) {
            throw new IllegalArgumentException("Год не может быть больше чем " + maxAllowedYear);
        }
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
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

    private Map<String, String> parseQueryParams(String query) {
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

    private void handleDeleteRequest(HttpExchange exchange, String path) throws IOException {
        // Извлекаем ID из пути
        Integer movieId = extractMovieId(path);

        if (movieId == null) {
            sendErrorResponse(exchange, 400, "Невалидный формат ID", "ID должен быть целым числом");
            return;
        }

        // Пытаемся удалить фильм
        boolean deleted = moviesStore.deleteMovie(movieId);

        if (deleted) {
            // Успешно удалено - статус 204 No Content без тела ответа
            exchange.sendResponseHeaders(204, -1);
        } else {
            // Фильм не найден - статус 404 Not Found
            sendErrorResponse(exchange, 404, "Фильм не найден", "Фильм с ID " + movieId + " не найден");
        }
    }
}