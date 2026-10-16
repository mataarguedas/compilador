package compilador.symbols;

import compilador.lexer.Token;
import compilador.lexer.TokenType;
import compilador.lexer.pattern.AsciiChars;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tabla de variables sensible a mayúsculas ($total y $Total son distintas), en orden de aparición; apoyo a la Etapa 2. */
public final class SymbolTable {

    // Longitud del signo $ que precede al nombre.
    private static final int DOLLAR_LENGTH = 1;

    // Entradas indexadas por el texto exacto; LinkedHashMap conserva el orden de aparición.
    private final Map<String, SymbolEntry> entries = new LinkedHashMap<String, SymbolEntry>();

    /** Construye la tabla con los tokens VARIABLE de nombre bien formado (los mal formados son errores, no símbolos). */
    public static SymbolTable build(List<Token> tokens) {
        SymbolTable table = new SymbolTable();
        for (Token token : tokens) {
            if (token.is(TokenType.VARIABLE) && hasWellFormedName(token.getLexeme())) {
                table.record(token.getLexeme(), token.getLine());
            }
        }
        return table;
    }

    /** Registra una aparición del identificador: crea la entrada o incrementa su contador. */
    public void record(String name, int line) {
        SymbolEntry entry = entries.get(name);
        if (entry == null) {
            entries.put(name, new SymbolEntry(name, line));
        } else {
            entry.addOccurrence();
        }
    }

    /** Devuelve las entradas en orden de primera aparición. */
    public List<SymbolEntry> getEntries() {
        return Collections.unmodifiableList(new ArrayList<SymbolEntry>(entries.values()));
    }

    /** Busca la entrada con exactamente ese nombre, o devuelve null. */
    public SymbolEntry find(String name) {
        return entries.get(name);
    }

    /** Devuelve la cantidad de identificadores distintos. */
    public int size() {
        return entries.size();
    }

    /** Indica si después del $ viene una letra o guion bajo. */
    private static boolean hasWellFormedName(String lexeme) {
        return lexeme.length() > DOLLAR_LENGTH
                && AsciiChars.isNameStart(lexeme.charAt(DOLLAR_LENGTH));
    }
}
