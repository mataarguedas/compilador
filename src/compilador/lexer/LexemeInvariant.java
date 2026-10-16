package compilador.lexer;

import java.util.List;

/** Verifica el invariante del léxico: concatenar todos los lexemas reproduce exactamente el texto fuente; autocomprobación de la Etapa 1. */
public final class LexemeInvariant {

    // Constructor privado: clase de utilidades que no se instancia.
    private LexemeInvariant() {
    }

    /** Indica si los lexemas concatenados son exactamente iguales al texto tokenizado. */
    public static boolean holds(List<Token> tokens, String rawText) {
        return firstMismatch(tokens, rawText) < 0;
    }

    /** Devuelve la posición (desde 0) del primer carácter distinto, o -1 si la concatenación es igual al texto. */
    public static int firstMismatch(List<Token> tokens, String rawText) {
        String joined = concatenate(tokens);
        int common = Math.min(joined.length(), rawText.length());
        for (int i = 0; i < common; i++) {
            if (joined.charAt(i) != rawText.charAt(i)) {
                return i;
            }
        }
        // Si el prefijo común coincide, solo difieren si las longitudes son distintas.
        return joined.length() == rawText.length() ? -1 : common;
    }

    /** Une todos los lexemas en orden. */
    public static String concatenate(List<Token> tokens) {
        StringBuilder text = new StringBuilder();
        for (Token token : tokens) {
            text.append(token.getLexeme());
        }
        return text.toString();
    }
}
