package compilador.lexer.pattern;

import compilador.lexer.CharacterStream;
import compilador.lexer.CharacterStream.Mark;
import compilador.lexer.TokenType;

/** Reconoce una cadena entre ', " o ` como un solo token (// # y $x dentro no se analizan); Etapa 3. Regex equivalente: q([^q\\]|\\[\s\S])*q, o si no cierra q[^\r\n]* */
public final class StringPattern implements LexemePattern {

    // Comillas que pueden delimitar una cadena y carácter de escape.
    private static final String QUOTES = "'\"`";
    private static final char BACKSLASH = '\\';

    // Estados del autómata: OPEN -> BODY <-> ESCAPE, BODY -> CLOSED, o UNTERMINATED al llegar al final.
    private enum State { OPEN, BODY, ESCAPE, CLOSED, UNTERMINATED }

    /** Puede empezar si el siguiente carácter es una de las comillas. */
    @Override
    public boolean canStart(CharacterStream stream) {
        return stream.peek() != CharacterStream.END && QUOTES.indexOf(stream.peek()) >= 0;
    }

    /** Ejecuta el autómata; si no cierra, retrocede y corta la cadena al final de su línea (recuperación del error 400). */
    @Override
    public PatternMatch match(CharacterStream stream) {
        Mark start = stream.mark();
        char quote = (char) stream.peek();
        State state = State.OPEN;
        while (state != State.CLOSED && state != State.UNTERMINATED) {
            state = step(state, stream, quote);
        }
        if (state == State.CLOSED) {
            return new PatternMatch(TokenType.STRING_LITERAL, stream.textSince(start));
        }
        // Recuperación: la cadena sin cerrar no se traga un ?> posterior.
        stream.reset(start);
        consumeRestOfLine(stream);
        return new PatternMatch(TokenType.UNTERMINATED_STRING, stream.textSince(start));
    }

    /** Realiza una transición consumiendo como máximo un carácter. */
    private static State step(State state, CharacterStream stream, char quote) {
        if (stream.isAtEnd()) {
            return State.UNTERMINATED;
        }
        char c = stream.advance();
        switch (state) {
            // La comilla de apertura o el carácter escapado llevan al cuerpo.
            case OPEN:
            case ESCAPE:
                return State.BODY;
            default:
                if (c == quote) {
                    return State.CLOSED;
                }
                return c == BACKSLASH ? State.ESCAPE : State.BODY;
        }
    }

    /** Consume la comilla de apertura y el resto de la línea, sin el terminador. */
    private static void consumeRestOfLine(CharacterStream stream) {
        stream.advance();
        while (!stream.isAtEnd() && !AsciiChars.isLineBreak(stream.peek())) {
            stream.advance();
        }
    }
}
