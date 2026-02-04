package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MoviesStore {
    private HashMap<Integer, Movie> storeList = new HashMap<>();


    public MoviesStore() {
        this.storeList = new HashMap<>();
    }

    public Collection<Movie> getAllMovies() {
        return storeList.values();
    }

    public Movie getMovie(Integer id) {
        return storeList.get(id);
    }

    public Integer addMovie(Movie movie) {
        Integer id = storeList.size() + 1;
        storeList.put(id, movie);
        return id;
    }

    public void initializeSampleData() {
        storeList.put(1, new Movie("Inception", 2010));
        storeList.put(2, new Movie("The Matrix", 1999));
        storeList.put(3, new Movie("The Shawshank Redemption", 1994));
    }

    public List<Movie> getMoviesByYear(int year) {
        return storeList.values().stream()
                .filter(movie -> movie.getYear() == year)
                .collect(Collectors.toList());
    }

    public boolean deleteMovie(Integer id) {
        Movie removedMovie = storeList.remove(id);
        if (removedMovie != null) {
            System.out.println("Movie deleted: ID " + id + " - " + removedMovie.getTitle());
            return true;
        } else {
            System.out.println("Movie not found for deletion: ID " + id);
            return false;
        }
    }
}