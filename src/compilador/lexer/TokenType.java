package compilador.lexer;

/** Todos los tipos de token que produce el léxico, cada uno asociado a su TokenCategory (PRD §6); Etapa 2: clasificación. */
public enum TokenType {

    // Etiquetas de apertura/cierre y texto fuera de PHP.
    OPEN_TAG(TokenCategory.TAG),
    CLOSE_TAG(TokenCategory.TAG),
    INLINE_TEXT(TokenCategory.INLINE_TEXT),

    // Palabras: variables, identificadores y palabras reservadas.
    VARIABLE(TokenCategory.IDENTIFIER),
    IDENTIFIER(TokenCategory.IDENTIFIER),
    KEYWORD(TokenCategory.KEYWORD),

    // Literales numéricos y de cadena.
    INTEGER_LITERAL(TokenCategory.LITERAL),
    FLOAT_LITERAL(TokenCategory.LITERAL),
    STRING_LITERAL(TokenCategory.LITERAL),

    // Comentarios de línea y de bloque.
    INLINE_COMMENT(TokenCategory.COMMENT),
    BLOCK_COMMENT(TokenCategory.COMMENT),

    // Operadores.
    ASSIGN(TokenCategory.OPERATOR),
    COMPARISON_OPERATOR(TokenCategory.OPERATOR),
    ARITHMETIC_OPERATOR(TokenCategory.OPERATOR),
    LOGICAL_OPERATOR(TokenCategory.OPERATOR),
    CONCAT_OPERATOR(TokenCategory.OPERATOR),
    OTHER_OPERATOR(TokenCategory.OPERATOR),

    // Delimitadores.
    SEMICOLON(TokenCategory.DELIMITER),
    COMMA(TokenCategory.DELIMITER),
    LPAREN(TokenCategory.DELIMITER),
    RPAREN(TokenCategory.DELIMITER),
    LBRACE(TokenCategory.DELIMITER),
    RBRACE(TokenCategory.DELIMITER),
    LBRACKET(TokenCategory.DELIMITER),
    RBRACKET(TokenCategory.DELIMITER),

    // Espacios y saltos de línea (se conservan para la adyacencia).
    WHITESPACE(TokenCategory.LAYOUT),
    NEWLINE(TokenCategory.LAYOUT),

    // Lexemas problemáticos que generan errores.
    UNKNOWN(TokenCategory.ERROR),
    MALFORMED_NUMBER(TokenCategory.ERROR),
    UNTERMINATED_STRING(TokenCategory.ERROR),
    UNTERMINATED_COMMENT(TokenCategory.ERROR);

    // Categoría general a la que pertenece el tipo.
    private final TokenCategory category;

    // Asocia cada tipo con su categoría.
    TokenType(TokenCategory category) {
        this.category = category;
    }

    /** Devuelve la categoría general del tipo de token. */
    public TokenCategory getCategory() {
        return category;
    }
}
