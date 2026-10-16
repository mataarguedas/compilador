package compilador.errors;

import compilador.io.LogFormat;

/** Problema léxico reportado (código, posición y descripción del PRD §5), inmutable y ordenado por línea, columna y código; Etapa 4. */
public final class LexicalError implements Comparable<LexicalError> {

    // Piezas fijas del formato "Error <código>. Line <NNNN>. <descripción>".
    private static final String LOG_PREFIX = "Error ";
    private static final String LINE_LABEL = ". Line ";
    private static final String DESCRIPTION_SEPARATOR = ". ";

    // Código, línea y columna (desde 1; columna 0 = toda la línea) y descripción específica.
    private final ErrorCode code;
    private final int line;
    private final int column;
    private final String description;

    /** Crea el error; el código y la descripción son obligatorios. */
    public LexicalError(ErrorCode code, int line, int column, String description) {
        if (code == null || description == null) {
            throw new IllegalArgumentException("code and description are required");
        }
        this.code = code;
        this.line = line;
        this.column = column;
        this.description = description;
    }

    /** Devuelve el código del error. */
    public ErrorCode getCode() {
        return code;
    }

    /** Devuelve la línea donde se reporta el error. */
    public int getLine() {
        return line;
    }

    /** Devuelve la columna del error, o 0 si afecta toda la línea. */
    public int getColumn() {
        return column;
    }

    /** Devuelve la descripción específica del error. */
    public String getDescription() {
        return description;
    }

    /** Formatea el error tal como aparece en la bitácora: "Error 200. Line 0005. ..." (sin terminador). */
    public String toLogLine() {
        return LOG_PREFIX + code.getCode()
                + LINE_LABEL + LogFormat.formatLineNumber(line)
                + DESCRIPTION_SEPARATOR + description;
    }

    /** Ordena por línea, luego por columna y luego por código numérico. */
    @Override
    public int compareTo(LexicalError other) {
        if (line != other.line) {
            return Integer.compare(line, other.line);
        }
        if (column != other.column) {
            return Integer.compare(column, other.column);
        }
        return Integer.compare(code.getCode(), other.code.getCode());
    }

    /** Dos errores son iguales si coinciden código, línea, columna y descripción. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LexicalError)) {
            return false;
        }
        LexicalError other = (LexicalError) obj;
        return code == other.code && line == other.line && column == other.column
                && description.equals(other.description);
    }

    /** Calcula el hash con los mismos campos que usa equals. */
    @Override
    public int hashCode() {
        int result = code.hashCode();
        result = 31 * result + line;
        result = 31 * result + column;
        return 31 * result + description.hashCode();
    }

    /** Devuelve la misma línea que se escribe en la bitácora. */
    @Override
    public String toString() {
        return toLogLine();
    }
}
