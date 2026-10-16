package compilador.io;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Busca los archivos .php a analizar sin nombre ni ruta fija, junto al jar o en user.dir (PRD §3.1); paso previo a la Etapa 1. */
public final class PhpFileLocator {

    /** Única extensión aceptada; la comparación distingue mayúsculas y minúsculas. */
    public static final String PHP_EXTENSION = ".php";

    // Extensión del jar, propiedad del directorio de trabajo y punto que separa la extensión.
    private static final String JAR_EXTENSION = ".jar";
    private static final String USER_DIR_PROPERTY = "user.dir";
    private static final char EXTENSION_DOT = '.';

    /** Resultado posible de una búsqueda. */
    public enum Status {
        /** Se encontró al menos un archivo para analizar. */
        FOUND,
        /** No existe ningún archivo .php en los directorios revisados. */
        NO_PHP_FOUND,
        /** El argumento de consola no termina exactamente en .php. */
        WRONG_EXTENSION,
        /** El argumento de consola nombra un archivo que no existe. */
        FILE_NOT_FOUND
    }

    /** Busca los archivos a analizar: el indicado en la consola o todos los .php del directorio base. */
    public SearchResult locate(String fileArgument) throws IOException {
        List<Path> directories = searchDirectories();
        if (fileArgument != null) {
            return locateArgument(fileArgument, directories);
        }
        List<IgnoredFile> ignored = new ArrayList<IgnoredFile>();
        // Se detiene en el primer directorio que contenga archivos .php.
        for (Path directory : directories) {
            List<Path> files = listPhpFiles(directory, ignored);
            if (!files.isEmpty()) {
                return new SearchResult(Status.FOUND, files, ignored, directories, null);
            }
        }
        return new SearchResult(Status.NO_PHP_FOUND, Collections.<Path>emptyList(), ignored,
                directories, null);
    }

    /** Devuelve primero el directorio base y luego user.dir si es distinto. */
    private static List<Path> searchDirectories() {
        List<Path> directories = new ArrayList<Path>();
        Path userDir = Paths.get(System.getProperty(USER_DIR_PROPERTY)).toAbsolutePath()
                .normalize();
        Path baseDir = resolveBaseDirectory(userDir);
        directories.add(baseDir);
        if (!baseDir.equals(userDir)) {
            directories.add(userDir);
        }
        return directories;
    }

    /** Obtiene la carpeta del jar con toURI() (soporta espacios y tildes); usa userDir si se ejecuta desde un directorio de clases. */
    private static Path resolveBaseDirectory(Path userDir) {
        try {
            CodeSource codeSource = PhpFileLocator.class.getProtectionDomain().getCodeSource();
            URL location = codeSource == null ? null : codeSource.getLocation();
            if (location == null) {
                return userDir;
            }
            Path codePath = Paths.get(location.toURI()).toAbsolutePath().normalize();
            if (Files.isDirectory(codePath) || codePath.getParent() == null) {
                return userDir;
            }
            return codePath.getParent();
        } catch (URISyntaxException | SecurityException | IllegalArgumentException e) {
            return userDir;
        }
    }

    /** Valida la extensión del argumento de consola y verifica que el archivo exista. */
    private static SearchResult locateArgument(String argument, List<Path> directories) {
        List<IgnoredFile> none = Collections.emptyList();
        if (!argument.endsWith(PHP_EXTENSION)) {
            return new SearchResult(Status.WRONG_EXTENSION, Collections.<Path>emptyList(),
                    none, directories, argument);
        }
        Path file = resolveArgument(argument, directories);
        if (file == null) {
            return new SearchResult(Status.FILE_NOT_FOUND, Collections.<Path>emptyList(),
                    none, directories, argument);
        }
        return new SearchResult(Status.FOUND, Collections.singletonList(file), none,
                directories, argument);
    }

    /** Devuelve el archivo existente que nombra el argumento o null; una ruta relativa se prueba en cada directorio en orden. */
    private static Path resolveArgument(String argument, List<Path> directories) {
        try {
            Path given = Paths.get(argument);
            if (given.isAbsolute()) {
                return Files.isRegularFile(given) ? given.normalize() : null;
            }
            for (Path directory : directories) {
                Path candidate = directory.resolve(given).normalize();
                if (Files.isRegularFile(candidate)) {
                    return candidate;
                }
            }
            return null;
        } catch (InvalidPathException e) {
            return null;
        }
    }

    /** Lista en orden alfabético los archivos aceptados de un directorio y agrega los rechazados a ignored. */
    private static List<Path> listPhpFiles(Path directory, List<IgnoredFile> ignored)
            throws IOException {
        List<Path> accepted = new ArrayList<Path>();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) {
                // Solo se consideran archivos regulares (no carpetas).
                if (!Files.isRegularFile(entry)) {
                    continue;
                }
                String reason = rejectionReason(entry.getFileName().toString());
                if (reason == null) {
                    accepted.add(entry);
                } else if (isCandidate(entry.getFileName().toString())) {
                    ignored.add(new IgnoredFile(entry, reason));
                }
            }
        }
        Collections.sort(accepted);
        Collections.sort(ignored);
        return accepted;
    }

    /** Devuelve false para el propio jar y las bitácoras, que nunca son entradas. */
    private static boolean isCandidate(String name) {
        return !name.endsWith(JAR_EXTENSION) && !name.endsWith(LogFormat.LOG_FILE_SUFFIX);
    }

    /** Devuelve null si el nombre se acepta o, si no, el motivo del rechazo. */
    static String rejectionReason(String name) {
        if (name.endsWith(PHP_EXTENSION)) {
            return name.length() > PHP_EXTENSION.length()
                    ? null : "the name has nothing before '" + PHP_EXTENSION + "'";
        }
        int dot = name.lastIndexOf(EXTENSION_DOT);
        if (dot < 0) {
            return "it has no extension; '" + PHP_EXTENSION + "' is required";
        }
        String extension = name.substring(dot);
        // Caso x.PHP: la extensión coincide solo si se ignoran las mayúsculas.
        if (extension.toLowerCase(Locale.ROOT).equals(PHP_EXTENSION)) {
            return "extension '" + extension + "' is not exactly '" + PHP_EXTENSION
                    + "'; the check is case-sensitive";
        }
        // Caso x.php.txt: .php no es la extensión final.
        if (name.contains(PHP_EXTENSION + EXTENSION_DOT)) {
            return "'" + PHP_EXTENSION + "' is not the final extension; the name ends in '"
                    + extension + "'";
        }
        return "extension '" + extension + "' is not '" + PHP_EXTENSION + "'";
    }

    /** Archivo visto pero no aceptado, junto con el motivo del rechazo. */
    public static final class IgnoredFile implements Comparable<IgnoredFile> {

        // Ruta del archivo y motivo por el que se ignoró.
        private final Path path;
        private final String reason;

        // Crea el registro de archivo ignorado.
        IgnoredFile(Path path, String reason) {
            this.path = path;
            this.reason = reason;
        }

        /** Devuelve la ruta del archivo ignorado. */
        public Path getPath() {
            return path;
        }

        /** Devuelve el motivo por el que se ignoró. */
        public String getReason() {
            return reason;
        }

        /** Ordena los archivos ignorados por ruta. */
        @Override
        public int compareTo(IgnoredFile other) {
            return path.compareTo(other.path);
        }
    }

    /** Resultado inmutable de locate(): estado, archivos, ignorados, directorios revisados y argumento. */
    public static final class SearchResult {

        // Datos de la búsqueda, guardados como listas no modificables.
        private final Status status;
        private final List<Path> files;
        private final List<IgnoredFile> ignored;
        private final List<Path> searchedDirectories;
        private final String argument;

        // Crea el resultado copiando las listas recibidas.
        SearchResult(Status status, List<Path> files, List<IgnoredFile> ignored,
                List<Path> searchedDirectories, String argument) {
            this.status = status;
            this.files = Collections.unmodifiableList(new ArrayList<Path>(files));
            this.ignored = Collections.unmodifiableList(new ArrayList<IgnoredFile>(ignored));
            this.searchedDirectories = Collections.unmodifiableList(
                    new ArrayList<Path>(searchedDirectories));
            this.argument = argument;
        }

        /** Devuelve el estado de la búsqueda. */
        public Status getStatus() {
            return status;
        }

        /** Devuelve los archivos a analizar en orden alfabético. */
        public List<Path> getFiles() {
            return files;
        }

        /** Devuelve los archivos vistos pero rechazados. */
        public List<IgnoredFile> getIgnored() {
            return ignored;
        }

        /** Devuelve los directorios revisados, primero el directorio base. */
        public List<Path> getSearchedDirectories() {
            return searchedDirectories;
        }

        /** Devuelve el archivo indicado en la consola, o null. */
        public String getArgument() {
            return argument;
        }
    }
}
