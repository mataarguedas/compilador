package compilador.lexer.pattern;

import compilador.lexer.CharacterStream;
import compilador.lexer.CharacterStream.Mark;
import compilador.lexer.TokenType;

// Regex equivalente (D = [0-9](_?[0-9])*): INTEGER = D | 0[xX][0-9a-fA-F](_?[0-9a-fA-F])* | 0[bB][01](_?[01])* | 0[oO][0-7](_?[0-7])* ; FLOAT = (D\.D? | \.D)([eE][+-]?D)? | D[eE][+-]?D ; MALFORMED = (cualquiera)[A-Za-z0-9_]+
/** Reconoce literales numéricos (enteros, flotantes, exponentes, hex, binario, octal, separador _); si le siguen letras es MALFORMED_NUMBER (402); Etapa 3. */
public final class NumberPattern implements LexemePattern {

    // Punto decimal, cero inicial de los prefijos y bases numéricas admitidas.
    private static final char DOT = '.';
    private static final char ZERO = '0';
    private static final int DECIMAL = 10;
    private static final int HEXADECIMAL = 16;
    private static final int BINARY_RADIX = 2;
    private static final int OCTAL_RADIX = 8;

    // Estados del autómata; cada estado consume dígitos de su base (o "_" seguido de dígito) hasta llegar a DONE.
    private enum State {
        START, INTEGER, FRACTION_START, FRACTION, EXPONENT, HEX, BINARY, OCTAL, DONE
    }

    /** Puede empezar con un dígito o con un punto seguido de dígito (.5). */
    @Override
    public boolean canStart(CharacterStream stream) {
        return AsciiChars.isDigit(stream.peek())
                || (stream.peek() == DOT && AsciiChars.isDigit(stream.peek(1)));
    }

    /** Ejecuta el autómata, decide si es entero o flotante y marca como mal formado si siguen letras, dígitos o _. */
    @Override
    public PatternMatch match(CharacterStream stream) {
        Mark start = stream.mark();
        State state = State.START;
        boolean isFloat = false;
        while (state != State.DONE) {
            state = step(state, stream);
            isFloat |= state == State.FRACTION_START || state == State.EXPONENT;
        }
        // Caso 12abc: todo el bloque es un solo token MALFORMED_NUMBER.
        if (AsciiChars.isNameChar(stream.peek())) {
            consumeNameChars(stream);
            return new PatternMatch(TokenType.MALFORMED_NUMBER, stream.textSince(start));
        }
        TokenType type = isFloat ? TokenType.FLOAT_LITERAL : TokenType.INTEGER_LITERAL;
        return new PatternMatch(type, stream.textSince(start));
    }

    /** Realiza una transición del autómata consumiendo los caracteres que reconoce. */
    private static State step(State state, CharacterStream stream) {
        switch (state) {
            case START:
                return fromStart(stream);
            case INTEGER:
                return fromInteger(stream);
            case FRACTION_START:
                if (acceptDigit(stream, DECIMAL, false)) {
                    return State.FRACTION;
                }
                return acceptExponent(stream) ? State.EXPONENT : State.DONE;
            case FRACTION:
                if (acceptDigit(stream, DECIMAL, true)) {
                    return State.FRACTION;
                }
                return acceptExponent(stream) ? State.EXPONENT : State.DONE;
            case EXPONENT:
                return acceptDigit(stream, DECIMAL, true) ? State.EXPONENT : State.DONE;
            case HEX:
                return acceptDigit(stream, HEXADECIMAL, true) ? State.HEX : State.DONE;
            case BINARY:
                return acceptDigit(stream, BINARY_RADIX, true) ? State.BINARY : State.DONE;
            case OCTAL:
                return acceptDigit(stream, OCTAL_RADIX, true) ? State.OCTAL : State.DONE;
            default:
                return State.DONE;
        }
    }

    /** Desde START: prueba un prefijo 0x/0b/0o; si no, consume el primer dígito (INTEGER) o el punto (FRACTION_START). */
    private static State fromStart(CharacterStream stream) {
        if (stream.peek() == ZERO) {
            State prefixed = acceptRadixPrefix(stream);
            if (prefixed != null) {
                return prefixed;
            }
        }
        State next = stream.peek() == DOT ? State.FRACTION_START : State.INTEGER;
        stream.advance();
        return next;
    }

    /** Desde INTEGER: sigue con dígitos, pasa a fracción con un punto (no "..") o a exponente. */
    private static State fromInteger(CharacterStream stream) {
        if (acceptDigit(stream, DECIMAL, true)) {
            return State.INTEGER;
        }
        if (stream.peek() == DOT && stream.peek(1) != DOT) {
            stream.advance();
            return State.FRACTION_START;
        }
        return acceptExponent(stream) ? State.EXPONENT : State.DONE;
    }

    /** Consume 0x, 0b u 0o si le sigue un dígito válido de esa base; devuelve el estado de la base o null. */
    private static State acceptRadixPrefix(CharacterStream stream) {
        int marker = Character.toLowerCase(stream.peek(1));
        State state;
        int radix;
        if (marker == 'x') {
            state = State.HEX;
            radix = HEXADECIMAL;
        } else if (marker == 'b') {
            state = State.BINARY;
            radix = BINARY_RADIX;
        } else if (marker == 'o') {
            state = State.OCTAL;
            radix = OCTAL_RADIX;
        } else {
            return null;
        }
        if (!isDigit(stream.peek(2), radix)) {
            return null;
        }
        // Consume el cero, la letra de la base y el primer dígito.
        stream.advance();
        stream.advance();
        stream.advance();
        return state;
    }

    /** Consume un dígito de la base o, si se permite, un "_" seguido de un dígito. */
    private static boolean acceptDigit(CharacterStream stream, int radix, boolean allowSeparator) {
        if (isDigit(stream.peek(), radix)) {
            stream.advance();
            return true;
        }
        if (allowSeparator && stream.peek() == AsciiChars.UNDERSCORE
                && isDigit(stream.peek(1), radix)) {
            stream.advance();
            stream.advance();
            return true;
        }
        return false;
    }

    /** Consume e/E, signo opcional y el primer dígito del exponente, solo si todos están presentes. */
    private static boolean acceptExponent(CharacterStream stream) {
        if (stream.peek() != 'e' && stream.peek() != 'E') {
            return false;
        }
        boolean signed = stream.peek(1) == '+' || stream.peek(1) == '-';
        int digitOffset = signed ? 2 : 1;
        if (!AsciiChars.isDigit(stream.peek(digitOffset))) {
            return false;
        }
        for (int i = 0; i <= digitOffset; i++) {
            stream.advance();
        }
        return true;
    }

    /** Indica si el carácter es un dígito ASCII válido en la base dada. */
    private static boolean isDigit(int c, int radix) {
        return c != CharacterStream.END && Character.digit(c, radix) >= 0 && c < 128;
    }

    /** Consume todas las letras, dígitos y guiones bajos que siguen al número. */
    private static void consumeNameChars(CharacterStream stream) {
        while (AsciiChars.isNameChar(stream.peek())) {
            stream.advance();
        }
    }
}
