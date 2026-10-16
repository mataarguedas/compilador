package compilador.errors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Acumula los errores léxicos de un archivo; todos los validadores de la Etapa 4 escriben en el mismo colector. */
public final class ErrorCollector {

    // Errores en el orden en que se reportaron.
    private final List<LexicalError> errors = new ArrayList<LexicalError>();

    /** Registra un error; no acepta null. */
    public void add(LexicalError error) {
        if (error == null) {
            throw new IllegalArgumentException("error is required");
        }
        errors.add(error);
    }

    /** Atajo que construye y registra un error con código, línea, columna (0 = toda la línea) y descripción. */
    public void add(ErrorCode code, int line, int column, String description) {
        add(new LexicalError(code, line, column, description));
    }

    /** Devuelve una copia de solo lectura de los errores ordenados por línea, columna y código. */
    public List<LexicalError> getSorted() {
        List<LexicalError> sorted = new ArrayList<LexicalError>(errors);
        Collections.sort(sorted);
        return Collections.unmodifiableList(sorted);
    }

    /** Devuelve la cantidad de errores registrados. */
    public int count() {
        return errors.size();
    }

    /** Indica si todavía no se ha registrado ningún error. */
    public boolean isEmpty() {
        return errors.isEmpty();
    }
}
