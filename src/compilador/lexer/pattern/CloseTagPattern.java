package compilador.lexer.pattern;

import compilador.lexer.CharacterStream;
import compilador.lexer.CharacterStream.Mark;
import compilador.lexer.TokenType;

/** Reconoce la etiqueta de cierre "?>" en modo PHP (el operador ?-> no lo es); Etapa 3. Regex equivalente: \?> */
public final class CloseTagPattern implements LexemePattern {

    // Texto de la etiqueta de cierre.
    private static final String CLOSE_TAG = "?>";

    /** Puede empezar si el texto actual comienza con "?>". */
    @Override
    public boolean canStart(CharacterStream stream) {
        return stream.lookingAt(CLOSE_TAG);
    }

    /** Consume los dos caracteres de "?>" y devuelve un CLOSE_TAG. */
    @Override
    public PatternMatch match(CharacterStream stream) {
        Mark start = stream.mark();
        for (int i = 0; i < CLOSE_TAG.length(); i++) {
            stream.advance();
        }
        return new PatternMatch(TokenType.CLOSE_TAG, stream.textSince(start));
    }
}
