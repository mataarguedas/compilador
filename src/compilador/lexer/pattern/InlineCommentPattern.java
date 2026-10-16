package compilador.lexer.pattern;

import compilador.lexer.CharacterStream;
import compilador.lexer.CharacterStream.Mark;
import compilador.lexer.TokenType;

/** Reconoce un comentario de línea con // o # hasta el fin de línea; su contenido (incluso ?>) no genera tokens; Etapa 3. Regex equivalente: (//|#)[^\r\n]* */
public final class InlineCommentPattern implements LexemePattern {

    // Marcadores de inicio de comentario de línea.
    private static final String DOUBLE_SLASH = "//";
    private static final String HASH = "#";

    /** Puede empezar en "//" o "#"; dentro de cadenas no llega aquí porque el patrón de cadena las consume completas. */
    @Override
    public boolean canStart(CharacterStream stream) {
        return stream.lookingAt(DOUBLE_SLASH) || stream.lookingAt(HASH);
    }

    /** Consume hasta antes del terminador de línea o el final del texto. */
    @Override
    public PatternMatch match(CharacterStream stream) {
        Mark start = stream.mark();
        while (!stream.isAtEnd() && !AsciiChars.isLineBreak(stream.peek())) {
            stream.advance();
        }
        return new PatternMatch(TokenType.INLINE_COMMENT, stream.textSince(start));
    }
}
