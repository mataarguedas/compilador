package compilador.errors;

/** Catálogo de todos los códigos de error léxico del PRD §5 con su número y categoría; modelo de errores de la Etapa 4. */
public enum ErrorCode {

    // 5.1 Etiqueta de apertura <?php (códigos 100–106).
    OPEN_TEXT_BEFORE(100, Category.OPENING_TAG),
    OPEN_BLANK_BEFORE(101, Category.OPENING_TAG),
    OPEN_MALFORMED(102, Category.OPENING_TAG),
    OPEN_MISSING(103, Category.OPENING_TAG),
    OPEN_NO_SPACE_AFTER(104, Category.OPENING_TAG),
    OPEN_ADDITIONAL(105, Category.OPENING_TAG),
    OPEN_BOM(106, Category.OPENING_TAG),

    // 5.2 Etiqueta de cierre ?> (códigos 110–116).
    CLOSE_MISSING(110, Category.CLOSING_TAG),
    CLOSE_MALFORMED(111, Category.CLOSING_TAG),
    CLOSE_CONTENT_AFTER(112, Category.CLOSING_TAG),
    CLOSE_TRAILING_WHITESPACE(113, Category.CLOSING_TAG),
    CLOSE_TRAILING_LINE_BREAKS(114, Category.CLOSING_TAG),
    CLOSE_TEXT_AFTER(115, Category.CLOSING_TAG),
    CLOSE_NOT_LAST(116, Category.CLOSING_TAG),

    // 5.3 Identificadores (códigos 200–205).
    IDENT_STARTS_WITH_DIGIT(200, Category.IDENTIFIER),
    IDENT_INVALID_START(201, Category.IDENTIFIER),
    IDENT_INVALID_CHARACTER(202, Category.IDENTIFIER),
    IDENT_MISSING_DOLLAR(203, Category.IDENTIFIER),
    IDENT_RESERVED_WORD(204, Category.IDENTIFIER),
    IDENT_EMPTY_TARGET(205, Category.IDENTIFIER),

    // 5.4 Comentarios (códigos 300–304).
    COMMENT_CONTINUATION(300, Category.COMMENT),
    COMMENT_UNMARKED_TEXT(301, Category.COMMENT),
    COMMENT_MALFORMED_MARKER(302, Category.COMMENT),
    COMMENT_MULTILINE_BLOCK(303, Category.COMMENT),
    COMMENT_UNTERMINATED(304, Category.COMMENT),

    // 5.5 Otros lexemas (códigos 400–402).
    LEXEME_UNTERMINATED_STRING(400, Category.LEXEME),
    LEXEME_UNRECOGNIZED_CHARACTER(401, Category.LEXEME),
    LEXEME_MALFORMED_NUMBER(402, Category.LEXEME);

    /** Grupo de reglas léxicas al que pertenece un código (uno por subsección del PRD). */
    public enum Category {
        OPENING_TAG,
        CLOSING_TAG,
        IDENTIFIER,
        COMMENT,
        LEXEME
    }

    // Número que se imprime en la bitácora y categoría de la regla.
    private final int code;
    private final Category category;

    // Asocia a cada constante su número y su categoría.
    ErrorCode(int code, Category category) {
        this.code = code;
        this.category = category;
    }

    /** Devuelve el número que se imprime en la bitácora, por ejemplo 200. */
    public int getCode() {
        return code;
    }

    /** Devuelve el grupo de reglas al que pertenece el código. */
    public Category getCategory() {
        return category;
    }
}
