package compilador.lexer;

/** Categoría general de un token, derivada de su TokenType (PRD §6); Etapa 2: clasificación de tokens. */
public enum TokenCategory {
    TAG,
    KEYWORD,
    IDENTIFIER,
    LITERAL,
    OPERATOR,
    DELIMITER,
    COMMENT,
    LAYOUT,
    INLINE_TEXT,
    ERROR
}
