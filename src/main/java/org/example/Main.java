package org.example;

import org.example.data.MovieCsv;
import org.example.data.MovieManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
//        MovieManager.catalogo.forEach(System.out::println);

        MovieManager mm = new MovieManager();

        System.out.println(mm.catalogo.size());

        System.out.println("Ejercicio 1:  Las 5 películas mejor valoradas.");
        mm.showTop5Movies();
        System.out.println("================================================\n");
        System.out.println("Ejercicio 1.1: Obtener películas por año");
        System.out.println(Arrays.toString(mm.getImages(2026)));
        System.out.println("================================================\n");
        System.out.println("Ejercicio 1.2: Buscar película por ID");
        mm.printMovies(mm.movieForId(5));
        System.out.println("================================================\n");
        System.out.println("Ejercicio 1.3: Saltar y obtener el título más largo");
        System.out.println(mm.skipLongTitle(15));
        System.out.println("================================================\n");
        System.out.println("Ejercicio 1.4: Limitamos y comprobamos nota");
        System.out.println(mm.limitAndEvaluate(10,4.2));
        System.out.println("================================================\n");
        System.out.println("Ejercicio 2: Número de películas por género");
        System.out.println(mm.movieForGenre());
        System.out.println("================================================\n");
        System.out.println("Ejercicio 3. Nota media por género");
        System.out.println(mm.rankingForGenre());
        System.out.println("================================================\n");
        System.out.println("Ejercicio 4. Año con mayor número de películas");
        mm.getYearMostMovie();
        System.out.println("================================================\n");
        System.out.println("Ejercicio 5. Dividir las películas en base a su nota");
        System.out.println(mm.reviewMovies(3));
        System.out.println("================================================\n");
    }
}
