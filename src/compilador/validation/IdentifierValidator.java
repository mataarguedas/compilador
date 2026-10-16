package compilador.validation;

import compilador.errors.ErrorCode;
import compilador.errors.ErrorCollector;
import compilador.lexer.ReservedWords;
import compilador.lexer.Token;
import compilador.lexer.pattern.AsciiChars;

/** Revisa el identificador a la izquierda de cada "=", códigos 200–205, en orden 205, 203, 200, 201, 202, 204 (PRD §5.3); Etapa 4. */
public final class IdentifierValidator implements Validator {

    // Signo con el que deben empezar las variables.
    private static final String DOLLAR = "$";

    // Plantillas de descripción del PRD §5.3.
    private static final String EMPTY_TARGET = "Assignment operator '=' at column %d has no"
            + " identifier on its left side.";
    private static final String MISSING_DOLLAR = "Invalid identifier '%s': the assignment target"
            + " does not start with '$'; variables must start with the dollar sign.";
    private static final String STARTS_WITH_DIGIT = "Invalid identifier '%s': it starts with the"
            + " digit '%s' after '$'; an identifier must start with a letter (a-z, A-Z) or"
            + " underscore (_).";
    private static final String INVALID_START = "Invalid identifier '%s': character '%s' after"
            + " '$' is not allowed; an identifier must start with a letter (a-z, A-Z) or"
            + " underscore (_).";
    private static final String DOLLAR_ONLY = "Invalid identifier '$': the dollar sign is not"
            + " followed by a name.";
    private static final String INVALID_CHARACTER = "Invalid identifier '%s': character '%s' at"
            + " position %d is not allowed; only letters, digits (0-9) and underscores (_) may"
            + " follow the first character.";
    private static final String RESERVED_WORD = "Invalid identifier '%s': '%s' is a reserved"
            + " word and cannot be used as an identifier.";

    // Extrae los destinos de asignación a validar.
    private final AssignmentTargetExtractor extractor = new AssignmentTargetExtractor();

    /** Valida cada destino y, si generó errores, marca sus tokens como reportados (así ñ en $ñame no da además un 401). */
    @Override
    public void validate(ValidationContext context, ErrorCollector errors) {
        for (AssignmentTarget target : extractor.extract(context.getTokens())) {
            int before = errors.count();
            check(target, errors);
            if (errors.count() > before) {
                for (Token token : target.getTokens()) {
                    token.markReported();
                }
            }
        }
    }

    /** Aplica las reglas a la base del destino ($notas en $notas[$i]); tras const no se exige $. */
    private static void check(AssignmentTarget target, ErrorCollector errors) {
        String base = target.getBase();
        // 205: no hay identificador a la izquierda del "=".
        if (base.isEmpty()) {
            Token assign = target.getAssign();
            errors.add(ErrorCode.IDENT_EMPTY_TARGET, assign.getLine(), assign.getColumn(),
                    String.format(EMPTY_TARGET, assign.getColumn()));
            return;
        }
        int nameStart = 0;
        if (!target.isConstant()) {
            // 203: la variable no empieza con $.
            if (!base.startsWith(DOLLAR)) {
                report(errors, target, ErrorCode.IDENT_MISSING_DOLLAR, 0,
                        String.format(MISSING_DOLLAR, LineText.excerpt(base)));
                return;
            }
            // 201: el $ no va seguido de ningún nombre.
            if (base.length() == DOLLAR.length()) {
                report(errors, target, ErrorCode.IDENT_INVALID_START, 0, DOLLAR_ONLY);
                return;
            }
            nameStart = DOLLAR.length();
        }
        checkFirstCharacter(target, base, nameStart, errors);
        checkLaterCharacters(target, base, nameStart, errors);
        // 204: el nombre coincide exactamente (con mayúsculas) con una palabra reservada.
        String name = base.substring(nameStart);
        if (ReservedWords.isReservedIdentifierName(name)) {
            report(errors, target, ErrorCode.IDENT_RESERVED_WORD, 0,
                    String.format(RESERVED_WORD, LineText.excerpt(base), name));
        }
    }

    /** Reporta 200 si el nombre empieza con dígito, o 201 si empieza con otro carácter no válido. */
    private static void checkFirstCharacter(AssignmentTarget target, String base, int index,
            ErrorCollector errors) {
        int first = base.codePointAt(index);
        String shown = new String(Character.toChars(first));
        if (AsciiChars.isDigit(first)) {
            report(errors, target, ErrorCode.IDENT_STARTS_WITH_DIGIT, index,
                    String.format(STARTS_WITH_DIGIT, LineText.excerpt(base), shown));
        } else if (!AsciiChars.isNameStart(first)) {
            report(errors, target, ErrorCode.IDENT_INVALID_START, index,
                    String.format(INVALID_START, LineText.excerpt(base), shown));
        }
    }

    /** Reporta 202 una vez por cada carácter posterior al primero que no sea letra, dígito o guion bajo. */
    private static void checkLaterCharacters(AssignmentTarget target, String base,
            int nameStart, ErrorCollector errors) {
        int index = nameStart + Character.charCount(base.codePointAt(nameStart));
        while (index < base.length()) {
            int c = base.codePointAt(index);
            if (!AsciiChars.isNameChar(c)) {
                // Posición desde 1 dentro del texto crudo, contando puntos de código.
                int position = base.codePointCount(0, index) + 1;
                report(errors, target, ErrorCode.IDENT_INVALID_CHARACTER, index,
                        String.format(INVALID_CHARACTER, LineText.excerpt(base),
                                new String(Character.toChars(c)), position));
            }
            index += Character.charCount(c);
        }
    }

    /** Registra el error en la línea del destino con la columna del carácter problemático. */
    private static void report(ErrorCollector errors, AssignmentTarget target, ErrorCode code,
            int offset, String description) {
        errors.add(code, target.getLine(), target.getColumn() + offset, description);
    }
}
