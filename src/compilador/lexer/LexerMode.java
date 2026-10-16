package compilador.lexer;

/** Los dos estados del analizador léxico (PRD §5.1 y §5.2); Etapa 1: adquisición de tokens. */
public enum LexerMode {

    /** Fuera del código PHP: solo se reconoce "<?" y todo lo demás es texto en línea. */
    INLINE,

    /** Dentro del código PHP: se prueban todos los patrones de lexemas. */
    PHP
}
