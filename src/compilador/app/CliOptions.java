package compilador.app;

/** Opciones de consola ya interpretadas: un archivo .php opcional y la bandera -t/--tokens; configuración, sin etapa léxica. */
public final class CliOptions {

    // Banderas aceptadas para mostrar la tabla de tokens y prefijo que identifica una opción.
    private static final String SHORT_TOKENS_FLAG = "-t";
    private static final String LONG_TOKENS_FLAG = "--tokens";
    private static final String OPTION_PREFIX = "-";

    // Archivo indicado por el usuario (o null) y si se debe imprimir la tabla de tokens.
    private final String fileArgument;
    private final boolean showTokens;

    // Constructor privado: las instancias se crean solo mediante parse().
    private CliOptions(String fileArgument, boolean showTokens) {
        this.fileArgument = fileArgument;
        this.showTokens = showTokens;
    }

    /** Interpreta los argumentos; lanza IllegalArgumentException ante una opción desconocida o más de un archivo. */
    public static CliOptions parse(String[] args) {
        String file = null;
        boolean tokens = false;
        for (String arg : args) {
            if (SHORT_TOKENS_FLAG.equals(arg) || LONG_TOKENS_FLAG.equals(arg)) {
                tokens = true;
            } else if (arg.startsWith(OPTION_PREFIX)) {
                throw new IllegalArgumentException("unknown option '" + arg + "'; the only option is "
                        + SHORT_TOKENS_FLAG + " / " + LONG_TOKENS_FLAG + ".");
            } else if (file != null) {
                throw new IllegalArgumentException("only one file can be given, but both '" + file
                        + "' and '" + arg + "' were.");
            } else {
                file = arg;
            }
        }
        return new CliOptions(file, tokens);
    }

    /** Devuelve el archivo indicado en la consola, o null para buscar archivos automáticamente. */
    public String getFileArgument() {
        return fileArgument;
    }

    /** Indica si se debe imprimir la tabla de tokens. */
    public boolean isShowTokens() {
        return showTokens;
    }
}
