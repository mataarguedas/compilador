package compilador.lexer.pattern;

/** Clases de caracteres compartidas por los patrones; solo las letras ASCII cuentan (ñ no es letra); Etapa 3. */
public final class AsciiChars {

    /** Salto de línea (LF). */
    public static final char LF = '\n';
    /** Retorno de carro (CR). */
    public static final char CR = '\r';
    /** Guion bajo, permitido en nombres y como separador de dígitos en números. */
    public static final char UNDERSCORE = '_';

    // Constructor privado: clase de utilidades que no se instancia.
    private AsciiChars() {
    }

    /** Indica si el carácter es una letra ASCII [A-Za-z]. */
    public static boolean isLetter(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /** Indica si el carácter es un dígito [0-9]. */
    public static boolean isDigit(int c) {
        return c >= '0' && c <= '9';
    }

    /** Indica si el carácter puede iniciar un nombre: [A-Za-z_]. */
    public static boolean isNameStart(int c) {
        return isLetter(c) || c == UNDERSCORE;
    }

    /** Indica si el carácter puede continuar un nombre: [A-Za-z0-9_]. */
    public static boolean isNameChar(int c) {
        return isNameStart(c) || isDigit(c);
    }

    /** Indica si el carácter es \r o \n. */
    public static boolean isLineBreak(int c) {
        return c == CR || c == LF;
    }
}
