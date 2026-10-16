package compilador.app;

import compilador.io.ErrorLogWriter;
import compilador.io.PhpFileLocator;
import compilador.io.PhpFileLocator.SearchResult;
import compilador.io.SourceFile;
import compilador.lexer.LexemeInvariant;
import compilador.report.ConsoleReporter;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.List;

/** Orquesta todo el flujo (localizar, leer, analizar, escribir bitácora y reportar); coordina las cuatro etapas léxicas. */
public final class Application {

    // Localizador de archivos .php, analizador por archivo y reportero de consola.
    private final PhpFileLocator locator = new PhpFileLocator();
    private final FileAnalyzer analyzer = new FileAnalyzer();
    private final ConsoleReporter reporter = new ConsoleReporter(System.out);

    /** Ejecuta el analizador con los argumentos de consola y devuelve un código de ExitCode. */
    public int run(String[] args) {
        reporter.printHeader();
        CliOptions options;
        try {
            options = CliOptions.parse(args);
        } catch (IllegalArgumentException e) {
            reporter.printUsageError(e.getMessage());
            return ExitCode.FAILURE;
        }
        SearchResult search;
        try {
            search = locator.locate(options.getFileArgument());
        } catch (IOException e) {
            reporter.printSearchError(e.getMessage());
            return ExitCode.FAILURE;
        }
        reporter.printIgnored(search.getIgnored());
        if (!reportSearchProblem(search)) {
            return ExitCode.FAILURE;
        }
        return analyzeAll(search.getFiles(), options.isShowTokens());
    }

    /** Devuelve true si se encontraron archivos; si no, imprime el motivo y devuelve false. */
    private boolean reportSearchProblem(SearchResult search) {
        switch (search.getStatus()) {
            case FOUND:
                return true;
            case WRONG_EXTENSION:
                reporter.printWrongExtension(search.getArgument());
                return false;
            case FILE_NOT_FOUND:
                reporter.printFileNotFound(search.getArgument(), search.getSearchedDirectories());
                return false;
            default:
                reporter.printNoPhpFound(search.getSearchedDirectories());
                return false;
        }
    }

    /** Analiza cada archivo sin detenerse si uno falla; devuelve FAILURE si alguno no se pudo procesar. */
    private int analyzeAll(List<Path> files, boolean showTokens) {
        int exitCode = ExitCode.SUCCESS;
        for (Path file : files) {
            if (!analyze(file, showTokens)) {
                exitCode = ExitCode.FAILURE;
            }
        }
        return exitCode;
    }

    /** Ejecuta el flujo completo sobre un archivo; devuelve false si no se pudo leer o escribir su bitácora. */
    private boolean analyze(Path file, boolean showTokens) {
        SourceFile source;
        try {
            source = analyzer.read(file);
        } catch (IOException e) {
            reporter.printReadError(file, describe(e));
            return false;
        }
        AnalysisResult result = analyzer.analyze(source);
        Path logPath;
        try {
            logPath = analyzer.writeLog(result);
        } catch (IOException e) {
            reporter.printLogWriteError(ErrorLogWriter.logPathFor(file), describe(e));
            return false;
        }
        reporter.printFileSummary(source, result.getTokens().size(), result.getErrors().size(),
                logPath);
        reporter.printSymbols(result.getSymbols());
        // La tabla de tokens solo se imprime si el usuario pasó -t/--tokens.
        if (showTokens) {
            reporter.printTokens(result.getTokens(),
                    LexemeInvariant.holds(result.getTokens(), source.getRawText()));
        }
        return true;
    }

    /** Arma un motivo legible del error de E/S, ya que las excepciones del sistema de archivos solo traen la ruta. */
    private static String describe(IOException e) {
        String detail = e instanceof FileSystemException
                ? ((FileSystemException) e).getReason() : e.getMessage();
        String type = e.getClass().getSimpleName();
        return detail == null ? type : type + " (" + detail + ")";
    }
}
