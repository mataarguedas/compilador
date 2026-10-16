# PROMPTS.md — Sequential build plan (10 prompts)

Run these prompts one at a time, in order, in Claude Code from the project root. Each one depends on the previous one. Do not start the next prompt until the current prompt's **Verify** step passes. Every prompt assumes `CLAUDE.md` and `PRD.md` are in the project root.

---

## Prompt 1 — Recon, skeleton and error model - DONE

```
Read CLAUDE.md and PRD.md completely. Then inspect build.xml, nbproject/project.properties
and the existing main class (from main.class) and tell me the base package ({base}) and the
jar name that `ant jar` produces. Ensure source.encoding=UTF-8.

Create the package skeleton from PRD §6 (empty packages are fine except the ones below) and
implement the errors package:
- errors/ErrorCode: enum with every code from PRD §5 (100–106, 110–116, 200–205, 300–304,
  400–402). Each constant has an int code and a category (OPENING_TAG, CLOSING_TAG, IDENTIFIER,
  COMMENT, LEXEME).
- errors/LexicalError: immutable (ErrorCode, line, column, description); Comparable by line,
  column, code; method toLogLine() returning exactly "Error 200. Line 0005. <description>"
  (line padded to at least 4 digits).
- errors/ErrorCollector: add(...), getSorted(), count(), isEmpty().
- app/ExitCode constants, and app/Application with an empty run(String[]) returning 0.
Make the existing main class delegate to Application. Java 8 syntax only.
```
**Verify:** `ant clean jar` succeeds, and `java -jar dist\<Project>.jar` exits with code 0.

---

## Prompt 2 — Locating and reading the .php file - DONE

```
Implement PRD §3 in the io and app packages:
- io/PhpFileLocator: resolves the base directory from the jar's CodeSource via toURI()
  (fallback to user.dir when it is a directory), lists regular files ending exactly in ".php"
  (case-sensitive), sorted; reports ignored near-misses (.PHP, .php.txt, .phtml) with a reason;
  resolves an optional CLI argument and rejects a wrong extension.
- io/SourceLine (number, content, terminator) and io/SourceFile (path, charset, hadBom,
  rawText without BOM, List<SourceLine>, lineSeparatorForAppending()).
- io/SourceFileReader: readAllBytes, strict UTF-8 decode with ISO-8859-1 fallback, BOM
  detection, line splitting that keeps \r\n, \n, \r and no phantom last line; empty file = one
  empty line.
- app/CliOptions: optional file argument and -t/--tokens flag.
Wire Application to locate and read files and print "File/Lines" per file; handle "no .php
found" and "wrong extension" with the exact messages from PRD §3.1 and exit code 1.
```
**Verify:** Put `codigo.php` next to the jar and confirm the correct line count (53). Rename it to `codigo.txt`: you should get the "not found" message with it listed as ignored. Run `java -jar X.jar codigo.txt`: you should get the extension error. Repeat from a folder whose path contains spaces.

---

## Prompt 3 — Error log writer (numbered, byte-faithful copy) - DONE

```
Implement io/LogFormat (NUMBER_SEPARATOR = " ", pad width rule) and io/ErrorLogWriter per
PRD §4: file name <basename>-errors.log next to the source, numbered copy preserving each
line's own terminator, the separator rule when the last line has no terminator, one empty
line, then sorted errors or "No lexical errors found.". Write with the source charset.
Wire it into Application (no analysis yet, so every log says no errors).
Then create test-fixtures/valid_codigo.php (the provided codigo.php WITHOUT a trailing newline,
UTF-8 without BOM) and valid_codigo_crlf.php (same, CRLF).
```
**Verify:** For both fixtures, removing the first 5 characters of every copy line gives the source back **byte for byte**; check this with a small throwaway Java snippet or `fc /b`. The CRLF log keeps CRLF.

---

## Prompt 4 — Token acquisition: CharacterStream, Token model, lexer modes, tags - DONE

```
Implement Stage 1 and the Stage 2 model:
- lexer/CharacterStream (peek(k), advance, mark/reset, line/column tracking where \r\n counts
  as one line break, isAtEnd).
- lexer/TokenType, lexer/TokenCategory (mapping from PRD §6), lexer/Token (type, lexeme, line,
  column, endLine, reported flag).
- lexer/LexerMode {INLINE, PHP} and lexer/Lexer with the pattern dispatch loop over a
  PatternRegistry.
- lexer/pattern/LexemePattern, PatternMatch, PatternRegistry, and the first patterns:
  OpenTagPattern (lexeme rules and malformed variants from PRD §5.1), CloseTagPattern (not
  "?->"), NewlinePattern, WhitespacePattern. Anything else in PHP mode temporarily becomes
  UNKNOWN (one char). INLINE mode produces INLINE_TEXT up to the next "<?".
- If the file has no "<?" at all, lex everything in PHP mode (PRD §5.1 recovery).
Add the invariant check: concatenation of all token lexemes == SourceFile.rawText (assert in
a helper used later by FixtureRunner). Implement the --tokens dump in report/ConsoleReporter.
```
**Verify:** Run `--tokens` on `valid_codigo.php`. You should see OPEN_TAG on line 1, CLOSE_TAG on line 53, and the invariant holding.

---

## Prompt 5 — Pattern determination: all remaining patterns + classification - DONE

```
Implement the remaining Stage 3 patterns, each with Javadoc stating its equivalent regex and,
for Variable/Number/String/BlockComment, an explicit enum State transition loop (no regex):
- InlineCommentPattern (// and # to end of line, terminator excluded; content never tokenized).
- BlockCommentPattern (/* ... */, may span lines -> endLine; unterminated -> UNTERMINATED_COMMENT
  to EOF).
- StringPattern (' " and `, escapes; unterminated -> UNTERMINATED_STRING ending at the end of
  its starting line, as recovery per PRD §5.5).
- VariablePattern ($ followed by [A-Za-z0-9_]*; a lone "$" is a VARIABLE with empty name).
- NumberPattern (int, float, .5, exponent, 0x, 0b, underscores; digits followed by letters/_ ->
  MALFORMED_NUMBER).
- WordPattern ([A-Za-z_][A-Za-z0-9_]*, ASCII) and OperatorPattern (longest match over the full
  PHP operator/punctuator table; '=' alone -> ASSIGN).
Implement Stage 2: lexer/ReservedWords (the 20 words; exact-case set for $identifiers,
case-insensitive lookup for bare words, both as documented constants) and
lexer/TokenClassifier (WORD -> KEYWORD/IDENTIFIER, operator lexeme -> operator TokenType).
Pattern order per PRD §6.
```
**Verify:** Run `--tokens` on `valid_codigo.php`: there should be no UNKNOWN tokens, `/` on line 22 should be ARITHMETIC_OPERATOR, and the comment contents should produce no tokens. Write a temporary file containing `"http://x # y"` and confirm it is a single STRING_LITERAL. The lexeme invariant must still hold.

---

## Prompt 6 — Validation stage part 1: opening and closing tags - DONE

```
Implement validation/Validator, validation/ValidationContext and:
- OpeningTagValidator: codes 100–106 exactly as PRD §5.1 (per-line 100/101 for lines before
  the tag, same-line prefix, malformed lexeme, missing tag, missing whitespace after <?php,
  additional opening tags, BOM).
- ClosingTagValidator: codes 110–116 exactly as PRD §5.2 (last CLOSE_TAG is "the" tag;
  whitespace vs text on the same line; line-break count including blank/whitespace-only lines;
  one 112 per content line after; 116 for earlier close tags; 111 for "? >"; the comment hint
  on 110 when "?>" appeared inside a comment).
Add validation to the Application pipeline (validators in PRD order) and write errors to the log.
Create fixtures open_leading_ws, open_content_before, open_malformed, open_missing,
close_missing, close_trailing_newline, close_trailing_space, close_content_after, bom, empty.
```
**Verify:** Each fixture produces exactly the codes listed in PRD §8, and `valid_codigo.php` still produces no errors.

---

## Prompt 7 — Validation stage part 2: identifiers + symbol table - DONE

```
Implement:
- validation/AssignmentTarget and validation/AssignmentTargetExtractor exactly as PRD §5.3
  (walk back from each ASSIGN on the same line, adjacency without whitespace, boundary
  delimiters, base cut at '[', '->', '::', skip destructuring, detect preceding 'const').
- validation/IdentifierValidator: rules 205, 203, 200, 201, 202 (one per invalid char,
  1-based position), 204 (case-sensitive exact match). Constant names after 'const' use the
  no-$ rule. Mark involved tokens reported=true.
- symbols/SymbolTable and SymbolEntry: exact-case keys, first line, occurrence count, from
  VARIABLE tokens in PHP mode; printed by ConsoleReporter.
Create test-fixtures/identifiers.php and expected/identifiers-errors.log from PRD §8.1 (the
error section must match character for character).
```
**Verify:** The `identifiers.php` log matches PRD §8.1 exactly. For `valid_codigo.php`, the symbol table lists `$total` and `$Total` as separate entries, and `$i` in the `for` header produces no error.

---

## Prompt 8 — Validation stage part 3: comments and remaining lexemes - DONE

```
Implement:
- validation/CommentValidator: 300/301 using the orphan-text-line definition in PRD §5.4
  (depth 0, statement start, only bare words/numbers/unknown with >=1 non-keyword first word not
  followed by '(' or '::', no ';'), 302 for a statement-initial '/', 303 multi-line block
  comments, 304 unterminated.
- validation/LexemeValidator: 400, 401 (with U+hex), 402; skip tokens already reported.
Create test-fixtures/comments.php from PRD §8.2 with its expected log. Confirm with --tokens
that no token is produced from comment contents ("if", "while", "echo", "$echo").
```
**Verify:** `comments.php` produces exactly 300 (0006), 301 (0008), 302 (0009) and 303 (0010). `valid_codigo.php` still has zero errors, and in particular line 22's division is not flagged.

---

## Prompt 9 — Fixture runner, edge cases and hardening - DONE

```
Create test/{base}/FixtureRunner.java (plain main, NO JUnit) that, for every *.php in the
given folder: runs the analysis pipeline programmatically into a temp dir, checks (1) the log
equals expected/<name>-errors.log byte for byte, (2) every copy line minus its prefix equals the
source line and terminators are identical, (3) the lexeme-concatenation invariant, and prints
PASS/FAIL per fixture with a diff of the first mismatching line; exits 1 on any failure.
Generate the expected logs for fixtures created in prompts 6–8, then review each one manually
against PRD §5 templates before accepting it.
Harden: files >9999 lines (padding widens), tabs, trailing spaces on lines, ISO-8859-1 file,
two .php files in the folder (two logs), read-only folder (clear console error, exit 1),
path with spaces. Review every class for single responsibility, Java 8 compliance, Javadoc
stating the lexical stage, and run the grep checks from CLAUDE.md.
```
**Verify:** Running FixtureRunner prints PASS for every fixture, and the grep checks return nothing.

---

## Prompt 10 — INFORME.txt (Spanish) and final delivery check

```
Write INFORME.txt in Spanish (UTF-8, plain text, max ~100 columns), at the project root, with:
1. Portada: proyecto, curso, integrantes (placeholders), fecha.
2. Descripción del analizador léxico y alcance.
3. Requisitos: JDK 8 o superior, java en el PATH (cómo comprobar con "java -version").
4. Estructura del proyecto y arquitectura POO: explicar las 4 etapas (obtención de tokens,
   clasificación, determinación de patrones con su expresión regular equivalente, validación
   de lexemas) indicando los paquetes y clases de cada una.
5. Compilación: NetBeans "Clean and Build" y alternativa "ant clean jar"; ubicación del .jar.
6. Ejecución paso a paso desde CMD: abrir CMD, cd a la carpeta dist, copiar el .php junto al
   .jar, "java -jar <Proyecto>.jar", argumento opcional y opción --tokens, códigos de salida.
7. Archivo de salida: nombre <nombre>-errors.log, formato de numeración, sección de errores,
   ejemplo real de la ejecución de codigo.php y de identifiers.php.
8. Catálogo completo de errores (códigos 100–402) con descripción y ejemplo.
9. Decisiones de diseño: sensibilidad a mayúsculas en palabras reservadas, "Include" tratado
   como "include", ?> dentro de comentarios ignorado, separador de un espacio configurable,
   BOM, recuperación de cadenas sin cerrar, sin librerías externas ni expresiones regulares.
10. Solución de problemas: "java no se reconoce", tildes en consola (chcp 65001), el editor
    agrega salto de línea final tras ?> (error 114), "ant clean" borra dist.
11. Limitaciones (PRD §10).
Then: ant clean jar, copy INFORME.txt and test-fixtures/valid_codigo.php (as codigo.php) into
dist, run from CMD, paste the real console output and log into section 7, and walk through the
PRD §9 traceability table, reporting each item as met/not met with evidence.
```
**Verify:** Every item in PRD §9 is met, the jar runs from a fresh CMD window, and `INFORME.txt` sits beside the jar.
