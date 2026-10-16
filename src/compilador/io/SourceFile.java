package compilador.io;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Archivo PHP cargado en memoria sin alterar su formato (texto sin BOM, líneas, charset y BOM); entrada de la Etapa 1 y de la bitácora. */
public final class SourceFile {

    // Ruta, charset de lectura, presencia de BOM, texto decodificado y lista de líneas.
    private final Path path;
    private final Charset charset;
    private final boolean hadBom;
    private final String rawText;
    private final List<SourceLine> lines;

    /** Crea el archivo cargado; guarda una copia no modificable de las líneas (siempre al menos una). */
    public SourceFile(Path path, Charset charset, boolean hadBom, String rawText,
            List<SourceLine> lines) {
        this.path = path;
        this.charset = charset;
        this.hadBom = hadBom;
        this.rawText = rawText;
        this.lines = Collections.unmodifiableList(new ArrayList<SourceLine>(lines));
    }

    /** Devuelve la ubicación del archivo. */
    public Path getPath() {
        return path;
    }

    /** Devuelve el charset con que se decodificó; la bitácora se escribe con el mismo. */
    public Charset getCharset() {
        return charset;
    }

    /** Indica si el archivo comenzaba con la marca BOM de UTF-8. */
    public boolean hadBom() {
        return hadBom;
    }

    /** Devuelve el texto decodificado sin BOM; equivale a concatenar contenido y terminador de cada línea. */
    public String getRawText() {
        return rawText;
    }

    /** Devuelve la lista de líneas, en orden y de solo lectura. */
    public List<SourceLine> getLines() {
        return lines;
    }

    /** Devuelve la cantidad de líneas. */
    public int getLineCount() {
        return lines.size();
    }

    /** Devuelve la última línea del archivo. */
    public SourceLine getLastLine() {
        return lines.get(lines.size() - 1);
    }

    /** Devuelve el separador para líneas añadidas a la bitácora: el primer terminador del archivo o el del sistema (PRD §4). */
    public String lineSeparatorForAppending() {
        for (SourceLine line : lines) {
            if (line.hasTerminator()) {
                return line.getTerminator();
            }
        }
        return System.lineSeparator();
    }
}
