package compilador;

import compilador.app.AnalysisResult;
import compilador.app.FileAnalyzer;
import compilador.io.LogFormat;
import compilador.io.SourceFile;
import compilador.io.SourceLine;
import compilador.lexer.LexemeInvariant;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Ejecutor de pruebas de regresión sobre test-fixtures (PRD §8) con un main simple, sin JUnit; verifica la salida de las cuatro etapas. */
public final class FixtureRunner {

    // Extensión de los fixtures, carpeta de bitácoras esperadas, carpeta por defecto y longitud del BOM.
    private static final String PHP_EXTENSION = ".php";
    private static final String EXPECTED_DIR = "expected";
    private static final String DEFAULT_FIXTURE_DIR = "test-fixtures";
    private static final int UTF8_BOM_LENGTH = 3;

    // Mismo flujo de análisis que usa el jar.
    private final FileAnalyzer analyzer = new FileAnalyzer();

    // Constructor privado: solo se instancia desde main.
    private FixtureRunner() {
    }

    /** Ejecuta todos los fixtures en una carpeta temporal, imprime PASS/FAIL y termina con 1 si alguno falló. */
    public static void main(String[] args) throws IOException {
        Path fixtureDir = Paths.get(args.length > 0 ? args[0] : DEFAULT_FIXTURE_DIR);
        List<Path> fixtures = listFixtures(fixtureDir);
        if (fixtures.isEmpty()) {
            System.out.println("No " + PHP_EXTENSION + " fixtures found in " + fixtureDir);
            System.exit(1);
        }
        Path workDir = Files.createTempDirectory("php-lexer-fixtures");
        FixtureRunner runner = new FixtureRunner();
        int failed = 0;
        try {
            for (Path fixture : fixtures) {
                if (!runner.runFixture(fixture, fixtureDir.resolve(EXPECTED_DIR), workDir)) {
                    failed++;
                }
            }
        } finally {
            deleteDirectory(workDir);
        }
        System.out.println();
        System.out.println((fixtures.size() - failed) + " passed, " + failed + " failed, "
                + fixtures.size() + " total");
        System.exit(failed == 0 ? 0 : 1);
    }

    /** Copia el fixture, lo analiza, aplica las tres verificaciones e imprime el resultado; devuelve true si todas pasan. */
    private boolean runFixture(Path fixture, Path expectedDir, Path workDir) throws IOException {
        String name = fixture.getFileName().toString();
        Path copy = Files.copy(fixture, workDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        List<String> failures = new ArrayList<String>();
        try {
            SourceFile source = analyzer.read(copy);
            AnalysisResult result = analyzer.analyze(source);
            byte[] log = Files.readAllBytes(analyzer.writeLog(result));
            Path expected = expectedDir.resolve(baseName(name) + LogFormat.LOG_FILE_SUFFIX);
            addIfFailed(failures, checkExpectedLog(log, expected, source.getCharset()));
            addIfFailed(failures, checkNumberedCopy(log, source, Files.readAllBytes(fixture)));
            addIfFailed(failures, checkLexemeInvariant(result));
        } catch (IOException e) {
            failures.add("I/O error: " + e);
        }
        System.out.println((failures.isEmpty() ? "PASS " : "FAIL ") + name);
        for (String failure : failures) {
            System.out.println("     " + failure);
        }
        return failures.isEmpty();
    }

    /** Verificación 1: la bitácora es idéntica byte a byte a la esperada; devuelve null o la primera diferencia. */
    private static String checkExpectedLog(byte[] log, Path expected, Charset charset)
            throws IOException {
        if (!Files.isRegularFile(expected)) {
            return "missing expected log " + expected;
        }
        byte[] wanted = Files.readAllBytes(expected);
        if (Arrays.equals(log, wanted)) {
            return null;
        }
        // Busca la primera línea distinta para mostrar un mensaje útil.
        List<String> actualLines = splitKeepingTerminators(new String(log, charset));
        List<String> wantedLines = splitKeepingTerminators(new String(wanted, charset));
        int count = Math.max(actualLines.size(), wantedLines.size());
        for (int i = 0; i < count; i++) {
            String actual = i < actualLines.size() ? actualLines.get(i) : "<missing>";
            String want = i < wantedLines.size() ? wantedLines.get(i) : "<missing>";
            if (!actual.equals(want)) {
                return "log differs from expected at log line " + (i + 1)
                        + "\n       expected: " + escape(want)
                        + "\n       actual  : " + escape(actual);
            }
        }
        return "log differs from expected in its bytes (charset or encoding)";
    }

    /** Verificación 2: cada línea copiada sin su prefijo NNNN es igual a la fuente con su terminador, y juntas reconstruyen los bytes. */
    private static String checkNumberedCopy(byte[] log, SourceFile source, byte[] sourceBytes) {
        List<String> logLines = splitKeepingTerminators(new String(log, source.getCharset()));
        List<SourceLine> lines = source.getLines();
        if (logLines.size() < lines.size()) {
            return "log has " + logLines.size() + " lines, source has " + lines.size();
        }
        StringBuilder rebuilt = new StringBuilder();
        for (SourceLine line : lines) {
            String prefix = LogFormat.formatLineNumber(line.getNumber()) + LogFormat.NUMBER_SEPARATOR;
            String logLine = logLines.get(line.getNumber() - 1);
            String expected = prefix + line.getContent() + line.getTerminator();
            if (!copyLineMatches(logLine, expected, line.hasTerminator())) {
                return "copy line " + line.getNumber() + " differs"
                        + "\n       expected: " + escape(expected)
                        + "\n       actual  : " + escape(logLine);
            }
            rebuilt.append(line.getContent()).append(line.getTerminator());
        }
        // Se compara contra los bytes originales sin el BOM, que no se reproduce en la bitácora.
        int offset = source.hadBom() ? UTF8_BOM_LENGTH : 0;
        byte[] body = Arrays.copyOfRange(sourceBytes, offset, sourceBytes.length);
        return Arrays.equals(rebuilt.toString().getBytes(source.getCharset()), body)
                ? null : "copy lines without prefixes do not rebuild the source bytes";
    }

    /** Verificación 3: concatenar todos los lexemas reproduce el texto fuente. */
    private static String checkLexemeInvariant(AnalysisResult result) {
        int mismatch = LexemeInvariant.firstMismatch(result.getTokens(),
                result.getSource().getRawText());
        return mismatch < 0 ? null
                : "lexeme concatenation differs from the source at offset " + mismatch;
    }

    /** Divide el texto en líneas, cada una con su propio \r\n, \n o \r. */
    private static List<String> splitKeepingTerminators(String text) {
        List<String> lines = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                i++;
            }
            if (c == '\r' || c == '\n') {
                lines.add(text.substring(start, i + 1));
                start = i + 1;
            }
        }
        if (start < text.length()) {
            lines.add(text.substring(start));
        }
        return lines;
    }

    /** Compara una línea copiada; si la fuente no tenía terminador, acepta el separador que agrega el escritor antes de los errores. */
    private static boolean copyLineMatches(String logLine, String expected,
            boolean hasTerminator) {
        if (hasTerminator) {
            return logLine.equals(expected);
        }
        return logLine.startsWith(expected)
                && isLineBreak(logLine.substring(expected.length()));
    }

    /** Indica si el texto es exactamente un salto de línea. */
    private static boolean isLineBreak(String text) {
        return text.equals("\r\n") || text.equals("\n") || text.equals("\r");
    }

    /** Escapa barras, \r, \n y tabulaciones y pone el texto entre comillas para mostrarlo. */
    private static String escape(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n")
                .replace("\t", "\\t") + "\"";
    }

    /** Agrega el fallo a la lista si no es null. */
    private static void addIfFailed(List<String> failures, String failure) {
        if (failure != null) {
            failures.add(failure);
        }
    }

    /** Devuelve el nombre del archivo sin la extensión .php. */
    private static String baseName(String fileName) {
        return fileName.substring(0, fileName.length() - PHP_EXTENSION.length());
    }

    /** Devuelve los archivos regulares que terminan exactamente en .php, ordenados por nombre. */
    private static List<Path> listFixtures(Path dir) throws IOException {
        List<Path> fixtures = new ArrayList<Path>();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(dir)) {
            for (Path entry : entries) {
                if (Files.isRegularFile(entry)
                        && entry.getFileName().toString().endsWith(PHP_EXTENSION)) {
                    fixtures.add(entry);
                }
            }
        }
        Collections.sort(fixtures);
        return fixtures;
    }

    /** Borra los archivos de la carpeta temporal y luego la carpeta. */
    private static void deleteDirectory(Path dir) throws IOException {
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(dir)) {
            for (Path entry : entries) {
                Files.deleteIfExists(entry);
            }
        }
        Files.deleteIfExists(dir);
    }
}
