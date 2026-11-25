package org.example.data;

// Este es el modelo que vamos a usar
import java.util.Arrays;
import java.util.Objects;

public class Movie {

    private int id;
    private String title;
    private String description;
    private int year;
    private String image_url;
    private String[] genre;
    private double stars;

    public Movie() {
    }

    public Movie(int id, String title, String description,
                 int year, String image_url, String[] genre, double stars) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.year = year;
        this.image_url = image_url;
        this.genre = genre;
        this.stars = stars;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }

    public String[] getGenre() {
        return genre;
    }

    public void setGenre(String[] genre) {
        this.genre = genre;
    }

    public double getStars() {
        return stars;
    }

    public void setStars(double stars) {
        this.stars = stars;
    }

    @Override
    public String toString() {
        return title + "\n\tSinopsis=" + description + "\n\tAño=" + year + "\n\tAño=" + image_url + "\n\tGenero=" + Arrays.toString(genre) + "\n\tCalificación=" + stars;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return year == movie.year && Objects.equals(title, movie.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, year);
    }
}
