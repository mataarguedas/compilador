package compilador.validation;

import compilador.lexer.Token;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Lado izquierdo de una asignación "=": texto crudo ($notas[$i]) y base a validar ($notas) (PRD §5.3); Etapa 4. */
public final class AssignmentTarget {

    // Secuencias donde termina la base del destino.
    private static final String[] BASE_TERMINATORS = {"[", "->", "::"};

    // Token "=", tokens del destino, texto concatenado y si es una constante declarada con const.
    private final Token assign;
    private final List<Token> tokens;
    private final String raw;
    private final boolean constant;

    /** Crea el destino y arma su texto crudo concatenando los lexemas (la lista puede estar vacía). */
    public AssignmentTarget(Token assign, List<Token> tokens, boolean constant) {
        this.assign = assign;
        this.tokens = Collections.unmodifiableList(new ArrayList<Token>(tokens));
        this.constant = constant;
        StringBuilder text = new StringBuilder();
        for (Token token : tokens) {
            text.append(token.getLexeme());
        }
        this.raw = text.toString();
    }

    /** Devuelve el token "=". */
    public Token getAssign() {
        return assign;
    }

    /** Devuelve los tokens del destino en orden del fuente. */
    public List<Token> getTokens() {
        return tokens;
    }

    /** Devuelve los lexemas concatenados del destino. */
    public String getRaw() {
        return raw;
    }

    /** Devuelve el destino crudo hasta el primer "[", "->" o "::". */
    public String getBase() {
        int end = raw.length();
        for (String terminator : BASE_TERMINATORS) {
            int index = raw.indexOf(terminator);
            if (index >= 0 && index < end) {
                end = index;
            }
        }
        return raw.substring(0, end);
    }

    /** Indica si el destino es un nombre de constante declarado con const. */
    public boolean isConstant() {
        return constant;
    }

    /** Indica si ningún token precede al "=". */
    public boolean isEmpty() {
        return tokens.isEmpty();
    }

    /** Devuelve la línea de la asignación. */
    public int getLine() {
        return assign.getLine();
    }

    /** Devuelve la columna del primer carácter del destino, o la del "=" si está vacío. */
    public int getColumn() {
        return tokens.isEmpty() ? assign.getColumn() : tokens.get(0).getColumn();
    }
}
