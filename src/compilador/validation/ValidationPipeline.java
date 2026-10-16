package compilador.validation;

import compilador.errors.ErrorCollector;
import compilador.io.SourceFile;
import compilador.lexer.Token;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ejecuta todos los validadores en el orden fijo del PRD §6; el orden importa por la marca "reported"; Etapa 4. */
public final class ValidationPipeline {

    // Validadores en el orden en que se ejecutan.
    private final List<Validator> validators;

    /** Registra los validadores: apertura, cierre, identificadores, comentarios y lexemas restantes. */
    public ValidationPipeline() {
        List<Validator> ordered = new ArrayList<Validator>();
        ordered.add(new OpeningTagValidator());
        ordered.add(new ClosingTagValidator());
        ordered.add(new IdentifierValidator());
        ordered.add(new CommentValidator());
        ordered.add(new LexemeValidator());
        this.validators = Collections.unmodifiableList(ordered);
    }

    /** Valida un archivo con todos los validadores y devuelve el colector con todos los errores. */
    public ErrorCollector validate(SourceFile source, List<Token> tokens) {
        ValidationContext context = new ValidationContext(source, tokens);
        ErrorCollector errors = new ErrorCollector();
        for (Validator validator : validators) {
            validator.validate(context, errors);
        }
        return errors;
    }
}
