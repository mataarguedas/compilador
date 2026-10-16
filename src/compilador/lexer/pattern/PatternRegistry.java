package compilador.lexer.pattern;

import compilador.lexer.CharacterStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Conjunto ordenado de patrones del modo PHP más el de apertura del modo INLINE (PRD §6); Etapa 3: determinación de patrones. */
public final class PatternRegistry {

    // Patrón de "<?" (único del modo INLINE) y lista ordenada de patrones del modo PHP.
    private final LexemePattern openTagPattern = new OpenTagPattern();
    private final List<LexemePattern> phpPatterns;

    /** Registra los patrones PHP en el orden en que se prueban; el primero que acepta la posición gana. */
    public PatternRegistry() {
        List<LexemePattern> patterns = new ArrayList<LexemePattern>();
        patterns.add(new NewlinePattern());
        patterns.add(new WhitespacePattern());
        patterns.add(new CloseTagPattern());
        patterns.add(new InlineCommentPattern());
        patterns.add(new BlockCommentPattern());
        patterns.add(new StringPattern());
        patterns.add(new VariablePattern());
        patterns.add(new NumberPattern());
        patterns.add(new WordPattern());
        patterns.add(new OperatorPattern());
        this.phpPatterns = Collections.unmodifiableList(patterns);
    }

    /** Devuelve el primer patrón PHP que puede empezar en la posición actual, o null (el léxico emitirá UNKNOWN). */
    public LexemePattern findPhpPattern(CharacterStream stream) {
        for (LexemePattern pattern : phpPatterns) {
            if (pattern.canStart(stream)) {
                return pattern;
            }
        }
        return null;
    }

    /** Devuelve el patrón de "<?", el único que se reconoce en modo INLINE. */
    public LexemePattern getOpenTagPattern() {
        return openTagPattern;
    }
}
