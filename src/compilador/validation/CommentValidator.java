package compilador.validation;

import compilador.errors.ErrorCode;
import compilador.errors.ErrorCollector;
import compilador.io.LogFormat;
import compilador.lexer.Token;
import compilador.lexer.TokenCategory;
import compilador.lexer.TokenType;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Revisa el reconocimiento y delimitación de comentarios, códigos 300–304 (PRD §5.4), recorriendo el archivo línea por línea; Etapa 4. */
public final class CommentValidator implements Validator {

    // Plantillas de descripción del PRD §5.4.
    private static final String CONTINUATION = "Text '%s' appears to continue the inline comment"
            + " of line %s; inline comments end at the end of the line and cannot span multiple"
            + " lines (start this line with // or #).";
    private static final String UNMARKED_TEXT = "Text '%s' is not valid PHP code and is not"
            + " marked as a comment; inline comments must start with // or #.";
    private static final String MALFORMED_MARKER = "Malformed comment marker '/' at column %d;"
            + " inline comments must start exactly with '//' or '#'.";
    private static final String MULTILINE_BLOCK = "Comment starting at column %d spans lines %s"
            + " to %s; comments must not extend across multiple lines (use // or # on each"
            + " line).";
    private static final String UNTERMINATED = "Comment '/*' opened at column %d is never closed"
            + " and runs to the end of the file.";

    // Barra suelta y caracteres que, pegados a ella, indican que no es un marcador mal formado.
    private static final String SLASH = "/";
    private static final String NOT_A_MARKER_AFTER_SLASH = "/*=";

    /** Tokens después de los cuales empieza una nueva sentencia (y una posible línea huérfana). */
    private static final Set<TokenType> STATEMENT_STARTERS = EnumSet.of(
            TokenType.SEMICOLON, TokenType.LBRACE, TokenType.RBRACE, TokenType.OPEN_TAG);

    /** Tipos de token que pueden formar una línea de texto huérfano. */
    private static final Set<TokenType> ORPHAN_PARTS = EnumSet.of(
            TokenType.IDENTIFIER, TokenType.KEYWORD, TokenType.INTEGER_LITERAL,
            TokenType.FLOAT_LITERAL, TokenType.MALFORMED_NUMBER, TokenType.UNKNOWN);

    /** Recorre cada línea llevando la profundidad de paréntesis/corchetes y el último token significativo para detectar 300–304. */
    @Override
    public void validate(ValidationContext context, ErrorCollector errors) {
        List<List<Token>> lines = groupByLine(context);
        int depth = 0;
        Token previous = null;
        for (int number = 1; number < lines.size(); number++) {
            List<Token> code = codeTokens(lines.get(number));
            // Una línea huérfana solo cuenta fuera de paréntesis y al inicio de una sentencia.
            if (depth == 0 && startsStatement(previous) && isOrphanText(code)) {
                reportOrphan(context, lines, number, code, errors);
            }
            checkSlashes(code, errors);
            checkBlockComments(lines.get(number), errors);
            for (Token token : code) {
                depth = Math.max(0, depth + depthChange(token));
                previous = token;
            }
        }
    }

    /** Agrupa por número de línea (índice 0 sin usar) los tokens que no son de espaciado. */
    private static List<List<Token>> groupByLine(ValidationContext context) {
        List<List<Token>> lines = new ArrayList<List<Token>>();
        for (int i = 0; i <= context.getLineCount(); i++) {
            lines.add(new ArrayList<Token>());
        }
        for (Token token : context.getTokens()) {
            if (token.getCategory() != TokenCategory.LAYOUT) {
                lines.get(token.getLine()).add(token);
            }
        }
        return lines;
    }

    /** Devuelve los tokens de la línea que no son espaciado ni comentarios. */
    private static List<Token> codeTokens(List<Token> lineTokens) {
        List<Token> code = new ArrayList<Token>();
        for (Token token : lineTokens) {
            if (!isComment(token)) {
                code.add(token);
            }
        }
        return code;
    }

    /** Indica si la línea es texto huérfano: empieza con una palabra no reservada y solo tiene palabras, números o desconocidos. */
    private static boolean isOrphanText(List<Token> code) {
        if (code.isEmpty() || !code.get(0).is(TokenType.IDENTIFIER)) {
            return false;
        }
        for (Token token : code) {
            if (!ORPHAN_PARTS.contains(token.getType())) {
                return false;
            }
        }
        return true;
    }

    /** Reporta 300 si la línea anterior termina en comentario de línea (parece continuarlo) o 301 en otro caso. */
    private static void reportOrphan(ValidationContext context, List<List<Token>> lines,
            int number, List<Token> code, ErrorCollector errors) {
        Token first = code.get(0);
        Token last = code.get(code.size() - 1);
        String text = LineText.excerpt(context.getLine(number).getContent().substring(
                first.getColumn() - 1, last.getColumn() - 1 + last.getLexeme().length()));
        List<Token> previousLine = lines.get(number - 1);
        boolean afterInlineComment = !previousLine.isEmpty()
                && previousLine.get(previousLine.size() - 1).is(TokenType.INLINE_COMMENT);
        if (afterInlineComment) {
            errors.add(ErrorCode.COMMENT_CONTINUATION, number, first.getColumn(), String.format(
                    CONTINUATION, text, LogFormat.formatLineNumber(number - 1)));
        } else {
            errors.add(ErrorCode.COMMENT_UNMARKED_TEXT, number, first.getColumn(),
                    String.format(UNMARKED_TEXT, text));
        }
    }

    /** Reporta 302 por un "/" al inicio de la línea o tras ; { } en la misma línea; un "/" tras un operando ($a / $b) es división. */
    private static void checkSlashes(List<Token> code, ErrorCollector errors) {
        for (int i = 0; i < code.size(); i++) {
            Token token = code.get(i);
            if (!SLASH.equals(token.getLexeme())) {
                continue;
            }
            boolean statementStart = i == 0
                    || STATEMENT_STARTERS.contains(code.get(i - 1).getType());
            if (statementStart && !isGluedToMarker(code, i)) {
                errors.add(ErrorCode.COMMENT_MALFORMED_MARKER, token.getLine(),
                        token.getColumn(), String.format(MALFORMED_MARKER, token.getColumn()));
            }
        }
    }

    /** Reporta 303 por un comentario de bloque cerrado que abarca varias líneas y 304 por uno que nunca se cierra. */
    private static void checkBlockComments(List<Token> lineTokens, ErrorCollector errors) {
        for (Token token : lineTokens) {
            if (token.is(TokenType.UNTERMINATED_COMMENT)) {
                errors.add(ErrorCode.COMMENT_UNTERMINATED, token.getLine(), token.getColumn(),
                        String.format(UNTERMINATED, token.getColumn()));
            } else if (token.is(TokenType.BLOCK_COMMENT) && token.getEndLine() > token.getLine()) {
                errors.add(ErrorCode.COMMENT_MULTILINE_BLOCK, token.getLine(), token.getColumn(),
                        String.format(MULTILINE_BLOCK, token.getColumn(),
                                LogFormat.formatLineNumber(token.getLine()),
                                LogFormat.formatLineNumber(token.getEndLine())));
            }
        }
    }

    /** Indica si el "/" en la posición dada va pegado inmediatamente a "/", "*" o "=". */
    private static boolean isGluedToMarker(List<Token> code, int index) {
        if (index + 1 >= code.size()) {
            return false;
        }
        Token slash = code.get(index);
        Token next = code.get(index + 1);
        return next.getLine() == slash.getLine() && next.getColumn() == slash.getColumn() + 1
                && NOT_A_MARKER_AFTER_SLASH.indexOf(next.getLexeme().charAt(0)) >= 0;
    }

    /** Indica si el token anterior inicia una sentencia (o si no hay token anterior). */
    private static boolean startsStatement(Token previous) {
        return previous == null || STATEMENT_STARTERS.contains(previous.getType());
    }

    /** Indica si el token es un comentario, incluido uno sin cerrar. */
    private static boolean isComment(Token token) {
        return token.getCategory() == TokenCategory.COMMENT
                || token.is(TokenType.UNTERMINATED_COMMENT);
    }

    /** Devuelve +1 por ( o [, -1 por ) o ] y 0 para los demás tokens. */
    private static int depthChange(Token token) {
        if (token.is(TokenType.LPAREN) || token.is(TokenType.LBRACKET)) {
            return 1;
        }
        return token.is(TokenType.RPAREN) || token.is(TokenType.RBRACKET) ? -1 : 0;
    }
}
