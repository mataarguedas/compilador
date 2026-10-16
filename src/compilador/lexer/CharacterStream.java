package compilador.lexer;

/** Lector carácter a carácter del texto sin BOM que lleva posición, línea, columna y modo; Etapa 1: adquisición de tokens. */
public final class CharacterStream {

    /** Valor que devuelve peek() cuando se mira más allá del final del texto. */
    public static final int END = -1;

    // Caracteres de fin de línea.
    private static final char CR = '\r';
    private static final char LF = '\n';

    // Texto, posición actual, línea y columna (desde 1), línea del último carácter consumido y modo del léxico.
    private final String text;
    private int position;
    private int line = 1;
    private int column = 1;
    private int lastConsumedLine = 1;
    private LexerMode mode;

    /** Crea el lector sobre el texto con el modo inicial indicado. */
    public CharacterStream(String text, LexerMode mode) {
        this.text = text;
        this.mode = mode;
    }

    /** Devuelve el carácter a "offset" posiciones adelante (0 = siguiente) sin consumirlo, o END. */
    public int peek(int offset) {
        int index = position + offset;
        return index < text.length() ? text.charAt(index) : END;
    }

    /** Devuelve el siguiente carácter sin consumirlo, o END. */
    public int peek() {
        return peek(0);
    }

    /** Indica si el texto en la posición actual comienza con la cadena esperada. */
    public boolean lookingAt(String expected) {
        return text.startsWith(expected, position);
    }

    /** Consume un carácter y actualiza línea y columna; \r\n, \r y \n cuentan como un solo salto. */
    public char advance() {
        if (isAtEnd()) {
            throw new IllegalStateException("advance() past the end of the text");
        }
        char c = text.charAt(position++);
        lastConsumedLine = line;
        // Un \r seguido de \n no cambia de línea: el salto se cuenta al consumir el \n.
        boolean crBeforeLf = c == CR && peek() == LF;
        if ((c == LF || c == CR) && !crBeforeLf) {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    /** Indica si ya se consumió todo el texto. */
    public boolean isAtEnd() {
        return position >= text.length();
    }

    /** Toma una instantánea de la posición actual para poder volver con reset(). */
    public Mark mark() {
        return new Mark(position, line, column, lastConsumedLine);
    }

    /** Regresa a una posición marcada previamente (retroceso). */
    public void reset(Mark mark) {
        position = mark.position;
        line = mark.line;
        column = mark.column;
        lastConsumedLine = mark.lastConsumedLine;
    }

    /** Devuelve el texto consumido desde la marca indicada. */
    public String textSince(Mark start) {
        return text.substring(start.position, position);
    }

    /** Devuelve el desplazamiento (desde 0) del siguiente carácter. */
    public int getPosition() {
        return position;
    }

    /** Devuelve la línea del siguiente carácter. */
    public int getLine() {
        return line;
    }

    /** Devuelve la columna del siguiente carácter. */
    public int getColumn() {
        return column;
    }

    /** Devuelve la línea del último carácter consumido. */
    public int getLastConsumedLine() {
        return lastConsumedLine;
    }

    /** Devuelve el modo actual del léxico. */
    public LexerMode getMode() {
        return mode;
    }

    /** Cambia el modo del léxico (INLINE o PHP). */
    public void setMode(LexerMode mode) {
        this.mode = mode;
    }

    /** Instantánea inmutable de una posición del lector. */
    public static final class Mark {

        // Valores guardados del lector en el momento de marcar.
        private final int position;
        private final int line;
        private final int column;
        private final int lastConsumedLine;

        // Solo CharacterStream crea marcas.
        private Mark(int position, int line, int column, int lastConsumedLine) {
            this.position = position;
            this.line = line;
            this.column = column;
            this.lastConsumedLine = lastConsumedLine;
        }

        /** Devuelve la línea guardada en la marca. */
        public int getLine() {
            return line;
        }

        /** Devuelve la columna guardada en la marca. */
        public int getColumn() {
            return column;
        }
    }
}
