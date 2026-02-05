package ru.practicum.moviehub.model;

public class Movie {
    private String title;

    public Integer getYear() {
        return year;
    }

    public String getTitle() {
        return title;
    }

    private Integer year;


    public Movie(String title, Integer year) {
        this.title = title;
        this.year = year;
    }
}