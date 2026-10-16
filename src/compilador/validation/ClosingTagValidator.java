package compilador.validation;

import compilador.errors.ErrorCode;
import compilador.errors.ErrorCollector;
import compilador.io.LogFormat;
import compilador.io.SourceLine;
import compilador.lexer.Token;
import compilador.lexer.TokenCategory;
import compilador.lexer.TokenType;
import java.util.List;

/** Revisa las reglas de la etiqueta de cierre, códigos 110–116 (debe terminar el archivo, exacta, sin nada después y única); Etapa 4. */
public final class ClosingTagValidator implements Validator {

    /** La etiqueta de cierre. */
    public static final String CLOSE_TAG = "?>";

    // Plantillas de descripción del PRD §5.2.
    private static final String MISSING = "Closing tag '?>' not found; the file must end with"
            + " '?>'.";
    private static final String MISSING_COMMENT_HINT = " (the '?>' inside the comment on line"
            + " %s is ignored).";
    private static final String MALFORMED = "Malformed closing tag '%s'; it must be written"
            + " exactly as '?>'.";
    private static final String CONTENT_AFTER = "Content '%s' found after the closing tag '?>'"
            + " (line %s); the closing tag must be on the last line of the file.";
    private static final String WHITESPACE_AFTER = "%d whitespace character(s) found after the"
            + " closing tag '?>'; nothing may follow it.";
    private static final String LINE_BREAKS_AFTER = "%d line break(s) found after the closing"
            + " tag '?>'; the file must end immediately after '?>'.";
    private static final String TEXT_AFTER = "Text '%s' found after the closing tag '?>' on the"
            + " same line; nothing may follow it.";
    private static final String NOT_LAST = "Closing tag '?>' at column %d is not the last one in"
            + " the file; the closing tag may appear only on the last line.";

    // Columna 0 = toda la línea; caracteres de una etiqueta "? >" mal escrita.
    private static final int WHOLE_LINE = 0;
    private static final char QUESTION_MARK = '?';
    private static final char GREATER_THAN = '>';

    /** El último CLOSE_TAG es "la" etiqueta de cierre; los anteriores se reportan como 116. */
    @Override
    public void validate(ValidationContext context, ErrorCollector errors) {
        List<Token> closeTags = context.tokensOfType(TokenType.CLOSE_TAG);
        if (closeTags.isEmpty()) {
            reportMissing(context, errors);
            return;
        }
        for (int i = 0; i < closeTags.size() - 1; i++) {
            Token earlier = closeTags.get(i);
            errors.add(ErrorCode.CLOSE_NOT_LAST, earlier.getLine(), earlier.getColumn(),
                    String.format(NOT_LAST, earlier.getColumn()));
        }
        Token tag = closeTags.get(closeTags.size() - 1);
        checkRestOfLine(context, tag, errors);
        checkLineBreaksAfter(context, tag, errors);
        checkLinesAfter(context, tag, errors);
    }

    /** Reporta 111 si la última línea no vacía parece "? >"; si no, 110 (mencionando un ?> dentro de comentario si lo hay). */
    private static void reportMissing(ValidationContext context, ErrorCollector errors) {
        int lastNonBlank = lastNonBlankLine(context);
        if (lastNonBlank > 0) {
            String text = LineText.trim(context.getLine(lastNonBlank).getContent());
            if (isSpacedCloseTag(text)) {
                errors.add(ErrorCode.CLOSE_MALFORMED, lastNonBlank, WHOLE_LINE,
                        String.format(MALFORMED, LineText.excerpt(text)));
                return;
            }
        }
        String description = MISSING;
        int commentLine = lineOfCloseTagInComment(context);
        if (commentLine > 0) {
            description += String.format(MISSING_COMMENT_HINT,
                    LogFormat.formatLineNumber(commentLine));
        }
        errors.add(ErrorCode.CLOSE_MISSING, context.getLineCount(), WHOLE_LINE, description);
    }

    /** Reporta 113 por espacios o 115 por otro texto después de la etiqueta en su misma línea. */
    private static void checkRestOfLine(ValidationContext context, Token tag,
            ErrorCollector errors) {
        int after = tag.getColumn() - 1 + CLOSE_TAG.length();
        String rest = context.getLine(tag.getLine()).getContent().substring(after);
        if (rest.isEmpty()) {
            return;
        }
        if (LineText.isBlank(rest)) {
            errors.add(ErrorCode.CLOSE_TRAILING_WHITESPACE, tag.getLine(), after + 1,
                    String.format(WHITESPACE_AFTER, rest.length()));
        } else {
            errors.add(ErrorCode.CLOSE_TEXT_AFTER, tag.getLine(), after + 1,
                    String.format(TEXT_AFTER, LineText.excerpt(LineText.trim(rest))));
        }
    }

    /** Reporta 114 una sola vez en la línea de la etiqueta, contando todos los saltos de línea hasta el final. */
    private static void checkLineBreaksAfter(ValidationContext context, Token tag,
            ErrorCollector errors) {
        int lineBreaks = 0;
        for (int number = tag.getLine(); number <= context.getLineCount(); number++) {
            if (context.getLine(number).hasTerminator()) {
                lineBreaks++;
            }
        }
        if (lineBreaks > 0) {
            int column = tag.getColumn() + CLOSE_TAG.length();
            errors.add(ErrorCode.CLOSE_TRAILING_LINE_BREAKS, tag.getLine(), column,
                    String.format(LINE_BREAKS_AFTER, lineBreaks));
        }
    }

    /** Reporta 112 por cada línea posterior a la etiqueta que tenga contenido no blanco. */
    private static void checkLinesAfter(ValidationContext context, Token tag,
            ErrorCollector errors) {
        for (int number = tag.getLine() + 1; number <= context.getLineCount(); number++) {
            SourceLine line = context.getLine(number);
            if (!LineText.isBlank(line.getContent())) {
                errors.add(ErrorCode.CLOSE_CONTENT_AFTER, number, WHOLE_LINE,
                        String.format(CONTENT_AFTER,
                                LineText.excerpt(LineText.trim(line.getContent())),
                                LogFormat.formatLineNumber(number)));
            }
        }
    }

    /** Indica si el texto es "?", uno o más espacios/tabulaciones y luego ">". */
    private static boolean isSpacedCloseTag(String text) {
        if (text.length() < CLOSE_TAG.length() + 1 || text.charAt(0) != QUESTION_MARK
                || text.charAt(text.length() - 1) != GREATER_THAN) {
            return false;
        }
        return LineText.isBlank(text.substring(1, text.length() - 1));
    }

    /** Devuelve la última línea con contenido no blanco, o 0 si todas están en blanco. */
    private static int lastNonBlankLine(ValidationContext context) {
        for (int number = context.getLineCount(); number >= 1; number--) {
            if (!LineText.isBlank(context.getLine(number).getContent())) {
                return number;
            }
        }
        return 0;
    }

    /** Devuelve la línea del último "?>" encontrado dentro de un comentario, o 0 si no hay. */
    private static int lineOfCloseTagInComment(ValidationContext context) {
        int line = 0;
        for (Token token : context.getTokens()) {
            int index = token.getCategory() == TokenCategory.COMMENT
                    || token.is(TokenType.UNTERMINATED_COMMENT)
                    ? token.getLexeme().lastIndexOf(CLOSE_TAG) : -1;
            if (index >= 0) {
                line = LineText.lineOfIndex(token, index);
            }
        }
        return line;
    }
}
