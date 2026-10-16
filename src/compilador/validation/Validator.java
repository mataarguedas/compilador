package compilador.validation;

import compilador.errors.ErrorCollector;

/** Grupo de reglas léxicas del PRD §5 (etiquetas, identificadores, comentarios, lexemas); Etapa 4: validación de lexemas. */
public interface Validator {

    /** Revisa las reglas del validador y registra cada violación; solo puede modificar la marca "reported" de los tokens. */
    void validate(ValidationContext context, ErrorCollector errors);
}
