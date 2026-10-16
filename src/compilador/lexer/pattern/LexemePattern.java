package compilador.lexer.pattern;

import compilador.lexer.CharacterStream;

/** Reconocedor escrito a mano para un tipo de lexema, equivalente a una expresión regular sin usar regex; Etapa 3: determinación de patrones. */
public interface LexemePattern {

    /** Mira hacia adelante sin consumir e indica si este patrón reconoce un lexema que empieza aquí. */
    boolean canStart(CharacterStream stream);

    /** Consume el lexema más largo que reconoce el patrón (al menos un carácter); solo se llama si canStart devolvió true. */
    PatternMatch match(CharacterStream stream);
}
