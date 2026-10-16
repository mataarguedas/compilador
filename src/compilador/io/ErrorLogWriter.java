package compilador.io;

import compilador.errors.LexicalError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Escribe <nombre>-errors.log junto al fuente: copia numerada idéntica más la lista ordenada de errores; persiste la Etapa 4. */
public final class ErrorLogWriter {

    /** Escribe la bitácora con el charset del fuente, reemplaza la anterior y devuelve su ruta. */
    public Path write(SourceFile source, List<LexicalError> errors) throws IOException {
        Path logPath = logPathFor(source.getPath());
        String content = render(source, errors);
        Files.write(logPath, content.getBytes(source.getCharset()));
        return logPath;
    }

    /** Calcula la ruta de la bitácora en la carpeta del fuente: codigo.php -> codigo-errors.log. */
    public static Path logPathFor(Path sourcePath) {
        String name = sourcePath.getFileName().toString();
        String baseName = name.endsWith(PhpFileLocator.PHP_EXTENSION)
                ? name.substring(0, name.length() - PhpFileLocator.PHP_EXTENSION.length())
                : name;
        return sourcePath.resolveSibling(baseName + LogFormat.LOG_FILE_SUFFIX);
    }

    /** Construye el texto completo de la bitácora: copia numerada y sección de errores. */
    static String render(SourceFile source, List<LexicalError> errors) {
        StringBuilder log = new StringBuilder();
        appendNumberedCopy(log, source);
        appendErrorSection(log, source, errors);
        return log.toString();
    }

    /** Agrega cada línea como NNNN + separador + contenido + terminador original. */
    private static void appendNumberedCopy(StringBuilder log, SourceFile source) {
        for (SourceLine line : source.getLines()) {
            log.append(LogFormat.formatLineNumber(line.getNumber()))
                    .append(LogFormat.NUMBER_SEPARATOR)
                    .append(line.getContent())
                    .append(line.getTerminator());
        }
    }

    /** Agrega una línea en blanco y luego los errores, o el mensaje de que no hay errores. */
    private static void appendErrorSection(StringBuilder log, SourceFile source,
            List<LexicalError> errors) {
        String separator = source.lineSeparatorForAppending();
        // Evita que la sección de errores quede pegada a la última línea copiada.
        if (!source.getLastLine().hasTerminator()) {
            log.append(separator);
        }
        log.append(separator);
        if (errors.isEmpty()) {
            log.append(LogFormat.NO_ERRORS_MESSAGE).append(separator);
            return;
        }
        for (LexicalError error : errors) {
            log.append(error.toLogLine()).append(separator);
        }
    }
}
