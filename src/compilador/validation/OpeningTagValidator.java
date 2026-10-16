package compilador.validation;

import compilador.errors.ErrorCode;
import compilador.errors.ErrorCollector;
import compilador.io.LogFormat;
import compilador.io.SourceLine;
import compilador.lexer.Token;
import compilador.lexer.TokenType;
import java.util.List;

/** Revisa las reglas de la etiqueta de apertura, códigos 100–106 (nada antes, exacta, espacio después, única, sin BOM); Etapa 4. */
public final class OpeningTagValidator implements Validator {

    /** La única etiqueta de apertura bien formada. */
    public static final String EXPECTED_TAG = "<?php";

    // Plantillas de descripción del PRD §5.1.
    private static final String TEXT_BEFORE_LINE = "Text '%s' found before the opening tag"
            + " '<?php' (line %s); the opening tag must be the first characters of the file.";
    private static final String TEXT_BEFORE_SAME_LINE = "Text '%s' found before the opening"
            + " tag '<?php' on the same line; the opening tag must be the first characters of"
            + " the file.";
    private static final String BLANK_LINE_BEFORE = "Blank line found before the opening tag"
            + " '<?php' (line %s); nothing may precede the opening tag.";
    private static final String WHITESPACE_BEFORE = "%d whitespace character(s) found before"
            + " the opening tag '<?php'; nothing may precede the opening tag.";
    private static final String MALFORMED = "Malformed opening tag '%s'; it must be written"
            + " exactly as '<?php'.";
    private static final String MISSING = "Opening tag '<?php' not found; the file must start"
            + " with '<?php'.";
    private static final String NO_SPACE_AFTER = "Opening tag '<?php' is immediately followed"
            + " by '%s'; a space or line break is required after it.";
    private static final String ADDITIONAL = "Additional opening tag '%s' found at column %d;"
            + " only one opening tag is allowed, at the start of the file.";
    private static final String BOM = "Invisible UTF-8 byte order mark (BOM) found before"
            + " '<?php'; save the file as UTF-8 without BOM.";

    // Columna 0 = error de toda la línea; la línea 1 es donde se reportan 103 y 106.
    private static final int WHOLE_LINE = 0;
    private static final int FIRST_LINE = 1;

    /** Reporta BOM (106) y ausencia (103); el primer OPEN_TAG es "la" etiqueta y cada uno posterior es adicional (105). */
    @Override
    public void validate(ValidationContext context, ErrorCollector errors) {
        if (context.getSource().hadBom()) {
            errors.add(ErrorCode.OPEN_BOM, FIRST_LINE, WHOLE_LINE, BOM);
        }
        List<Token> openTags = context.tokensOfType(TokenType.OPEN_TAG);
        if (openTags.isEmpty()) {
            errors.add(ErrorCode.OPEN_MISSING, FIRST_LINE, WHOLE_LINE, MISSING);
            return;
        }
        Token tag = openTags.get(0);
        checkLinesBefore(context, tag, errors);
        checkSameLinePrefix(context, tag, errors);
        checkLexeme(context, tag, errors);
        // Toda etiqueta de apertura después de la primera es un error 105.
        for (int i = 1; i < openTags.size(); i++) {
            Token extra = openTags.get(i);
            errors.add(ErrorCode.OPEN_ADDITIONAL, extra.getLine(), extra.getColumn(),
                    String.format(ADDITIONAL, LineText.excerpt(extra.getLexeme()),
                            extra.getColumn()));
        }
    }

    /** Reporta 101 por cada línea en blanco o 100 por cada línea con texto antes de la línea de la etiqueta. */
    private static void checkLinesBefore(ValidationContext context, Token tag,
            ErrorCollector errors) {
        for (int number = 1; number < tag.getLine(); number++) {
            String content = context.getLine(number).getContent();
            String lineLabel = LogFormat.formatLineNumber(number);
            if (LineText.isBlank(content)) {
                errors.add(ErrorCode.OPEN_BLANK_BEFORE, number, WHOLE_LINE,
                        String.format(BLANK_LINE_BEFORE, lineLabel));
            } else {
                errors.add(ErrorCode.OPEN_TEXT_BEFORE, number, WHOLE_LINE,
                        String.format(TEXT_BEFORE_LINE,
                                LineText.excerpt(LineText.trim(content)), lineLabel));
            }
        }
    }

    /** Reporta 101 si solo hay espacios, o 100 si hay texto, antes de la etiqueta en su misma línea. */
    private static void checkSameLinePrefix(ValidationContext context, Token tag,
            ErrorCollector errors) {
        String prefix = context.getLine(tag.getLine()).getContent()
                .substring(0, tag.getColumn() - 1);
        if (prefix.isEmpty()) {
            return;
        }
        if (LineText.isBlank(prefix)) {
            errors.add(ErrorCode.OPEN_BLANK_BEFORE, tag.getLine(), FIRST_LINE,
                    String.format(WHITESPACE_BEFORE, prefix.length()));
        } else {
            errors.add(ErrorCode.OPEN_TEXT_BEFORE, tag.getLine(), FIRST_LINE,
                    String.format(TEXT_BEFORE_SAME_LINE,
                            LineText.excerpt(LineText.trim(prefix))));
        }
    }

    /** Reporta 102 si la etiqueta no es exactamente "<?php", o 104 si está pegada al carácter siguiente. */
    private static void checkLexeme(ValidationContext context, Token tag,
            ErrorCollector errors) {
        if (!EXPECTED_TAG.equals(tag.getLexeme())) {
            errors.add(ErrorCode.OPEN_MALFORMED, tag.getLine(), tag.getColumn(),
                    String.format(MALFORMED, LineText.excerpt(tag.getLexeme())));
            return;
        }
        SourceLine line = context.getLine(tag.getLine());
        int after = tag.getColumn() - 1 + EXPECTED_TAG.length();
        // Si después de "<?php" hay un carácter que no es espacio ni tabulación, es error 104.
        if (after < line.getContent().length()
                && !LineText.isSpaceOrTab(line.getContent().charAt(after))) {
            int codePoint = line.getContent().codePointAt(after);
            errors.add(ErrorCode.OPEN_NO_SPACE_AFTER, tag.getLine(), after + 1,
                    String.format(NO_SPACE_AFTER, new String(Character.toChars(codePoint))));
        }
    }
}
