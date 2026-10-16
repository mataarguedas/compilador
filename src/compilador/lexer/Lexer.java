package compilador.lexer;

import compilador.lexer.CharacterStream.Mark;
import compilador.lexer.pattern.LexemePattern;
import compilador.lexer.pattern.PatternMatch;
import compilador.lexer.pattern.PatternRegistry;
import java.util.ArrayList;
import java.util.List;

/** Divide el texto fuente en tokens alternando modos INLINE y PHP sin descartar ningún carácter; Etapa 1: adquisición de tokens. */
public final class Lexer {

    // Inicio de la etiqueta de apertura de PHP.
    private static final String OPEN_TAG_START = "<?";

    // Patrones de la Etapa 3 y clasificador de la Etapa 2.
    private final PatternRegistry registry = new PatternRegistry();
    private final TokenClassifier classifier = new TokenClassifier();

    /** Tokeniza el texto (sin BOM) y devuelve todos los tokens en orden, incluidos espacios y saltos de línea. */
    public List<Token> tokenize(String text) {
        // Si no hay ningún "<?", todo se analiza en modo PHP para que las demás validaciones corran (error 103).
        LexerMode initialMode = text.contains(OPEN_TAG_START) ? LexerMode.INLINE : LexerMode.PHP;
        CharacterStream stream = new CharacterStream(text, initialMode);
        List<Token> tokens = new ArrayList<Token>();
        while (!stream.isAtEnd()) {
            tokens.add(nextToken(stream));
        }
        return tokens;
    }

    /** Reconoce el siguiente lexema según el modo, lo clasifica, actualiza el modo y crea el token. */
    private Token nextToken(CharacterStream stream) {
        Mark start = stream.mark();
        PatternMatch match = stream.getMode() == LexerMode.INLINE
                ? matchInline(stream) : matchPhp(stream);
        TokenType type = classifier.classify(match.getType(), match.getLexeme());
        updateMode(stream, type);
        return new Token(type, match.getLexeme(), start.getLine(),
                start.getColumn(), stream.getLastConsumedLine());
    }

    /** En modo INLINE reconoce OPEN_TAG o consume texto hasta el siguiente "<?" como INLINE_TEXT. */
    private PatternMatch matchInline(CharacterStream stream) {
        LexemePattern openTag = registry.getOpenTagPattern();
        if (openTag.canStart(stream)) {
            return openTag.match(stream);
        }
        Mark start = stream.mark();
        while (!stream.isAtEnd() && !openTag.canStart(stream)) {
            stream.advance();
        }
        return new PatternMatch(TokenType.INLINE_TEXT, stream.textSince(start));
    }

    /** En modo PHP prueba los patrones en orden; si ninguno aplica, produce un token UNKNOWN. */
    private PatternMatch matchPhp(CharacterStream stream) {
        LexemePattern pattern = registry.findPhpPattern(stream);
        if (pattern != null) {
            return pattern.match(stream);
        }
        return matchUnknown(stream);
    }

    /** Consume un punto de código completo (ambas mitades de un par sustituto) como UNKNOWN. */
    private static PatternMatch matchUnknown(CharacterStream stream) {
        Mark start = stream.mark();
        char first = stream.advance();
        if (Character.isHighSurrogate(first) && !stream.isAtEnd()
                && Character.isLowSurrogate((char) stream.peek())) {
            stream.advance();
        }
        return new PatternMatch(TokenType.UNKNOWN, stream.textSince(start));
    }

    /** OPEN_TAG cambia a modo PHP y CLOSE_TAG regresa a modo INLINE. */
    private static void updateMode(CharacterStream stream, TokenType type) {
        if (type == TokenType.OPEN_TAG) {
            stream.setMode(LexerMode.PHP);
        } else if (type == TokenType.CLOSE_TAG) {
            stream.setMode(LexerMode.INLINE);
        }
    }
}
