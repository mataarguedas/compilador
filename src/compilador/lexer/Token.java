package compilador.lexer;

/** Lexema reconocido con su tipo y posición; unidad clasificada de la Etapa 2 que consume la Etapa 4 (solo "reported" es mutable). */
public final class Token {

    // Tipo, texto exacto, línea y columna de inicio, línea final y marca de "ya reportado".
    private final TokenType type;
    private final String lexeme;
    private final int line;
    private final int column;
    private final int endLine;
    private boolean reported;

    /** Crea el token; el lexema no puede ser vacío y endLine difiere de line solo si abarca varias líneas. */
    public Token(TokenType type, String lexeme, int line, int column, int endLine) {
        if (type == null || lexeme == null || lexeme.isEmpty()) {
            throw new IllegalArgumentException("type and a non-empty lexeme are required");
        }
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
        this.endLine = endLine;
    }

    /** Devuelve el tipo del token. */
    public TokenType getType() {
        return type;
    }

    /** Devuelve la categoría general del token. */
    public TokenCategory getCategory() {
        return type.getCategory();
    }

    /** Devuelve el texto exacto del token. */
    public String getLexeme() {
        return lexeme;
    }

    /** Devuelve la línea del primer carácter. */
    public int getLine() {
        return line;
    }

    /** Devuelve la columna del primer carácter. */
    public int getColumn() {
        return column;
    }

    /** Devuelve la línea del último carácter. */
    public int getEndLine() {
        return endLine;
    }

    /** Indica si un validador ya reportó un error para este token. */
    public boolean isReported() {
        return reported;
    }

    /** Marca el token como reportado para que los validadores siguientes lo omitan. */
    public void markReported() {
        reported = true;
    }

    /** Indica si el token es del tipo dado. */
    public boolean is(TokenType expected) {
        return type == expected;
    }

    /** Representación de depuración: TIPO@línea:columna[lexema]. */
    @Override
    public String toString() {
        return type + "@" + line + ":" + column + "[" + lexeme + "]";
    }
}
