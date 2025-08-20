package com.alura.literalura.Principal;
import com.alura.literalura.config.ConsumoApiGutendex;
import com.alura.literalura.config.ConvertirDatos;
import com.alura.literalura.models.Autor;
import com.alura.literalura.models.Libro;
import com.alura.literalura.models.LibrosRespuestaApi;
import com.alura.literalura.models.records.DatosLibro;
import com.alura.literalura.repository.iAutorRepository;
import com.alura.literalura.repository.iLibroRepository;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

public class Principal {

    private Scanner sc = new Scanner(System.in);
    private ConsumoApiGutendex consumoApi = new ConsumoApiGutendex();
    private ConvertirDatos convertir = new ConvertirDatos();
    private static String API_BASE = "https://gutendex.com/books/?search=";
    private List<Libro> datosLibro = new ArrayList<>();
    private iLibroRepository libroRepository;
    private iAutorRepository autorRepository;

    public Principal(iLibroRepository libroRepository, iAutorRepository autorRepository) {
        this.libroRepository = libroRepository;
        this.autorRepository = autorRepository;
    }
    public void consumo() {
        var opcion = -1;
        while (opcion != 0) {
            var menu = """
                    
                    |***************************************************|
                    |      Elija la opción a través de su número:       |
                    |***************************************************|
                    
                    1 - Buscar libro por titulo
                    2 - Listar libros registrados
                    3 - Listar autores registrados
                    4 - Listar autores vivos en un determinado año
                    5 - Listar libros por idioma
                    0 - Salir                    
                    """;
            try {
                System.out.println(menu);
                opcion = sc.nextInt();
                sc.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("|****************************************|");
                System.out.println("|  Por favor, ingrese un número válido.  |");
                System.out.println("|****************************************|\n");
                sc.nextLine();
                continue;
            }
            switch (opcion) {
                case 1:
                    buscarLibroPorTitulo();
                    break;
                case 2:
                    librosRegistrados();
                    break;
                case 3:
                    autoresRegistrados();
                    break;
                case 4:
                    autoresVivosAnio();
                    break;
                case 5:
                    buscarLibrosPorIdioma();
                    break;
                case 0:
                    opcion = 0;
                    System.out.println("|********************************|");
                    System.out.println("|    La aplicación se está cerrando|");
                    System.out.println("|********************************|\n");
                    break;
                default:
                    System.out.println("|*********************|");
                    System.out.println("|  Opción no válida. |");
                    System.out.println("|*********************|\n");
                    System.out.println("Intente nuevamente");
                    consumo();
                    break;
            }
        }
    }
    private Libro getDatosLibro() {
        System.out.println("Ingrese el nombre del libro que desea buscar: ");
        var nombreLibro = sc.nextLine().toLowerCase();
        var json = consumoApi.obtenerDatos(API_BASE + nombreLibro.replace(" ", "%20"));
        LibrosRespuestaApi datos = convertir.convertirDatosJsonAJava(json, LibrosRespuestaApi.class);

        if (datos != null && datos.getResultadoLibros() != null && !datos.getResultadoLibros().isEmpty()) {
            DatosLibro primerLibro = datos.getResultadoLibros().get(0); // Obtiene el primer libro de la lista
            return new Libro(primerLibro);
        } else {
            System.out.println("No se encontraron resultados.");
            return null;
        }
    }
    private void buscarLibroPorTitulo() {
        Libro libro = getDatosLibro();

        if (libro == null) {
            System.out.println("Libro no encontrado");
            return;
        }
        try {
            boolean libroExists = libroRepository.existsByTitulo(libro.getTitulo());
            if (libroExists) {
                System.out.println("El libro ya existe en la base de datos!");
            } else {
                libroRepository.save(libro);
                System.out.println(libro.toString());
            }
        } catch (InvalidDataAccessApiUsageException e) {
            System.out.println("No se puede persistir el libro buscado!");
        }
    }
    @Transactional(readOnly = true)
    private void librosRegistrados() {
                List<Libro> libros = libroRepository.findAll();
        if (libros.isEmpty()) {
            System.out.println("No se encontraron libros en la base de datos.");
        } else {
            System.out.println("Libros encontrados:");
            for (Libro libro : libros) {
                System.out.println(libro.toString());
            }
        }
    }
    private void autoresRegistrados() {
        //listar autores registrados
        List<Autor> autores = autorRepository.findAll();
        if (autores.isEmpty()) {
            System.out.println("No se encontraron autores en la base de datos. \n");
        } else {
            System.out.println("Autores encontrados: \n");
            Set<String> autoresUnicos = new HashSet<>();
            for (Autor autor : autores) {
                // add() retorna true si el nombre no estaba presente y se añade correctamente
                if (autoresUnicos.add(autor.getNombre())) {
                    System.out.println(autor.getNombre() + '\n');
                }
            }
        }
    }
    private void autoresVivosAnio() {
        //Buscar autores vivos por año
        System.out.println("Ingrese el año vivo de autor(es) que desea buscar: \n");
        var anioBuscado = sc.nextInt();
        sc.nextLine();
        List<Autor> autoresVivos = autorRepository.findByCumpleaniosLessThanOrFechaFallecimientoGreaterThanEqual(anioBuscado, anioBuscado);
        if (autoresVivos.isEmpty()) {
            System.out.println("No se encontraron autores que estuvieran vivos en el año " + anioBuscado + ".");
        } else {
            System.out.println("Los autores que estaban vivos en el año " + anioBuscado + " son:");
            Set<String> autoresUnicos = new HashSet<>();
            for (Autor autor : autoresVivos) {
                if (autor.getCumpleanios() != null && autor.getFechaFallecimiento() != null) {
                    if (autor.getCumpleanios() <= anioBuscado && autor.getFechaFallecimiento() >= anioBuscado) {
                        if (autoresUnicos.add(autor.getNombre())) {
                            System.out.println("Autor: " + autor.getNombre());
                        }
                    }
                }
            }
        }
    }
    private void buscarLibrosPorIdioma() {
        System.out.println("Ingrese el idioma para buscar los libros: \n");
        System.out.println("|***********************************|");
        System.out.println("|  es- español |");
        System.out.println("|  en- inglés  |");
        System.out.println("|  fr- francés  |");
        System.out.println("|  pt- portugués  |");
        System.out.println("|***********************************|\n");
        var idioma = sc.nextLine();
        List<Libro> librosPorIdioma = libroRepository.findByIdioma(idioma);

        if (librosPorIdioma.isEmpty()) {
            System.out.println("No se encontraron libros en la base de datos.");
        } else {
            System.out.println("Libros segun idioma encontrados en la base de datos:");
            for (Libro libro : librosPorIdioma) {
                System.out.println(libro.toString());
            }
        }

    }
}











