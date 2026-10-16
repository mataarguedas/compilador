package compilador.lexer.pattern;

import compilador.lexer.CharacterStream;
import compilador.lexer.CharacterStream.Mark;
import compilador.lexer.TokenType;

/** Reconoce una variable de forma permisiva ($1variable o $ solo también son VARIABLE; el validador decide); Etapa 3. Regex equivalente: \$[A-Za-z0-9_]* */
public final class VariablePattern implements LexemePattern {

    // Signo que inicia una variable.
    private static final char DOLLAR = '$';

    // Estados del autómata: DOLLAR -> NAME (mientras haya [A-Za-z0-9_]) -> DONE.
    private enum State { DOLLAR, NAME, DONE }

    /** Puede empezar si el siguiente carácter es $. */
    @Override
    public boolean canStart(CharacterStream stream) {
        return stream.peek() == DOLLAR;
    }

    /** Consume el $ y luego letras, dígitos y guiones bajos ASCII; un carácter no ASCII como ñ termina el token. */
    @Override
    public PatternMatch match(CharacterStream stream) {
        Mark start = stream.mark();
        State state = State.DOLLAR;
        while (state != State.DONE) {
            if (state == State.DOLLAR) {
                stream.advance();
                state = State.NAME;
            } else if (AsciiChars.isNameChar(stream.peek())) {
                stream.advance();
            } else {
                state = State.DONE;
            }
        }
        return new PatternMatch(TokenType.VARIABLE, stream.textSince(start));
    }
}
