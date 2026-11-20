package org.example;

import org.example.data.MovieAPI;
import org.example.data.MovieManager;

import java.io.IOException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        //MovieManager.catalogo.forEach(System.out::println);
        MovieManager mm = new MovieManager();
        //Top 5 peliculas mejor valoradas
        mm.showTop5Movies();
    }
}