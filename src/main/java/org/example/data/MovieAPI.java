package org.example.data;

// Consumimos una Api Rest de peliculas para practicar
import org.example.data.Movie; // cambiar con la ruta correcta
import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;

public class MovieAPI {

    private static final String URL =
            "https://devsapihub.com/api-movies";

    public static List<Movie> obtenerPeliculas()
            throws IOException, InterruptedException {

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request,
                        HttpResponse.BodyHandlers.ofString());

        Gson gson = new Gson();

        return Arrays.asList(gson.fromJson(response.body(), Movie[].class));
    }
}