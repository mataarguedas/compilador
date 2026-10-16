package compilador;

import compilador.app.Application;

// Proyecto desarrollado con apoyo de IA generativa (Claude Code, Anthropic), revisado por el estudiante; ver sección 12 de INFORME.txt.
/** Punto de entrada del analizador léxico de PHP; no pertenece a ninguna etapa léxica, solo delega en Application. */
public class Compilador {

    /** Crea la aplicación, la ejecuta con los argumentos de consola y termina con su código de salida. */
    public static void main(String[] args) {
        System.exit(new Application().run(args));
    }
}
