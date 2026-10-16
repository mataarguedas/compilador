package compilador.lexer.pattern;

import compilador.lexer.CharacterStream;
import compilador.lexer.CharacterStream.Mark;
import compilador.lexer.TokenType;

/** Reconoce la etiqueta de apertura bien o mal formada (<?php, <?PHP, <? php, <?=, <?); Etapa 3. Regex equivalente: <\?(=|[ \t]*[pP]([hH][pP]?)?)? */
public final class OpenTagPattern implements LexemePattern {

    // Inicio de la etiqueta, marca de "echo corto" y letras esperadas.
    private static final String TAG_START = "<?";
    private static final char SHORT_ECHO = '=';
    private static final String TAG_LETTERS = "php";

    /** Puede empezar si el texto actual comienza con "<?". */
    @Override
    public boolean canStart(CharacterStream stream) {
        return stream.lookingAt(TAG_START);
    }

    /** Consume "<?" y luego "=" o espacios con letras de "php"; toda variante es OPEN_TAG y el validador reporta las incorrectas. */
    @Override
    public PatternMatch match(CharacterStream stream) {
        Mark start = stream.mark();
        stream.advance();
        stream.advance();
        if (stream.peek() == SHORT_ECHO) {
            stream.advance();
        } else {
            matchSpacedLetters(stream);
        }
        return new PatternMatch(TokenType.OPEN_TAG, stream.textSince(start));
    }

    /** Consume [ \t]* seguido de un prefijo de "php" sin importar mayúsculas; si no hay letras, no consume nada. */
    private static void matchSpacedLetters(CharacterStream stream) {
        Mark afterTagStart = stream.mark();
        while (stream.peek() == ' ' || stream.peek() == '\t') {
            stream.advance();
        }
        int letters = 0;
        while (letters < TAG_LETTERS.length() && matchesIgnoreCase(stream.peek(),
                TAG_LETTERS.charAt(letters))) {
            stream.advance();
            letters++;
        }
        // Los espacios solo forman parte de la etiqueta si les siguen letras.
        if (letters == 0) {
            stream.reset(afterTagStart);
        }
    }

    /** Compara un carácter con la letra esperada en minúscula o mayúscula. */
    private static boolean matchesIgnoreCase(int actual, char expectedLower) {
        return actual == expectedLower || actual == Character.toUpperCase(expectedLower);
    }
}
