package compilador.io;

/** Constantes y reglas de formato de la bitácora de errores (PRD §4); formato de salida de los resultados de la Etapa 4. */
public final class LogFormat {

    /** Sufijo que reemplaza ".php" en el nombre de la bitácora: codigo-errors.log. */
    public static final String LOG_FILE_SUFFIX = "-errors.log";

    /** Texto entre el número de línea y el contenido copiado; cambiarlo a "" pega el número al texto. */
    public static final String NUMBER_SEPARATOR = " ";

    /** Cantidad mínima de dígitos del número de línea; los números más largos nunca se truncan. */
    public static final int LINE_NUMBER_WIDTH = 4;

    /** Última línea de la bitácora cuando el análisis no encontró problemas. */
    public static final String NO_ERRORS_MESSAGE = "No lexical errors found.";

    // Formato que rellena con ceros a la izquierda: "%04d".
    private static final String LINE_NUMBER_FORMAT = "%0" + LINE_NUMBER_WIDTH + "d";

    // Constructor privado: clase de utilidades que no se instancia.
    private LogFormat() {
    }

    /** Rellena el número de línea con ceros a 4 dígitos: 5 -> "0005", 12345 -> "12345". */
    public static String formatLineNumber(int lineNumber) {
        return String.format(LINE_NUMBER_FORMAT, lineNumber);
    }
}
