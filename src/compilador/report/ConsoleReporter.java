package compilador.report;

import compilador.io.LogFormat;
import compilador.io.PhpFileLocator;
import compilador.io.PhpFileLocator.IgnoredFile;
import compilador.io.SourceFile;
import compilador.lexer.Token;
import compilador.symbols.SymbolEntry;
import compilador.symbols.SymbolTable;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

/** Imprime la salida de consola (PRD §7): encabezado, ignorados, resumen, símbolos, tokens y mensajes de error de búsqueda. */
public final class ConsoleReporter {

    // Textos y formatos fijos de la salida de consola.
    private static final String TITLE = "PHP Lexical Analyzer";
    private static final String ERROR_PREFIX = "ERROR: ";
    private static final String FILE_LABEL = "File      : ";
    private static final String LINES_LABEL = "Lines     : ";
    private static final String IGNORED_LABEL = "Ignored   : ";
    private static final String TOKENS_LABEL = "   Tokens: ";
    private static final String ERRORS_LABEL = "   Errors: ";
    private static final String TOKEN_ROW_FORMAT = "  %-5s %-4s %-20s %-11s %s";
    private static final String LOG_LABEL = "Log       : ";
    private static final String SYMBOLS_TITLE = "Identifiers (case-sensitive symbol table):";
    private static final String NO_SYMBOLS = "  (none)";
    private static final String SYMBOL_ROW_FORMAT = "  %-{width}s first line %s   occurrences %d";
    private static final int SYMBOL_NAME_PADDING = 3;

    // Flujo de salida, normalmente System.out.
    private final PrintStream out;

    /** Crea el reportero que imprime en el flujo indicado. */
    public ConsoleReporter(PrintStream out) {
        this.out = out;
    }

    /** Imprime el título del programa. */
    public void printHeader() {
        out.println(TITLE);
    }

    /** Lista los archivos candidatos rechazados con el motivo de cada uno. */
    public void printIgnored(List<IgnoredFile> ignored) {
        for (IgnoredFile file : ignored) {
            out.println(IGNORED_LABEL + file.getPath().getFileName() + " (" + file.getReason() + ")");
        }
    }

    /** Imprime el resumen de un archivo: ruta, líneas, tokens, errores y ruta de la bitácora. */
    public void printFileSummary(SourceFile source, int tokenCount, int errorCount,
            Path logPath) {
        out.println();
        out.println(FILE_LABEL + source.getPath());
        out.println(LINES_LABEL + source.getLineCount() + TOKENS_LABEL + tokenCount
                + ERRORS_LABEL + errorCount);
        out.println(LOG_LABEL + logPath);
    }

    /** Imprime la tabla de símbolos, un identificador por fila, para mostrar que $total y $Total son distintos. */
    public void printSymbols(SymbolTable symbols) {
        out.println(SYMBOLS_TITLE);
        if (symbols.size() == 0) {
            out.println(NO_SYMBOLS);
            return;
        }
        // Calcula el ancho de la columna de nombres según el identificador más largo.
        int width = 0;
        for (SymbolEntry entry : symbols.getEntries()) {
            width = Math.max(width, entry.getName().length());
        }
        String format = SYMBOL_ROW_FORMAT.replace("{width}",
                String.valueOf(width + SYMBOL_NAME_PADDING));
        for (SymbolEntry entry : symbols.getEntries()) {
            out.println(String.format(format, entry.getName(),
                    LogFormat.formatLineNumber(entry.getFirstLine()), entry.getOccurrences()));
        }
    }

    /** Imprime la tabla de tokens (-t/--tokens) y si la concatenación de lexemas es igual al texto fuente. */
    public void printTokens(List<Token> tokens, boolean invariantHolds) {
        out.println("Tokens:");
        out.println(String.format(TOKEN_ROW_FORMAT, "LINE", "COL", "TYPE", "CATEGORY", "LEXEME"));
        for (Token token : tokens) {
            out.println(String.format(TOKEN_ROW_FORMAT,
                    LogFormat.formatLineNumber(token.getLine()), token.getColumn(),
                    token.getType(), token.getCategory(), quote(token.getLexeme())));
        }
        out.println("Lexeme concatenation equals the source text: "
                + (invariantHolds ? "yes" : "NO"));
    }

    /** Pone el lexema entre comillas simples y escapa CR, LF, tabulación, barra invertida y controles para que quepa en una fila. */
    private static String quote(String lexeme) {
        StringBuilder text = new StringBuilder("'");
        for (int i = 0; i < lexeme.length(); i++) {
            char c = lexeme.charAt(i);
            if (c == '\r') {
                text.append("\\r");
            } else if (c == '\n') {
                text.append("\\n");
            } else if (c == '\t') {
                text.append("\\t");
            } else if (c == '\\') {
                text.append("\\\\");
            } else if (Character.isISOControl(c)) {
                text.append(String.format("\\u%04X", (int) c));
            } else {
                text.append(c);
            }
        }
        return text.append('\'').toString();
    }

    /** Mensaje exacto (PRD §3.1) para un archivo de consola que no termina exactamente en .php. */
    public void printWrongExtension(String argument) {
        out.println(ERROR_PREFIX + "'" + argument + "' does not have the required "
                + PhpFileLocator.PHP_EXTENSION + " extension; the file was not processed.");
    }

    /** Mensaje para un archivo indicado en la consola que no existe. */
    public void printFileNotFound(String argument, List<Path> searched) {
        out.println(ERROR_PREFIX + "file '" + argument + "' was not found (searched in "
                + joinQuoted(searched) + "); the file was not processed.");
    }

    /** Mensaje cuando no se encontró ningún archivo .php en los directorios revisados. */
    public void printNoPhpFound(List<Path> searched) {
        out.println(ERROR_PREFIX + "no file with the " + PhpFileLocator.PHP_EXTENSION
                + " extension was found in " + joinQuoted(searched) + ".");
        out.println("Place the " + PhpFileLocator.PHP_EXTENSION
                + " file in the same folder as the .jar and run it again.");
    }

    /** Mensaje cuando un archivo no se pudo leer. */
    public void printReadError(Path file, String detail) {
        out.println(ERROR_PREFIX + "could not read '" + file + "': " + detail);
    }

    /** Mensaje cuando la bitácora no se pudo escribir. */
    public void printLogWriteError(Path log, String detail) {
        out.println(ERROR_PREFIX + "could not write the log '" + log + "': " + detail
                + ". Check that the folder is not read-only.");
    }

    /** Mensaje cuando no se pudo listar un directorio durante la búsqueda. */
    public void printSearchError(String detail) {
        out.println(ERROR_PREFIX + "could not search for " + PhpFileLocator.PHP_EXTENSION
                + " files: " + detail);
    }

    /** Mensaje para un uso incorrecto de los argumentos de consola. */
    public void printUsageError(String detail) {
        out.println(ERROR_PREFIX + detail);
    }

    /** Une las rutas entre comillas simples separadas por " or ". */
    private static String joinQuoted(List<Path> paths) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < paths.size(); i++) {
            if (i > 0) {
                text.append(" or ");
            }
            text.append('\'').append(paths.get(i)).append('\'');
        }
        return text.toString();
    }
}
