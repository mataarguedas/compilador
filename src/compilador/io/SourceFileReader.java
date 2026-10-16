package compilador.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Lee un PHP byte a byte y lo convierte en SourceFile sin alterar su formato (PRD §3.2); prepara la entrada de la Etapa 1. */
public final class SourceFileReader {

    // Bytes de la marca BOM de UTF-8, caracteres de fin de línea y terminador vacío.
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final char CR = '\r';
    private static final char LF = '\n';
    private static final String CRLF = "\r\n";
    private static final String NO_TERMINATOR = "";

    /** Lee y decodifica el archivo: UTF-8 estricto sin BOM o, si falla, ISO-8859-1. */
    public SourceFile read(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        boolean hasBom = startsWithBom(bytes);
        int offset = hasBom ? UTF8_BOM.length : 0;
        String text = decodeStrictUtf8(bytes, offset);
        Charset charset = StandardCharsets.UTF_8;
        // Si no es UTF-8 válido, los primeros bytes tampoco eran un BOM: se usa ISO-8859-1.
        if (text == null) {
            hasBom = false;
            text = new String(bytes, StandardCharsets.ISO_8859_1);
            charset = StandardCharsets.ISO_8859_1;
        }
        return new SourceFile(path, charset, hasBom, text, splitLines(text));
    }

    /** Indica si los bytes comienzan con la marca BOM de UTF-8 (EF BB BF). */
    private static boolean startsWithBom(byte[] bytes) {
        if (bytes.length < UTF8_BOM.length) {
            return false;
        }
        for (int i = 0; i < UTF8_BOM.length; i++) {
            if (bytes[i] != UTF8_BOM[i]) {
                return false;
            }
        }
        return true;
    }

    /** Decodifica como UTF-8 estricto desde el desplazamiento dado; devuelve null si los bytes no son UTF-8 válido. */
    private static String decodeStrictUtf8(byte[] bytes, int offset) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(bytes, offset, bytes.length - offset))
                    .toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    /** Divide el texto en líneas con su terminador original; texto vacío da una línea vacía y el último terminador no crea línea extra. */
    static List<SourceLine> splitLines(String text) {
        List<SourceLine> lines = new ArrayList<SourceLine>();
        int start = 0;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            // Avanza hasta encontrar un \r o \n.
            if (c != CR && c != LF) {
                i++;
                continue;
            }
            String terminator = terminatorAt(text, i);
            lines.add(new SourceLine(lines.size() + 1, text.substring(start, i), terminator));
            i += terminator.length();
            start = i;
        }
        // Agrega la última línea sin terminador, o una línea vacía si el texto estaba vacío.
        if (start < text.length() || lines.isEmpty()) {
            lines.add(new SourceLine(lines.size() + 1, text.substring(start), NO_TERMINATOR));
        }
        return lines;
    }

    /** Devuelve el terminador que empieza en la posición dada: "\r\n", "\r" o "\n". */
    private static String terminatorAt(String text, int index) {
        if (text.charAt(index) == CR) {
            boolean followedByLf = index + 1 < text.length() && text.charAt(index + 1) == LF;
            return followedByLf ? CRLF : String.valueOf(CR);
        }
        return String.valueOf(LF);
    }
}
