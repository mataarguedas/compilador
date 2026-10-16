package compilador.app;

/** Códigos de salida del proceso (PRD §7); infraestructura de la aplicación, fuera de las etapas léxicas. */
public final class ExitCode {

    /** Se analizó al menos un archivo; los errores léxicos no cuentan como fallo. */
    public static final int SUCCESS = 0;

    /** No se encontró archivo, la extensión era incorrecta u ocurrió un error de E/S. */
    public static final int FAILURE = 1;

    // Constructor privado: clase de constantes que no se instancia.
    private ExitCode() {
    }
}
