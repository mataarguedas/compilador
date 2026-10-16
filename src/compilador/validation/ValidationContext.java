package compilador.validation;

import compilador.io.SourceFile;
import compilador.io.SourceLine;
import compilador.lexer.Token;
import compilador.lexer.TokenType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Todo lo que un validador puede inspeccionar: el archivo fuente y la lista completa de tokens; Etapa 4: validación de lexemas. */
public final class ValidationContext {

    // Archivo analizado y todos sus tokens (incluidos espacios y saltos de línea).
    private final SourceFile source;
    private final List<Token> tokens;

    /** Crea el contexto guardando una copia no modificable de los tokens. */
    public ValidationContext(SourceFile source, List<Token> tokens) {
        this.source = source;
        this.tokens = Collections.unmodifiableList(new ArrayList<Token>(tokens));
    }

    /** Devuelve el archivo fuente analizado. */
    public SourceFile getSource() {
        return source;
    }

    /** Devuelve todos los tokens en orden. */
    public List<Token> getTokens() {
        return tokens;
    }

    /** Devuelve la línea fuente con el número indicado (desde 1). */
    public SourceLine getLine(int number) {
        return source.getLines().get(number - 1);
    }

    /** Devuelve la cantidad de líneas del fuente (al menos 1). */
    public int getLineCount() {
        return source.getLineCount();
    }

    /** Devuelve, en orden, los tokens del tipo indicado. */
    public List<Token> tokensOfType(TokenType type) {
        List<Token> matching = new ArrayList<Token>();
        for (Token token : tokens) {
            if (token.is(type)) {
                matching.add(token);
            }
        }
        return matching;
    }
}
