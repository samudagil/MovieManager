package org.example.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Adaptador IMDb -> CSV de 300 películas -> List<Movie>.
 * Solo JDK 21 (sin dependencias). Los valores IMDb son históricos.
 *
 * Archivo de origen:
 * https://github.com/shahbaj-cse/movie-data-etl-pipeline/blob/main/movie_data/imdb_movies_rating.csv
 */
public final class MovieCsv {
    private MovieCsv() {}

    public static final URI IMDb_CSV = URI.create(
            "https://raw.githubusercontent.com/shahbaj-cse/movie-data-etl-pipeline/main/movie_data/imdb_movies_rating.csv");

    private static final String[] HEADER = {
            "id", "title", "description", "year", "image_url", "genre", "stars"
    };

    // Datos intermedios mediante record de Java 21.
    private record FilmData(String title, String description, int year,
                            String imageUrl, String[] genres, double stars) {}

    /**
     * Paso 1: descarga la fuente pública, selecciona 300 películas y crea el CSV.
     * Se asignan identificadores locales consecutivos del 1 al 300.
     */
    public static void generarCsv(Path destino) throws IOException, InterruptedException {
        HttpClient cliente = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest peticion = HttpRequest.newBuilder(IMDb_CSV)
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", "Java21-Movie-Csv-Exercise")
                .GET()
                .build();

        HttpResponse<InputStream> respuesta = cliente.send(
                peticion, HttpResponse.BodyHandlers.ofInputStream());

        if (respuesta.statusCode() != 200) {
            try (InputStream descartado = respuesta.body()) { /* cerrar */ }
            throw new IOException("Error HTTP al obtener IMDb: " + respuesta.statusCode());
        }

        try (Reader origen = new InputStreamReader(respuesta.body(), StandardCharsets.UTF_8)) {
            generarCsv(origen, destino, 300);
        }
    }

    /** Variante reutilizable para leer un CSV de IMDb ya descargado. */
    public static void generarCsvDesdeArchivo(Path origen, Path destino) throws IOException {
        try (Reader lector = Files.newBufferedReader(origen, StandardCharsets.UTF_8)) {
            generarCsv(lector, destino, 300);
        }
    }

    private static void generarCsv(Reader lector, Path destino, int cantidad) throws IOException {
        AtomicInteger secuencia = new AtomicInteger(1);
        List<Movie> peliculas;

        try (Stream<List<String>> registros = leerRegistros(lector)) {
            peliculas = registros
                    .skip(1)                          // Cabecera IMDb
                    .map(MovieCsv::convertirIMDb)       // Optional<FilmData>
                    .flatMap(Optional::stream)         // Descarta filas incompletas
                    .limit(cantidad)                  // Exactamente 300 válidas, si existen
                    .map(d -> new Movie(
                            secuencia.getAndIncrement(), d.title(), d.description(),
                            d.year(), d.imageUrl(), d.genres(), d.stars()))
                    .toList();                        // Stream.toList(), JDK 16+
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }

        if (peliculas.size() != cantidad) {
            throw new IOException("Solo se encontraron " + peliculas.size()
                    + " películas válidas; se necesitaban " + cantidad);
        }

        // NIO.2 con UTF-8, escritura de un registro por línea y cierre automático.
        try (var out = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {
            out.write(String.join(",", HEADER));
            out.newLine();

            for (Movie p : peliculas) {
                String[] columnas = {
                        Integer.toString(p.getId()),
                        p.getTitle(),
                        p.getDescription(),
                        Integer.toString(p.getYear()),
                        p.getImage_url(),
                        String.join("|", p.getGenre()),
                        Double.toString(p.getStars())
                };
                out.write(Arrays.stream(columnas)
                        .map(MovieCsv::escaparCampoCsv)
                        .reduce((a, b) -> a + "," + b).orElse(""));
                out.newLine();
            }
        }
    }

    /** Paso 2: carga todas las filas como List<Movie> (lista inmodificable). */
    public static List<Movie> cargar(Path csv) throws IOException {
        try (Reader reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8);
             Stream<List<String>> registros = leerRegistros(reader)) {

            AtomicInteger linea = new AtomicInteger(1);
            return registros
                    .skip(1)  // Cabecera id,title,description,year,image_url,genre,stars
                    .filter(c -> !(c.size() == 1 && c.getFirst().isBlank()))
                    .map(c -> convertirMovie(c, linea.incrementAndGet()))
                    .toList();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private static Movie convertirMovie(List<String> c, int fila) {
        if (c.size() != 7) {
            throw new IllegalArgumentException(
                    "Fila " + fila + ": esperaba 7 columnas, hay " + c.size());
        }
        try {
            String[] generos = Arrays.stream(c.get(5).split("\\|", -1))
                    .map(String::trim).filter(s -> !s.isBlank()).toArray(String[]::new);
            return new Movie(
                    Integer.parseInt(c.get(0).trim()),
                    c.get(1), c.get(2),
                    Integer.parseInt(c.get(3).trim()),
                    c.get(4), generos,
                    Double.parseDouble(c.get(6).trim()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Fila " + fila + " incorrecta: " + e.getMessage(), e);
        }
    }

    // Columnas de la fuente IMDb:
    // [0] Poster_Link, [1] Series_Title, [2] Released_Year,
    // [5] Genre, [6] IMDB_Rating, [7] Overview.
    private static Optional<FilmData> convertirIMDb(List<String> c) {
        if (c.size() < 8) return Optional.empty();
        try {
            String image = c.get(0).trim();
            String title = c.get(1).trim();
            String overview = c.get(7).replaceAll("\\s+", " ").trim();
            int year = Integer.parseInt(c.get(2).trim());
            double imdb = Double.parseDouble(c.get(6).trim());
            String[] genres = Arrays.stream(c.get(5).split(","))
                    .map(String::trim).filter(s -> !s.isBlank()).toArray(String[]::new);

            if (title.isEmpty() || overview.isEmpty() || !image.startsWith("https://")
                    || genres.length == 0 || year < 1888 || imdb < 0 || imdb > 10) {
                return Optional.empty();
            }

            // IMDb 0..10 -> stars 0..5, redondeadas a una décima.
            double stars = BigDecimal.valueOf(imdb)
                    .divide(BigDecimal.valueOf(2), 1, RoundingMode.HALF_UP)
                    .doubleValue();
            return Optional.of(new FilmData(title, overview, year, image, genres, stars));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    /** Entrecomilla un campo si contiene caracteres relevantes para CSV. */
    private static String escaparCampoCsv(String campo) {
        if (campo.contains(",") || campo.contains("\"") || campo.contains("\n")
                || campo.contains("\r")) {
            return "\"" + campo.replace("\"", "\"\"") + "\"";
        }
        return campo;
    }

    /** Stream perezoso de registros CSV (soporta comas, comillas y saltos entrecomillados). */
    private static Stream<List<String>> leerRegistros(Reader r) {
        CsvIterator iterator = new CsvIterator(r);
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, 0), false);
    }

    private static final class CsvIterator implements Iterator<List<String>> {
        private final PushbackReader in;
        private List<String> siguiente;

        CsvIterator(Reader reader) {
            in = new PushbackReader(new BufferedReader(reader), 2);
            avanzar();
        }

        private void avanzar() {
            try { siguiente = leerUno(); }
            catch (IOException e) { throw new UncheckedIOException(e); }
        }

        @Override public boolean hasNext() { return siguiente != null; }

        @Override public List<String> next() {
            if (siguiente == null) throw new NoSuchElementException();
            List<String> actual = siguiente;
            avanzar();
            return actual;
        }

        private List<String> leerUno() throws IOException {
            List<String> columnas = new ArrayList<>();
            StringBuilder campo = new StringBuilder();
            boolean entreComillas = false;
            boolean inicioCampo = true;
            boolean leidoAlgo = false;

            int car;
            while ((car = in.read()) != -1) {
                leidoAlgo = true;
                if (car == '\"') {
                    if (entreComillas) {
                        int siguiente = in.read();
                        if (siguiente == '\"') {
                            campo.append('\"');
                        } else {
                            entreComillas = false;
                            if (siguiente != -1) in.unread(siguiente);
                        }
                    } else if (inicioCampo) {
                        entreComillas = true;
                        inicioCampo = false;
                    } else {
                        throw new IOException("Comilla inesperada en un campo CSV");
                    }
                } else if (car == ',' && !entreComillas) {
                    columnas.add(campo.toString());
                    campo.setLength(0);
                    inicioCampo = true;
                } else if ((car == '\n' || car == '\r') && !entreComillas) {
                    if (car == '\r') {
                        int siguiente = in.read();
                        if (siguiente != '\n' && siguiente != -1) in.unread(siguiente);
                    }
                    columnas.add(campo.toString());
                    return columnas;
                } else {
                    // Conserva saltos de línea legítimos dentro de campos entrecomillados.
                    campo.append((char) car);
                    inicioCampo = false;
                }
            }

            if (!leidoAlgo) return null;
            if (entreComillas) throw new IOException("Campo CSV sin cerrar al final del archivo");
            columnas.add(campo.toString());
            return columnas;
        }
    }

    public static void main(String[] args) {
        Path destino = Path.of("movies_300.csv");
        try {
            if (args.length == 2 && "--fuente-local".equals(args[0])) {
                generarCsvDesdeArchivo(Path.of(args[1]), destino);
            } else if (args.length == 0) {
                generarCsv(destino);
            } else {
                System.out.println("Uso: java MovieCsv  o  java MovieCsv --fuente-local imdb_top_1000.csv");
                return;
            }

            List<Movie> peliculas = cargar(destino);
            System.out.println("CSV creado: " + destino.toAbsolutePath());
            System.out.println("Películas cargadas: " + peliculas.size());
            peliculas.stream().limit(5).forEach(System.out::println);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operación interrumpida: " + e.getMessage());
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}

