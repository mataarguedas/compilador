package compilador.validation;

import compilador.errors.ErrorCode;
import compilador.errors.ErrorCollector;
import compilador.lexer.Token;

/** Revisa los lexemas restantes, códigos 400–402 (cadenas sin cerrar, caracteres desconocidos, números mal formados); Etapa 4. */
public final class LexemeValidator implements Validator {

    // Plantillas de descripción del PRD §5.5.
    private static final String UNTERMINATED_STRING = "String literal opened with %s at column"
            + " %d is never closed.";
    private static final String UNRECOGNIZED = "Unrecognized character '%s' (U+%04X) at column"
            + " %d.";
    private static final String MALFORMED_NUMBER = "Malformed numeric literal '%s' at column %d;"
            + " a number cannot be followed directly by letters.";

    /** Se ejecuta al final y omite tokens ya reportados (así ñ en $ñame = solo da el error 201, no un 401). */
    @Override
    public void validate(ValidationContext context, ErrorCollector errors) {
        for (Token token : context.getTokens()) {
            if (!token.isReported()) {
                check(token, errors);
            }
        }
    }

    /** Reporta el error que corresponde al tipo del token y lo marca como reportado. */
    private static void check(Token token, ErrorCollector errors) {
        int line = token.getLine();
        int column = token.getColumn();
        String lexeme = token.getLexeme();
        switch (token.getType()) {
            case UNTERMINATED_STRING:
                errors.add(ErrorCode.LEXEME_UNTERMINATED_STRING, line, column,
                        String.format(UNTERMINATED_STRING, lexeme.charAt(0), column));
                break;
            case UNKNOWN:
                errors.add(ErrorCode.LEXEME_UNRECOGNIZED_CHARACTER, line, column,
                        String.format(UNRECOGNIZED, lexeme, lexeme.codePointAt(0), column));
                break;
            case MALFORMED_NUMBER:
                errors.add(ErrorCode.LEXEME_MALFORMED_NUMBER, line, column,
                        String.format(MALFORMED_NUMBER, LineText.excerpt(lexeme), column));
                break;
            default:
                return;
        }
        token.markReported();
    }
}
