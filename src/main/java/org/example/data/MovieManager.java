package org.example.data;

import org.example.data.error.TresholdExcepcion;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MovieManager {

    public static List<Movie> catalogo;

    static {
        try {
//            catalogo =  MovieAPI.obtenerPeliculas();
//            Set<Movie> catalogoSinDuplicados = new HashSet<>(catalogo);
//            catalogoSinDuplicados.addAll(MovieCsv.cargar(Path.of("movies_300.csv")));
//            catalogo = new ArrayList<>(catalogoSinDuplicados);

            catalogo = new ArrayList<>(MovieCsv.cargar(Path.of("movies_300.csv")));
            MovieAPI.obtenerPeliculas().stream()
                    .filter(m -> !catalogo.contains(m))
                    .forEach(m -> catalogo.add(m));

        } catch (IOException | InterruptedException e) {
            System.err.println("Fallo en la API " + e);
        }
    }

    public List<Movie> getTopMovies(int limit){

        return catalogo.stream()
                .sorted(Comparator.comparing((Movie m) -> m.getStars()).reversed())
                .limit(limit)
                .toList()
                //.collect(Collectors.toList())
                ;
    }

    public void showTop5Movies(){
        getTopMovies(5)
                .forEach(m -> System.out.println(m.getTitle() + "---" + m.getStars()));
    }

    public List<Movie> moviesForYear(int year){

        return catalogo.stream()
                .filter(m -> m.getYear() == year)
                .collect(Collectors.toList());
    }

    public String[] getImages(List<Movie> movies){

        return movies.stream()
                .map(m -> m.getImage_url())
                //.toArray()    Array de Object
                .toArray(String[]::new);
    }

    public String[] getImages(int year){

        return getImages(moviesForYear(year));
    }

    public List<Movie> movieForId(int id){

        return catalogo.stream()
                .filter(m -> m.getId() == id)
                .toList();
    }

    public void printMovies(List<Movie> movies){
        movies.forEach(System.out::println);
    }

    public int skipLongTitle(int salto){
        if (salto >= TresholdExcepcion.MAXIMUMSIZE || salto < TresholdExcepcion.MINIMUMSIZE){
            throw new TresholdExcepcion();
        }

        return catalogo.stream()
                .skip(salto)
                .mapToInt(m -> m.getTitle().trim().length())
                .max()
                .orElse(0);
    }

    public boolean limitAndEvaluate(int limite, double stars){
        if (limite >= TresholdExcepcion.MAXIMUMSIZE || limite < TresholdExcepcion.MINIMUMSIZE){
            throw new TresholdExcepcion();
        }

        return catalogo.stream()
                .limit(limite)
                .allMatch(m -> m.getStars() >= stars);
    }

    public Map<String, Long> movieForGenre(){

        return catalogo.stream()
                .flatMap(m -> Arrays.stream(m.getGenre()))
                .collect(Collectors
                .groupingBy(Function.identity(),Collectors.counting()));
    }

    public Map<String, Double> rankingForGenre(){

        return catalogo.stream()
                    .flatMap(m -> Arrays.stream(m.getGenre())
                                .map(g -> Map
                                        .entry(g, m.getStars()))
                    )
                    .collect(Collectors.groupingBy(
                            Map.Entry::getKey,
                            Collectors.averagingDouble(entry -> entry.getValue())
                    ));
    }

    public void showYearMostMovie(){
        Map.Entry<Integer,Long> obj = getYearMostMovie();
        System.out.println(obj.getKey() + " : " + obj.getValue());
    }

    public Map.Entry<Integer,Long> getYearMostMovie(){

        return catalogo.stream()
                .collect(Collectors.groupingBy(Movie::getYear,
                        Collectors.counting()))
                .entrySet()
                .stream()
//              .max((x,y) -> (int) (x.getValue() - y.getValue()))
                .max(Map.Entry.comparingByValue())
                .get();
    }

    public Map<Boolean,List<Movie>> reviewMovies(double nota){

        return catalogo.stream()
                .collect(Collectors
                        .partitioningBy(m -> m.getStars() >= nota));
    }
}
