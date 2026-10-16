package compilador.validation;

import compilador.lexer.Token;

/** Utilidades de texto para armar descripciones de error: líneas en blanco, recorte, extractos de 40 caracteres y líneas de tokens; Etapa 4. */
public final class LineText {

    /** Cantidad máxima de caracteres de un texto citado en una descripción. */
    public static final int MAX_EXCERPT_LENGTH = 40;

    /** Se agrega al final de un texto citado que fue recortado. */
    public static final String ELLIPSIS = "...";

    // Constructor privado: clase de utilidades que no se instancia.
    private LineText() {
    }

    /** Indica si el carácter es espacio o tabulación. */
    public static boolean isSpaceOrTab(char c) {
        return c == ' ' || c == '\t';
    }

    /** Indica si el texto está vacío o solo contiene espacios y tabulaciones. */
    public static boolean isBlank(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (!isSpaceOrTab(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** Quita espacios y tabulaciones al inicio y al final del texto. */
    public static String trim(String text) {
        int start = 0;
        int end = text.length();
        while (start < end && isSpaceOrTab(text.charAt(start))) {
            start++;
        }
        while (end > start && isSpaceOrTab(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(start, end);
    }

    /** Recorta el texto a 40 caracteres (puntos de código) y agrega "..." si era más largo. */
    public static String excerpt(String text) {
        if (text.codePointCount(0, text.length()) <= MAX_EXCERPT_LENGTH) {
            return text;
        }
        return text.substring(0, text.offsetByCodePoints(0, MAX_EXCERPT_LENGTH)) + ELLIPSIS;
    }

    /** Devuelve la línea fuente del carácter en la posición dada dentro de un token multilínea (\r\n cuenta como un salto). */
    public static int lineOfIndex(Token token, int index) {
        String lexeme = token.getLexeme();
        int line = token.getLine();
        for (int i = 0; i < index; i++) {
            char c = lexeme.charAt(i);
            boolean crBeforeLf = c == '\r' && i + 1 < lexeme.length() && lexeme.charAt(i + 1) == '\n';
            if ((c == '\n' || c == '\r') && !crBeforeLf) {
                line++;
            }
        }
        return line;
    }
}
