package compilador.lexer.pattern;

import compilador.lexer.CharacterStream;
import compilador.lexer.CharacterStream.Mark;
import compilador.lexer.TokenType;

/** Reconoce una secuencia de espacios y tabulaciones (los saltos de línea son NEWLINE aparte); Etapa 3. Regex equivalente: [ \t]+ */
public final class WhitespacePattern implements LexemePattern {

    /** Puede empezar si el siguiente carácter es espacio o tabulación. */
    @Override
    public boolean canStart(CharacterStream stream) {
        return isWhitespace(stream.peek());
    }

    /** Consume todos los espacios y tabulaciones consecutivos. */
    @Override
    public PatternMatch match(CharacterStream stream) {
        Mark start = stream.mark();
        while (isWhitespace(stream.peek())) {
            stream.advance();
        }
        return new PatternMatch(TokenType.WHITESPACE, stream.textSince(start));
    }

    /** Indica si el carácter es espacio o tabulación. */
    private static boolean isWhitespace(int c) {
        return c == ' ' || c == '\t';
    }
}
