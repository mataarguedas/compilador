package compilador.symbols;

/** Un identificador de la tabla de símbolos: texto exacto, primera línea y cantidad de apariciones; apoyo a la Etapa 2. */
public final class SymbolEntry {

    // Nombre exacto (con $), línea de la primera aparición y contador de apariciones.
    private final String name;
    private final int firstLine;
    private int occurrences;

    /** Crea la entrada con su primera aparición. */
    SymbolEntry(String name, int firstLine) {
        this.name = name;
        this.firstLine = firstLine;
        this.occurrences = 1;
    }

    /** Suma una aparición más del identificador. */
    void addOccurrence() {
        occurrences++;
    }

    /** Devuelve el identificador tal como se escribió, incluido el $. */
    public String getName() {
        return name;
    }

    /** Devuelve la línea donde aparece por primera vez. */
    public int getFirstLine() {
        return firstLine;
    }

    /** Devuelve cuántas veces aparece el identificador. */
    public int getOccurrences() {
        return occurrences;
    }
}
