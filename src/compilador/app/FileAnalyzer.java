package compilador.app;

import compilador.io.ErrorLogWriter;
import compilador.io.SourceFile;
import compilador.io.SourceFileReader;
import compilador.lexer.Lexer;
import compilador.lexer.Token;
import compilador.symbols.SymbolTable;
import compilador.validation.ValidationPipeline;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Flujo de análisis de un archivo (leer, tokenizar, tabla de símbolos, validar, bitácora) sin imprimir nada; encadena las etapas 1 a 4. */
public final class FileAnalyzer {

    // Componentes de cada paso del flujo: lectura, etapas léxicas, validación y escritura de la bitácora.
    private final SourceFileReader reader = new SourceFileReader();
    private final Lexer lexer = new Lexer();
    private final ValidationPipeline validation = new ValidationPipeline();
    private final ErrorLogWriter logWriter = new ErrorLogWriter();

    /** Lee un archivo sin alterar su formato (PRD §3.2); lanza IOException si no se puede leer. */
    public SourceFile read(Path file) throws IOException {
        return reader.read(file);
    }

    /** Ejecuta las cuatro etapas léxicas sobre un archivo ya leído y devuelve el resultado. */
    public AnalysisResult analyze(SourceFile source) {
        List<Token> tokens = lexer.tokenize(source.getRawText());
        SymbolTable symbols = SymbolTable.build(tokens);
        return new AnalysisResult(source, tokens, symbols,
                validation.validate(source, tokens).getSorted());
    }

    /** Escribe <nombre>-errors.log junto al archivo fuente (PRD §4) y devuelve su ruta. */
    public Path writeLog(AnalysisResult result) throws IOException {
        return logWriter.write(result.getSource(), result.getErrors());
    }
}
