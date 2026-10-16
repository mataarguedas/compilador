package compilador.lexer.pattern;

import compilador.lexer.TokenType;

/** Resultado de un LexemePattern: el lexema consumido y el tipo de token determinado; Etapa 3: determinación de patrones. */
public final class PatternMatch {

    // Tipo determinado por el patrón y texto consumido.
    private final TokenType type;
    private final String lexeme;

    /** Crea el resultado; el lexema no puede ser vacío. */
    public PatternMatch(TokenType type, String lexeme) {
        if (type == null || lexeme == null || lexeme.isEmpty()) {
            throw new IllegalArgumentException("type and a non-empty lexeme are required");
        }
        this.type = type;
        this.lexeme = lexeme;
    }

    /** Devuelve el tipo de token determinado. */
    public TokenType getType() {
        return type;
    }

    /** Devuelve el texto consumido. */
    public String getLexeme() {
        return lexeme;
    }
}
