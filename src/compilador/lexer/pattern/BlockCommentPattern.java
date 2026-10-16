package compilador.lexer.pattern;

import compilador.lexer.CharacterStream;
import compilador.lexer.CharacterStream.Mark;
import compilador.lexer.TokenType;

// Regex equivalente: BLOCK_COMMENT = /\*([^*]|\*+[^*/])*\*+/ ; si llega al final sin cerrar, UNTERMINATED_COMMENT = /\*[\s\S]*
/** Reconoce un comentario de bloque; si abarca varias líneas el validador da error 303 y si nunca cierra es UNTERMINATED_COMMENT (304); Etapa 3. */
public final class BlockCommentPattern implements LexemePattern {

    // Apertura del comentario y caracteres que forman el cierre.
    private static final String OPENER = "/*";
    private static final char STAR = '*';
    private static final char SLASH = '/';

    // Estados del autómata: SLASH -> OPEN_STAR -> BODY <-> STAR -> CLOSED, o UNTERMINATED al llegar al final.
    private enum State { SLASH, OPEN_STAR, BODY, STAR, CLOSED, UNTERMINATED }

    /** Puede empezar si el texto actual comienza con la apertura de comentario de bloque. */
    @Override
    public boolean canStart(CharacterStream stream) {
        return stream.lookingAt(OPENER);
    }

    /** Ejecuta el autómata hasta CLOSED (BLOCK_COMMENT) o UNTERMINATED (UNTERMINATED_COMMENT). */
    @Override
    public PatternMatch match(CharacterStream stream) {
        Mark start = stream.mark();
        State state = State.SLASH;
        while (state != State.CLOSED && state != State.UNTERMINATED) {
            state = step(state, stream);
        }
        TokenType type = state == State.CLOSED
                ? TokenType.BLOCK_COMMENT : TokenType.UNTERMINATED_COMMENT;
        return new PatternMatch(type, stream.textSince(start));
    }

    /** Realiza una transición consumiendo como máximo un carácter. */
    private static State step(State state, CharacterStream stream) {
        // Los dos primeros estados consumen la barra y el asterisco de apertura.
        if (state == State.SLASH || state == State.OPEN_STAR) {
            stream.advance();
            return state == State.SLASH ? State.OPEN_STAR : State.BODY;
        }
        if (stream.isAtEnd()) {
            return State.UNTERMINATED;
        }
        char c = stream.advance();
        // En BODY un asterisco puede iniciar el cierre.
        if (state == State.BODY) {
            return c == STAR ? State.STAR : State.BODY;
        }
        // En STAR una barra cierra el comentario; otro asterisco se queda en STAR.
        if (c == SLASH) {
            return State.CLOSED;
        }
        return c == STAR ? State.STAR : State.BODY;
    }
}
