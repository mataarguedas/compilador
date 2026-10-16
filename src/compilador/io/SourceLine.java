package compilador.io;

/** Una línea física del archivo tal como se leyó: contenido y terminador original; modelo de entrada para la Etapa 1 y la bitácora. */
public final class SourceLine {

    // Número de línea (desde 1), texto sin terminador y terminador original ("\r\n", "\n", "\r" o "").
    private final int number;
    private final String content;
    private final String terminator;

    /** Crea la línea; el contenido y el terminador son obligatorios y no se recortan ni normalizan. */
    public SourceLine(int number, String content, String terminator) {
        if (content == null || terminator == null) {
            throw new IllegalArgumentException("content and terminator are required");
        }
        this.number = number;
        this.content = content;
        this.terminator = terminator;
    }

    /** Devuelve el número de línea, comenzando en 1. */
    public int getNumber() {
        return number;
    }

    /** Devuelve el texto de la línea sin su terminador. */
    public String getContent() {
        return content;
    }

    /** Devuelve el terminador original de la línea, o "" si no tiene. */
    public String getTerminator() {
        return terminator;
    }

    /** Indica si la línea termina con un salto de línea. */
    public boolean hasTerminator() {
        return !terminator.isEmpty();
    }
}
