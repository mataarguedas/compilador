package compilador.lexer;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Las 20 palabras reservadas (PRD §5.3) y sus dos reglas de mayúsculas; Etapa 2: clasificación de tokens. */
public final class ReservedWords {

    /** La verificación de $nombre distingue mayúsculas: $if es error pero $If no. */
    public static final boolean IDENTIFIER_CHECK_CASE_SENSITIVE = true;

    /** La clasificación de palabras sueltas como KEYWORD ignora mayúsculas, como PHP (ECHO es palabra clave). */
    public static final boolean KEYWORD_CLASSIFICATION_CASE_SENSITIVE = false;

    /** Palabras reservadas en minúscula; "Include" del enunciado se trata como "include". */
    public static final Set<String> WORDS = Collections.unmodifiableSet(new HashSet<String>(
            Arrays.asList("class", "function", "if", "while", "echo", "else", "continue",
                    "for", "return", "public", "null", "false", "true", "trim", "foreach",
                    "private", "include", "print", "const", "do")));

    // Constructor privado: clase de utilidades que no se instancia.
    private ReservedWords() {
    }

    /** Indica si el nombre de variable (sin $) es reservado y no puede usarse como identificador. */
    public static boolean isReservedIdentifierName(String name) {
        return contains(name, IDENTIFIER_CHECK_CASE_SENSITIVE);
    }

    /** Indica si una palabra suelta se clasifica como KEYWORD. */
    public static boolean isKeyword(String word) {
        return contains(word, KEYWORD_CLASSIFICATION_CASE_SENSITIVE);
    }

    /** Busca la palabra en la lista, pasándola a minúscula si la comparación no distingue mayúsculas. */
    private static boolean contains(String word, boolean caseSensitive) {
        return WORDS.contains(caseSensitive ? word : word.toLowerCase(Locale.ROOT));
    }
}
