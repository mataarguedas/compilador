package compilador.lexer.pattern;

import compilador.lexer.CharacterStream;
import compilador.lexer.CharacterStream.Mark;
import compilador.lexer.TokenType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// Regex equivalente (alternativas más largas primero): <<=|>>=|\*\*=|\?\?=|\?->|\.\.\.|<=>|===|!==|==|!=|<>|<=|>=|&&|\|\||\+\+|--|\+=|-=|\*=|/=|\.=|%=|&=|\|=|\^=|->|=>|::|<<|>>|\?\?|\*\*|[=+\-*\/%.<>!&|^~?:@;,(){}\[\]\\]
/** Reconoce operadores y signos de PHP por coincidencia más larga (=== antes que == y que =) con tipo provisional OTHER_OPERATOR; Etapa 3. */
public final class OperatorPattern implements LexemePattern {

    /** Todos los operadores y signos de puntuación reconocidos en modo PHP. */
    public static final List<String> OPERATORS = Collections.unmodifiableList(Arrays.asList(
            "<<=", ">>=", "**=", "??=", "?->", "...", "<=>", "===", "!==",
            "==", "!=", "<>", "<=", ">=", "&&", "||", "++", "--",
            "+=", "-=", "*=", "/=", ".=", "%=", "&=", "|=", "^=",
            "->", "=>", "::", "<<", ">>", "??", "**",
            "=", "+", "-", "*", "/", "%", ".", "<", ">", "!", "&", "|", "^", "~",
            "?", ":", "@", ";", ",", "(", ")", "{", "}", "[", "]", "\\"));

    // Los mismos operadores ordenados del más largo al más corto.
    private static final List<String> LONGEST_FIRST = sortLongestFirst(OPERATORS);

    /** Puede empezar si algún operador coincide en la posición actual. */
    @Override
    public boolean canStart(CharacterStream stream) {
        return longestMatch(stream) != null;
    }

    /** Consume el operador más largo que coincide; la Etapa 2 le asigna el tipo final (ASSIGN, SEMICOLON...). */
    @Override
    public PatternMatch match(CharacterStream stream) {
        Mark start = stream.mark();
        String operator = longestMatch(stream);
        for (int i = 0; i < operator.length(); i++) {
            stream.advance();
        }
        return new PatternMatch(TokenType.OTHER_OPERATOR, stream.textSince(start));
    }

    /** Devuelve el operador más largo con el que comienza el texto, o null si ninguno. */
    private static String longestMatch(CharacterStream stream) {
        for (String operator : LONGEST_FIRST) {
            if (stream.lookingAt(operator)) {
                return operator;
            }
        }
        return null;
    }

    /** Ordena la lista de operadores por longitud descendente. */
    private static List<String> sortLongestFirst(List<String> operators) {
        List<String> sorted = new ArrayList<String>(operators);
        Collections.sort(sorted, new Comparator<String>() {
            // Compara por longitud para que los más largos queden primero.
            @Override
            public int compare(String a, String b) {
                return b.length() - a.length();
            }
        });
        return Collections.unmodifiableList(sorted);
    }
}
