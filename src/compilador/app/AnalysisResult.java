package compilador.app;

import compilador.errors.LexicalError;
import compilador.io.SourceFile;
import compilador.lexer.Token;
import compilador.symbols.SymbolTable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Resultado inmutable de las cuatro etapas léxicas para un archivo: fuente, tokens, tabla de símbolos y errores ordenados. */
public final class AnalysisResult {

    // Datos producidos por el análisis de un archivo.
    private final SourceFile source;
    private final List<Token> tokens;
    private final SymbolTable symbols;
    private final List<LexicalError> errors;

    /** Guarda copias no modificables de los tokens y de los errores (ya ordenados por línea, columna y código). */
    public AnalysisResult(SourceFile source, List<Token> tokens, SymbolTable symbols,
            List<LexicalError> errors) {
        this.source = source;
        this.tokens = Collections.unmodifiableList(new ArrayList<Token>(tokens));
        this.symbols = symbols;
        this.errors = Collections.unmodifiableList(new ArrayList<LexicalError>(errors));
    }

    /** Devuelve el archivo fuente analizado. */
    public SourceFile getSource() {
        return source;
    }

    /** Devuelve los tokens en orden de aparición. */
    public List<Token> getTokens() {
        return tokens;
    }

    /** Devuelve la tabla de símbolos del archivo. */
    public SymbolTable getSymbols() {
        return symbols;
    }

    /** Devuelve los errores ordenados por línea, columna y código. */
    public List<LexicalError> getErrors() {
        return errors;
    }
}
