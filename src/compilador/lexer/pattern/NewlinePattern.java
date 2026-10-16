package compilador.lexer.pattern;

import compilador.lexer.CharacterStream;
import compilador.lexer.CharacterStream.Mark;
import compilador.lexer.TokenType;

/** Reconoce un salto de línea conservando su forma original; Etapa 3. Regex equivalente: \r\n|\r|\n */
public final class NewlinePattern implements LexemePattern {

    // Caracteres de fin de línea.
    private static final char CR = '\r';
    private static final char LF = '\n';

    /** Puede empezar si el siguiente carácter es \r o \n. */
    @Override
    public boolean canStart(CharacterStream stream) {
        return stream.peek() == CR || stream.peek() == LF;
    }

    /** Consume \r\n como un solo salto, o bien un \r o \n aislado. */
    @Override
    public PatternMatch match(CharacterStream stream) {
        Mark start = stream.mark();
        char first = stream.advance();
        if (first == CR && stream.peek() == LF) {
            stream.advance();
        }
        return new PatternMatch(TokenType.NEWLINE, stream.textSince(start));
    }
}
