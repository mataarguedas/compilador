package compilador.lexer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Asigna el TokenType final a un lexema determinado por un patrón de la Etapa 3; Etapa 2: clasificación de tokens. */
public final class TokenClassifier {

    // Tabla que asocia cada operador o delimitador con su tipo de token específico.
    private static final Map<String, TokenType> OPERATOR_TYPES = buildOperatorTypes();

    /** Palabras sueltas pasan a KEYWORD si son reservadas; operadores se buscan en la tabla; los demás tipos quedan igual. */
    public TokenType classify(TokenType determined, String lexeme) {
        if (determined == TokenType.IDENTIFIER) {
            return ReservedWords.isKeyword(lexeme) ? TokenType.KEYWORD : TokenType.IDENTIFIER;
        }
        // Operadores sin tipo propio (+=, .=, ->, =>, ::, ??...) quedan como OTHER_OPERATOR.
        if (determined == TokenType.OTHER_OPERATOR) {
            TokenType type = OPERATOR_TYPES.get(lexeme);
            return type == null ? TokenType.OTHER_OPERATOR : type;
        }
        return determined;
    }

    /** Construye la tabla de operadores: "=" es ASSIGN y los de comparación, aritmética, lógica, concatenación y delimitadores tienen su tipo. */
    private static Map<String, TokenType> buildOperatorTypes() {
        Map<String, TokenType> types = new HashMap<String, TokenType>();
        types.put("=", TokenType.ASSIGN);
        put(types, TokenType.COMPARISON_OPERATOR,
                "==", "===", "!=", "!==", "<>", "<", ">", "<=", ">=", "<=>");
        put(types, TokenType.ARITHMETIC_OPERATOR, "+", "-", "*", "/", "%", "**", "++", "--");
        put(types, TokenType.LOGICAL_OPERATOR, "&&", "||", "!");
        types.put(".", TokenType.CONCAT_OPERATOR);
        types.put(";", TokenType.SEMICOLON);
        types.put(",", TokenType.COMMA);
        types.put("(", TokenType.LPAREN);
        types.put(")", TokenType.RPAREN);
        types.put("{", TokenType.LBRACE);
        types.put("}", TokenType.RBRACE);
        types.put("[", TokenType.LBRACKET);
        types.put("]", TokenType.RBRACKET);
        return Collections.unmodifiableMap(types);
    }

    /** Registra varios lexemas con el mismo tipo de token. */
    private static void put(Map<String, TokenType> types, TokenType type, String... lexemes) {
        for (String lexeme : lexemes) {
            types.put(lexeme, type);
        }
    }
}
