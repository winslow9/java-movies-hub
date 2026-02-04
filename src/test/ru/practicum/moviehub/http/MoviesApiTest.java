package ru.practicum.moviehub.http;

import com.google.gson.*;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.store.MoviesStore;

import javax.swing.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {

    HttpClient client = HttpClient.newHttpClient();
    private static final String BASE = "http://localhost:8080";
    MoviesStore moviesStore = new MoviesStore();
    final MoviesServer server = new MoviesServer(moviesStore, 8080);
    Gson gson = new GsonBuilder().setPrettyPrinting().create();


    @BeforeEach
    void beforeEach() throws IOException {
        server.start();
    }

    @AfterEach
    void afterEach() {
        server.stop();
    }

    // Тесты на GET
    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).GET().build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals("[]", resp.body(), "GET /movies должен вернуть []");
    }

    @Test
    void getMovies_returnsInitializedSampleData() throws Exception {
        moviesStore.initializeSampleData();
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).GET().build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String result = gson.toJson(moviesStore.getAllMovies());

        assertEquals(result, resp.body(), "GET /movies должен вернуть все фильмы");
    }

    @Test
    void getMovie_byId_true() throws Exception {
        // Подготовка
        moviesStore.initializeSampleData();
        int expectedMovieId = 2;
        String expectedTitle = moviesStore.getMovie(expectedMovieId).getTitle(); // "The Matrix"

        // Выполнение запроса
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/" + expectedMovieId)).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();

        String actualTitle = jsonResponse.get("title").getAsString();
        assertEquals(expectedTitle, actualTitle, "Название фильма должно совпадать");
    }

    @Test
    void getError_byNotExistedId() throws Exception {
        Integer id = 4;
        moviesStore.initializeSampleData();
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/" + id)).GET().build();

        Integer result = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).statusCode();


        assertEquals(404, result);
    }

    @Test
    void getError_idIsNotInteger() throws Exception {
        String id = "Z";
        moviesStore.initializeSampleData();
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/" + id)).GET().build();

        Integer result = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).statusCode();


        assertEquals(400, result);
    }

    @Test
    void getMovies_byYear() throws Exception {
        Integer year = 1999;
        moviesStore.initializeSampleData();
        String result = gson.toJson(moviesStore.getMoviesByYear(year));

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies?year=" + year)).GET().build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String pretty = gson.toJson(JsonParser.parseString(resp.body()).getAsJsonArray());

        assertEquals(pretty, result, "GET по 1999 году должен вернуть The Matrix");
    }

    @Test
    void getEmpty_byYear() throws Exception {
        Integer year = 1987;
        moviesStore.initializeSampleData();
        String result = gson.toJson(moviesStore.getMoviesByYear(year));

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies?year=" + year)).GET().build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String pretty = gson.toJson(JsonParser.parseString(resp.body()).getAsJsonArray());

        assertEquals(pretty, result, "GET по 1987 вернет []");
    }

    @Test
    void getError_byYear() throws Exception {
        String year = "aba";
        moviesStore.initializeSampleData();

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies?year=" + year)).GET().build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, resp.statusCode(), "GET с нечисловым годом должен вернуть 400");
    }

    //Тесты на Delete
    @Test
    void delete_byId() throws Exception {
        Integer id = 1;
        moviesStore.initializeSampleData();

        // Получаем id = 1
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/" + id)).GET().build();
        HttpResponse<String> prevResp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String pretty = gson.toJson(JsonParser.parseString(prevResp.body()).getAsJsonObject());
        assertEquals(pretty, prevResp.body());

        // Удаляем id = 1
        HttpRequest reqDelete = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/" + id)).DELETE().build();
        HttpResponse<String> resDelete = client.send(reqDelete, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(null, moviesStore.getMovie(id));
    }

    @Test
    void movieDoesntFound_byId() throws Exception {
        Integer id = 4;
        moviesStore.initializeSampleData();

        // Получаем id = 4
        HttpRequest getAfterRequest = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/" + id)).GET().build();

        HttpResponse<String> getAfterResponse = client.send(getAfterRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertTrue(getAfterResponse.body().contains("Фильм не найден"));
    }

    @Test
    void idIsNotInt() throws Exception {
        String id = "as";
        moviesStore.initializeSampleData();

        // Получаем id = as
        HttpRequest getAfterRequest = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/" + id)).GET().build();

        HttpResponse<String> getAfterResponse = client.send(getAfterRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));


        assertTrue(getAfterResponse.body().contains("Невалидный формат ID"));
    }

    // Тесты на POST
    @Test
    void successCreateMovie() throws Exception {
        String newMovie = "{\n" + "\t\"title\": \"DMB\",\n" + "\t\"year\": 1999\n" + "}";

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(newMovie)).build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonObject respBody = JsonParser.parseString(resp.body()).getAsJsonObject();


        assertEquals("DMB", respBody.get("title").getAsString());
        assertEquals(1999, respBody.get("year").getAsInt());
        assertTrue(respBody.has("id"), "Ответ должен содержать id");
    }

    @Test
    void emptyTitleError() throws Exception {
        String newMovie = "{\n" + "\t\"title\": \"\",\n" + "\t\"year\": 1999\n" + "}";

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(newMovie)).build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Integer code = resp.statusCode();
        assertEquals(400, code);
    }

    @Test
    void largeTitleError() throws Exception {
        String newMovie = "{\n" + "\t\"title\": \"ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff\",\n" + "\t\"year\": 1999\n" + "}";

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(newMovie)).build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Integer code = resp.statusCode();
        assertEquals(400, code);
    }

    @Test
    void badContentTypeError() throws Exception {
        String newMovie = "{\n" + "\t\"title\": \"DMB\",\n" + "\t\"year\": 2999\n" + "}";

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(newMovie)).build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Integer code = resp.statusCode();
        assertTrue(resp.body().contains("Ошибка валидации"));
    }

    @Test
    void badYearError() throws Exception {
        String newMovie = "{\n" + "\t\"title\": \"DMB\",\n" + "\t\"year\": 2999\n" + "}";

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).header("Content-Type", "aaaaaaaaaaapplication/json").POST(HttpRequest.BodyPublishers.ofString(newMovie)).build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Integer code = resp.statusCode();
        assertTrue(resp.body().contains("Ошибка валидации"));
    }

    @Test
    void brokenJson() throws Exception {
        String newMovie = "{\n" + "\t\"title\": \"DMB\",\n" + "\t\"year\": 2999\n";

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(newMovie)).build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("Невалидный JSON"));
    }
}