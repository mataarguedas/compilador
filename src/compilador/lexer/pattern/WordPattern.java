package compilador.lexer.pattern;

import compilador.lexer.CharacterStream;
import compilador.lexer.CharacterStream.Mark;
import compilador.lexer.TokenType;

/** Reconoce una palabra suelta sin $ con tipo provisional IDENTIFIER (la Etapa 2 decide si es KEYWORD); Etapa 3. Regex equivalente: [A-Za-z_][A-Za-z0-9_]* */
public final class WordPattern implements LexemePattern {

    /** Puede empezar con una letra ASCII o guion bajo. */
    @Override
    public boolean canStart(CharacterStream stream) {
        return AsciiChars.isNameStart(stream.peek());
    }

    /** Consume el primer carácter y luego letras, dígitos y guiones bajos. */
    @Override
    public PatternMatch match(CharacterStream stream) {
        Mark start = stream.mark();
        stream.advance();
        while (AsciiChars.isNameChar(stream.peek())) {
            stream.advance();
        }
        return new PatternMatch(TokenType.IDENTIFIER, stream.textSince(start));
    }
}
