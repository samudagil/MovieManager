package org.example.data;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MovieManager {
    public static List<Movie> catalogo;
    static {
        try {
            catalogo = MovieAPI.obtenerPeliculas();
        } catch (IOException | InterruptedException e){
            System.err.println("Error en la API" + e);
        }
    }
    public List<Movie> getTopMovies (int limit){
        return catalogo.stream()
                .sorted(Comparator.comparing((Movie m) -> m.getStars()).reversed())
                .limit(limit)
                //.collect(Collectors.toList())
                .toList();
    }
    public void showTop5Movies (){
        getTopMovies(5)
                .forEach(m -> System.out.println(m.getTitle() + "--" + m.getStars()));
    }
}
