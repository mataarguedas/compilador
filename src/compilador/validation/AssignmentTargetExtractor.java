package compilador.validation;

import compilador.lexer.Token;
import compilador.lexer.TokenCategory;
import compilador.lexer.TokenType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/** Encuentra el destino a la izquierda de cada "=" (solo tokens ASSIGN) recorriendo hacia atrás en la misma línea (PRD §5.3); Etapa 4. */
public final class AssignmentTargetExtractor {

    /** Palabra clave que convierte el destino en un nombre de constante. */
    public static final String CONST_KEYWORD = "const";

    // Tipos de token que detienen la recolección del destino (espacios, saltos, etiquetas, ; , ( ) { } [, asignación, comparación, lógicos).
    private static final Set<TokenType> BOUNDARY_TYPES = Collections.unmodifiableSet(EnumSet.of(
            TokenType.WHITESPACE, TokenType.NEWLINE, TokenType.OPEN_TAG, TokenType.CLOSE_TAG,
            TokenType.INLINE_TEXT, TokenType.SEMICOLON, TokenType.COMMA, TokenType.LPAREN,
            TokenType.RPAREN, TokenType.LBRACE, TokenType.RBRACE, TokenType.LBRACKET,
            TokenType.ASSIGN, TokenType.COMPARISON_OPERATOR, TokenType.LOGICAL_OPERATOR,
            TokenType.UNTERMINATED_COMMENT));

    // Operadores compuestos que tampoco pueden ser parte de un nombre.
    private static final Set<String> BOUNDARY_OPERATORS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("+=", "-=", "*=", "/=", ".=", "%=", "**=",
                    "&=", "|=", "^=", "<<=", ">>=", "??=", "=>")));

    /** Devuelve un destino por cada token ASSIGN, excepto las asignaciones de desestructuración. */
    public List<AssignmentTarget> extract(List<Token> tokens) {
        List<AssignmentTarget> targets = new ArrayList<AssignmentTarget>();
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).is(TokenType.ASSIGN)) {
                AssignmentTarget target = extractAt(tokens, i);
                if (target != null) {
                    targets.add(target);
                }
            }
        }
        return targets;
    }

    /** Extrae el destino del "=" en la posición dada, o devuelve null si debe omitirse. */
    private AssignmentTarget extractAt(List<Token> tokens, int assignIndex) {
        int index = assignIndex - 1;
        // Salta los espacios entre el destino y el "=".
        while (index >= 0 && tokens.get(index).is(TokenType.WHITESPACE)) {
            index--;
        }
        if (index >= 0 && isDestructuring(tokens, index)) {
            return null;
        }
        LinkedList<Token> collected = new LinkedList<Token>();
        // Junta tokens pegados hacia atrás; un "]" se salta junto con todo hasta su "[" para que $a[$i + 1] sea un solo destino.
        while (index >= 0 && !isBoundary(tokens.get(index))) {
            int open = tokens.get(index).is(TokenType.RBRACKET)
                    ? matchingOpenBracket(tokens, index) : index;
            for (int k = index; k >= open; k--) {
                collected.addFirst(tokens.get(k));
            }
            index = open - 1;
        }
        boolean constant = isConstKeyword(previousSignificant(tokens, index));
        return new AssignmentTarget(tokens.get(assignIndex), collected, constant);
    }

    /** Detecta desestructuración: ")" antes del "=" (list(...) =) o "[...]" sin nada pegado delante ([$a, $b] =). */
    private static boolean isDestructuring(List<Token> tokens, int index) {
        Token last = tokens.get(index);
        if (last.is(TokenType.RPAREN)) {
            return true;
        }
        if (!last.is(TokenType.RBRACKET)) {
            return false;
        }
        int open = matchingOpenBracket(tokens, index);
        return open != index && (open == 0 || isBoundary(tokens.get(open - 1)));
    }

    /** Devuelve el índice del "[" que corresponde al "]" en la misma línea, o el mismo índice si no hay. */
    private static int matchingOpenBracket(List<Token> tokens, int close) {
        int depth = 0;
        for (int k = close; k >= 0 && !tokens.get(k).is(TokenType.NEWLINE); k--) {
            if (tokens.get(k).is(TokenType.RBRACKET)) {
                depth++;
            } else if (tokens.get(k).is(TokenType.LBRACKET) && --depth == 0) {
                return k;
            }
        }
        return close;
    }

    /** Indica si el token detiene la recolección: tipo frontera, comentario u operador compuesto. */
    private static boolean isBoundary(Token token) {
        return BOUNDARY_TYPES.contains(token.getType())
                || token.getCategory() == TokenCategory.COMMENT
                || (token.is(TokenType.OTHER_OPERATOR)
                        && BOUNDARY_OPERATORS.contains(token.getLexeme()));
    }

    /** Devuelve el token más cercano en o antes del índice que no sea espacio, salto ni comentario, o null. */
    private static Token previousSignificant(List<Token> tokens, int index) {
        for (int k = index; k >= 0; k--) {
            Token token = tokens.get(k);
            if (token.getCategory() != TokenCategory.LAYOUT
                    && token.getCategory() != TokenCategory.COMMENT) {
                return token;
            }
        }
        return null;
    }

    /** Indica si el token es la palabra clave const. */
    private static boolean isConstKeyword(Token token) {
        return token != null && token.is(TokenType.KEYWORD)
                && CONST_KEYWORD.equalsIgnoreCase(token.getLexeme());
    }
}
