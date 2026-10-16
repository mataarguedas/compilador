# PRD — PHP Lexical Analyzer (Java, Ant, no third-party libraries)

## 1. Summary

A command-line Java application, packaged as an executable `.jar`, that implements the **lexical-analysis stage of a compiler for PHP**. It performs four steps:

1. It locates and reads a `.php` file that sits in the same folder as the `.jar`, keeping the file's exact formatting.
2. It tokenizes the file using hand-written automata, with no generator tools and no regex engine.
3. It validates **opening/closing tags**, **identifiers** and **inline comments**.
4. It writes `<filename>-errors.log`. This file holds a line-numbered, byte-faithful copy of the source, followed by a list of specific errors.

It also delivers the Spanish user documentation `INFORME.txt`.

## 2. Hard constraints

| # | Constraint |
|---|------------|
| C1 | NetBeans project type **Java with Ant → Java Application** (already created). Build with the default `build.xml`; the jar is produced in `dist/`. |
| C2 | **No external or third-party libraries.** This excludes JFlex, JavaCC, ANTLR, JLex, SableCC, Spoon, JUnit, Apache Commons, and anything else outside the JDK. |
| C3 | **No `java.util.regex` in the lexer or validators.** Every pattern is a hand-written, character-by-character recognizer (explicit DFA). This makes the *pattern determination (equivalent logic)* stage visible. |
| C4 | Code must compile on **Java 8+**. Do not use `var`, records, text blocks, switch expressions, `String.repeat`, `String.isBlank` or `List.of`. |
| C5 | Runs from CMD with `java -jar <Project>.jar` and needs no arguments or configuration. |
| C6 | Object-oriented and modular. Each lexical stage has its own classes (see §6). |

## 3. Input handling (Requirement 1 — File Reading)

### 3.1 Locating the file
- **Base directory:** the directory that contains the running `.jar`. Resolve it with `Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()`. If that location is a directory (for example `build/classes` when running from NetBeans), fall back to `System.getProperty("user.dir")`. Paths with spaces or accented characters must work, which is why the `toURI()` form is used.
- **No fixed name, path or configuration.** The program lists every regular file in the base directory whose name ends with exactly `.php`. This check is case-sensitive: `x.PHP`, `x.php.txt` and `x.phtml` are rejected.
- Each accepted file is processed in alphabetical order and produces its own log.
- If the base directory contains no `.php` file, the program also checks `user.dir` (when different). If neither has one, it prints a clear console message and exits with code `1`.
- **Optional argument:** `java -jar X.jar some.php`. If the argument does not end with exactly `.php`, the program prints `ERROR: '<name>' does not have the required .php extension; the file was not processed.`, creates no log, and exits with `1`. A relative argument is resolved against the base directory first and then against `user.dir`.
- Rejected candidate files (wrong extension) are listed on the console as ignored, with the reason.

### 3.2 Reading without altering format
- Read the **raw bytes** (`Files.readAllBytes`).
- Decode as UTF-8 using a strict `CharsetDecoder` (`CodingErrorAction.REPORT`). If decoding fails, fall back to `ISO-8859-1`. Remember which charset was used, because the log is written in the same one.
- Detect a UTF-8 **BOM** (`EF BB BF`). Record its presence (error 106), strip it from the text, and **do not** reproduce it in the log. It is an encoding marker, not a visible character.
- Split into `SourceLine` objects `{number, content, terminator}`. The terminator is one of `"\r\n"`, `"\n"`, `"\r"` or `""` (only possible on the last line). Nothing is trimmed, tabs are kept, and blank lines are kept.
- An empty file becomes one `SourceLine` with number 1, empty content and an empty terminator.
- A file whose last character is a line terminator produces **no** extra phantom line. The final terminator belongs to the last real line. For example, `"?>\n"` is one line `?>` with terminator `\n`.

## 4. Output: the error log (Requirement 2)

- **Name:** `<basename>-errors.log`, where `<basename>` is the file name without the final `.php`. For example, `codigo.php` produces `codigo-errors.log`. The log is written in the **same folder as the `.php` file** and overwrites any existing log.
- **Numbered copy:** for every `SourceLine`, write `NNNN` + `" "` + `content` + `terminator`. `NNNN` is the line number zero-padded to 4 digits (width grows automatically past 9999 lines). The single space separator is defined in one constant, `LogFormat.NUMBER_SEPARATOR`, so it can be changed to `""` if the instructor requires digits glued to the text. The content between the prefix and the terminator is **byte-identical** to the source.
- **Error section:**
  1. If the last source line has an empty terminator (the normal case for a compliant file), first write one line separator so errors do not join the last line.
  2. Write one empty line.
  3. Write every error, one per line, sorted by line, then column, then code:
     `Error <code>. Line <NNNN>. <specific description>`
  4. If there are no errors, write the single line `No lexical errors found.`
- **Line separator for added lines:** use the first terminator found in the file. If the file has none, use `System.lineSeparator()`.
- **Encoding:** the same charset that was used to read the file.

**Example** (valid `codigo.php`, no trailing newline):
```
0001 <?php
0002 // Sistema simple de gestión de notas
0003 
...
0053 ?>

No lexical errors found.
```

## 5. Lexical rules and error catalog

The error format is always `Error <code>. Line <NNNN>. <description>`. Descriptions must name the offending lexeme or character, and give its column when useful. Generic messages are forbidden. Several errors on the same line are each reported separately. Placeholders in `{braces}`; `{col}` is 1-based. When a quoted text is longer than 40 characters, truncate it to 40 and append `...`.

### 5.1 Opening tag `<?php` (codes 100–106)

**Lexer behaviour.** The lexer starts in INLINE mode. The first `<?` it meets begins an `OPEN_TAG` token. The token's lexeme is `<?`, plus any spaces or tabs, plus up to 3 letters (case-insensitive `php`). Examples: `<?php`, `<?PHP`, `<? php`, `<?`, `<?=`. The lexer switches to PHP mode in every case, for error recovery. If the file contains **no** `<?` at all, the whole file is lexed in PHP mode from line 1 (error 103), so that the other validations still run.

| Code | When | Description template |
|------|------|----------------------|
| 100 | A line before the tag's line contains text | `Text '{text}' found before the opening tag '<?php' (line {NNNN}); the opening tag must be the first characters of the file.` |
| 100 | The tag's own line has text before the tag | `Text '{prefix}' found before the opening tag '<?php' on the same line; the opening tag must be the first characters of the file.` |
| 101 | A line before the tag is blank or whitespace-only | `Blank line found before the opening tag '<?php' (line {NNNN}); nothing may precede the opening tag.` |
| 101 | Only whitespace precedes the tag on its line | `{n} whitespace character(s) found before the opening tag '<?php'; nothing may precede the opening tag.` |
| 102 | The tag lexeme is not exactly `<?php` | `Malformed opening tag '{lexeme}'; it must be written exactly as '<?php'.` |
| 103 | The file contains no `<?` | `Opening tag '<?php' not found; the file must start with '<?php'.` (reported on line 1) |
| 104 | Exact `<?php` is immediately followed by a non-whitespace character | `Opening tag '<?php' is immediately followed by '{c}'; a space or line break is required after it.` |
| 105 | A second opening tag appears | `Additional opening tag '{lexeme}' found at column {col}; only one opening tag is allowed, at the start of the file.` |
| 106 | UTF-8 BOM present | `Invisible UTF-8 byte order mark (BOM) found before '<?php'; save the file as UTF-8 without BOM.` (line 1) |

### 5.2 Closing tag `?>` (codes 110–116)

**Lexer behaviour.** In PHP mode, outside strings and comments, `?>` produces a `CLOSE_TAG` token and switches to INLINE mode. `?->` is the nullsafe operator and is **not** a closing tag. In INLINE mode, the text up to the next `<?` becomes `INLINE_TEXT`. If that later `<?` exists, error 105 is reported for it and error 116 for the earlier `?>`. The **last** `CLOSE_TAG` in the file is "the" closing tag.

| Code | When | Description template |
|------|------|----------------------|
| 110 | No `CLOSE_TAG` exists | `Closing tag '?>' not found; the file must end with '?>'.` (reported on the last line). If a `?>` was seen inside a comment, append ` (the '?>' inside the comment on line {NNNN} is ignored).` |
| 111 | No `CLOSE_TAG`, and the last non-blank line trimmed is `?` + spaces/tabs + `>` | `Malformed closing tag '{text}'; it must be written exactly as '?>'.` (reported instead of 110) |
| 112 | A later line after the closing tag has non-whitespace content | `Content '{text}' found after the closing tag '?>' (line {NNNN}); the closing tag must be on the last line of the file.` (one error per such line) |
| 113 | Only spaces/tabs follow `?>` on its line | `{n} whitespace character(s) found after the closing tag '?>'; nothing may follow it.` |
| 114 | The `?>` line has a terminator; blank or whitespace-only lines after it are included in the count | `{n} line break(s) found after the closing tag '?>'; the file must end immediately after '?>'.` (one error, on the `?>` line) |
| 115 | Other text follows `?>` on the same line | `Text '{text}' found after the closing tag '?>' on the same line; nothing may follow it.` |
| 116 | A `?>` that is not the last closing tag | `Closing tag '?>' at column {col} is not the last one in the file; the closing tag may appear only on the last line.` |

Code before `?>` on the last line (for example `echo 1; ?>`) is allowed.

### 5.3 Identifiers (codes 200–205)

Identifier validation runs **only on the target to the left of the assignment operator `=`**. That means the `ASSIGN` token only. `==`, `===`, `!=`, `<=`, `>=`, `=>`, `+=` and `.=` are different tokens and do not trigger it.

**Extracting the target.**
1. From each `ASSIGN` token, walk backwards on the same line. Skip whitespace first.
2. Collect consecutive tokens with **no whitespace between them**. Stop at whitespace, the start of the line, a comment, `OPEN_TAG`, or one of the boundary delimiters `;` `,` `(` `)` `{` `}`.
3. The concatenated lexemes form the **raw target**. For example, `$-data` is the tokens `$`, `-`, `data`.
4. **Base extraction:** if the raw target contains `[`, `->` or `::`, validate only the part before the first of them. For example, `$notas[$i]` is validated as `$notas`.

**Skips.**
- If the token immediately before `=` is `)` or `]` (destructuring), skip validation.
- If the significant token before the raw target is the keyword `const`, validate the raw target as a **constant name**. It needs no `$`: the first character must be a letter or `_`, and the rest must be letters, digits or `_`. Otherwise the same codes apply.

**Rules, checked in order.** Every applicable rule reports its own error.

| Code | Rule | Description template |
|------|------|----------------------|
| 205 | Raw target is empty | `Assignment operator '=' at column {col} has no identifier on its left side.` |
| 203 | Does not start with `$` (non-const) | `Invalid identifier '{raw}': the assignment target does not start with '$'; variables must start with the dollar sign.` |
| 200 | Character after `$` is a digit | `Invalid identifier '{raw}': it starts with the digit '{d}' after '$'; an identifier must start with a letter (a-z, A-Z) or underscore (_).` |
| 201 | Character after `$` is some other invalid character | `Invalid identifier '{raw}': character '{c}' after '$' is not allowed; an identifier must start with a letter (a-z, A-Z) or underscore (_).` |
| 201 | Nothing follows `$` | `Invalid identifier '$': the dollar sign is not followed by a name.` |
| 202 | Each invalid character after the first name character | `Invalid identifier '{raw}': character '{c}' at position {p} is not allowed; only letters, digits (0-9) and underscores (_) may follow the first character.` |
| 204 | Name after `$` matches a reserved word | `Invalid identifier '{raw}': '{word}' is a reserved word and cannot be used as an identifier.` |

Notes on the rules:
- Error 202 is reported once per invalid character occurrence. `{p}` is the 1-based position inside `{raw}`.
- **Valid characters are ASCII only:** `a-z`, `A-Z`, `0-9` and `_`. A character such as `ñ` is invalid.
- **Reserved words (20):** `class function if while echo else continue for return public null false true trim foreach private include print const do`. The assignment writes `Include`; this is treated as `include`.
- **Case sensitivity (design decision):** identifiers are case-sensitive. Therefore:
  - The reserved-word check on `$name` is an **exact, case-sensitive** match. `$if` is an error; `$If` is not.
  - The `SymbolTable` keys identifiers by their exact text, so `$total` and `$Total` are two different entries.
  - Classification of **bare words** as `KEYWORD` is case-insensitive, because PHP keywords are (`ECHO` is a keyword).
  - Both behaviours are controlled by constants in `ReservedWords`.
- The console prints the symbol table after analysis to show that `$total` and `$Total` are distinct.

### 5.4 Comments (codes 300–304)

- `//` or `#` (outside strings) starts an `INLINE_COMMENT` that runs to the end of the line, excluding the terminator. Its content produces **no tokens**. Following the assignment's rule that content is "completely ignored", this includes `?>`. This deviates from real PHP and is documented in INFORME.txt.
- `/* ... */` produces a `BLOCK_COMMENT` token. It is allowed only on a single line.
- `//` or `#` inside a string literal is part of the string, not a comment.

**Orphan text line.** A line is an orphan text line when all of these hold:
- the parenthesis/bracket depth at its start is 0;
- the previous significant, non-comment token is `;`, `{`, `}` or `OPEN_TAG` (or there is none);
- its significant tokens are only bare words, numbers or unknown characters, with at least one bare word;
- its first token is a bare word that is not a keyword and is not followed by `(` or `::`;
- it contains no `;`.

| Code | When | Description template |
|------|------|----------------------|
| 300 | Orphan text line directly after a line ending in an `INLINE_COMMENT` | `Text '{text}' appears to continue the inline comment of line {NNNN}; inline comments end at the end of the line and cannot span multiple lines (start this line with // or #).` |
| 301 | Orphan text line anywhere else | `Text '{text}' is not valid PHP code and is not marked as a comment; inline comments must start with // or #.` |
| 302 | A `/` token that starts the line or follows `;`, `{` or `}` on the same line, and is not followed by `/`, `*` or `=` | `Malformed comment marker '/' at column {col}; inline comments must start exactly with '//' or '#'.` |
| 303 | A `BLOCK_COMMENT` spans more than one line | `Comment starting at column {col} spans lines {NNNN} to {MMMM}; comments must not extend across multiple lines (use // or # on each line).` |
| 304 | A `/*` is never closed | `Comment '/*' opened at column {col} is never closed and runs to the end of the file.` |

### 5.5 Other lexeme validation (codes 400–402)

| Code | When | Description template |
|------|------|----------------------|
| 400 | A string reaches the end of the file without its closing quote | `String literal opened with {quote} at column {col} is never closed.` |
| 401 | A character that matches no pattern | `Unrecognized character '{c}' (U+{hex}) at column {col}.` |
| 402 | Digits immediately followed by letters or `_` (for example `12abc`) | `Malformed numeric literal '{lexeme}' at column {col}; a number cannot be followed directly by letters.` |

- **Recovery for 400:** end the string at the end of its starting line and resume lexing on the next line. This prevents the unterminated string from swallowing `?>`.
- **Deduplication:** tokens that already produced an identifier error are marked `reported` and are skipped by the lexeme validator. For example, `ñ` in `$ñame =` yields only error 201, not an extra 401.

## 6. Architecture (OOP, one package per lexical stage)

Here `{base}` is the package of the existing NetBeans main class; read it from `main.class` in `nbproject/project.properties`.

```
{base}/
  Main (existing)                 → delegates to app.Application; no logic here
  app/        Application, CliOptions, ExitCode
  io/         PhpFileLocator, SourceFileReader, SourceFile, SourceLine, ErrorLogWriter, LogFormat
  lexer/      [Stage 1 – TOKEN ACQUISITION]    CharacterStream, Lexer, LexerMode
              [Stage 2 – TOKEN CLASSIFICATION] Token, TokenType, TokenCategory, TokenClassifier, ReservedWords
  lexer/pattern/  [Stage 3 – PATTERN DETERMINATION]
              LexemePattern (interface), PatternMatch, PatternRegistry,
              OpenTagPattern, CloseTagPattern, NewlinePattern, WhitespacePattern,
              InlineCommentPattern, BlockCommentPattern, StringPattern,
              VariablePattern, NumberPattern, WordPattern, OperatorPattern
  validation/ [Stage 4 – LEXEME VALIDATION]
              Validator (interface), ValidationContext, AssignmentTargetExtractor, AssignmentTarget,
              OpeningTagValidator, ClosingTagValidator, IdentifierValidator,
              CommentValidator, LexemeValidator
  symbols/    SymbolTable, SymbolEntry
  errors/     ErrorCode (enum: code + category), LexicalError (Comparable), ErrorCollector
  report/     ConsoleReporter
test/{base}/  FixtureRunner (plain main, no JUnit)
test-fixtures/ *.php + expected/*.log
```

Each stage is explained below.

- **CharacterStream** keeps position, line, column and mode. It offers `peek(k)`, `advance()`, `mark()`/`reset()` and `isAtEnd()`, and works on the BOM-stripped text.
- **LexemePattern** has this shape:
  ```java
  boolean canStart(CharacterStream s);
  PatternMatch match(CharacterStream s); // consumes chars, returns lexeme + type + flags
  ```
  Every pattern documents in its Javadoc the regular expression it is **equivalent** to. The more complex patterns (`VariablePattern`, `NumberPattern`, `StringPattern`, `BlockCommentPattern`) implement an explicit `enum State` transition loop.
- **Lexer.** In PHP mode it tries patterns in this order: newline, whitespace, close tag, inline comment, block comment, string, variable, number, word, operator, and finally `UNKNOWN`. The operator pattern uses **longest match** over the full PHP operator/punctuator table, for example `===` before `==` before `=`. In INLINE mode it recognises only `OPEN_TAG`; everything else is `INLINE_TEXT`.
- **TokenType** covers these types:
  - tags: `OPEN_TAG`, `CLOSE_TAG`, `INLINE_TEXT`
  - words: `VARIABLE`, `IDENTIFIER`, `KEYWORD`
  - literals: `INTEGER_LITERAL`, `FLOAT_LITERAL`, `STRING_LITERAL`
  - comments: `INLINE_COMMENT`, `BLOCK_COMMENT`
  - operators: `ASSIGN`, `COMPARISON_OPERATOR`, `ARITHMETIC_OPERATOR`, `LOGICAL_OPERATOR`, `CONCAT_OPERATOR`, `OTHER_OPERATOR`
  - delimiters: `SEMICOLON`, `COMMA`, `LPAREN`, `RPAREN`, `LBRACE`, `RBRACE`, `LBRACKET`, `RBRACKET`
  - layout: `WHITESPACE`, `NEWLINE`
  - problems: `UNKNOWN`, `MALFORMED_NUMBER`, `UNTERMINATED_STRING`, `UNTERMINATED_COMMENT`

  Each type maps to a **TokenCategory**: `TAG`, `KEYWORD`, `IDENTIFIER`, `LITERAL`, `OPERATOR`, `DELIMITER`, `COMMENT`, `LAYOUT`, `INLINE_TEXT` or `ERROR`.
- **Token** holds type, lexeme, line, column, endLine and a `reported` flag. Whitespace and newline tokens are kept, because the validators need adjacency information. Concatenating all lexemes reproduces the text exactly; this is an invariant that is tested.
- **Validators run in this fixed order:** Opening, Closing, Identifier, Comment, Lexeme. Each receives a `ValidationContext` (source + tokens) and an `ErrorCollector`.
- **Application pipeline:** locate, read, lex, build the symbol table, validate, write the log, report to the console.

## 7. Console output

```
PHP Lexical Analyzer
File      : C:\curso\dist\codigo.php
Lines     : 53   Tokens: 3xx   Errors: 0
Log       : C:\curso\dist\codigo-errors.log
Identifiers (case-sensitive symbol table):
  $nombre_estudiante   first line 0005   occurrences 1
  ...
  $total               first line 0047   occurrences 2
  $Total               first line 0048   occurrences 2
```

- Pass `-t` / `--tokens` to also print the token table (line, column, type, category, lexeme with escapes shown). This flag is optional and never required.
- **Exit codes:** `0` if at least one file was analyzed (lexical errors do not count as a failure); `1` if no file was found, the extension was wrong, or an I/O error occurred.

## 8. Test fixtures (`test-fixtures/`, plus `expected/<name>-errors.log`)

| Fixture | Purpose | Expected codes |
|---------|---------|----------------|
| `valid_codigo.php` | Provided file with no trailing newline | none |
| `valid_codigo_crlf.php` | Same file with CRLF endings; log must keep CRLF | none |
| `identifiers.php` | See §8.1 | 200, 201, 202, 203, 204 |
| `open_leading_ws.php` | `  <?php` | 101 |
| `open_content_before.php` | `echo "";` / blank line / `<?php` | 100, 101 |
| `open_malformed.php` | `<?PHP` | 102 |
| `open_missing.php` | No tag | 103 |
| `close_missing.php` | No `?>` | 110 |
| `close_trailing_newline.php` | Ends `?>\n\n` | 114 (n=2) |
| `close_trailing_space.php` | `?>` + 2 spaces | 113 |
| `close_content_after.php` | `?>` / `echo "hello";` | 114, 112 |
| `comments.php` | See §8.2 | 300, 301, 302, 303 |
| `bom.php` | BOM + valid code | 106 |
| `empty.php` | 0 bytes | 103, 110 |

### 8.1 `identifiers.php` (no trailing newline) and its exact expected error section

```
<?php
$variable = 1;
$_data = 2;
$name1 = 3;
$1variable = 4;
$-data = 5;
$if = 10;
$else = true;
$1a = 1; $for = 2;
$If = 3;
$na@me = 6;
total = 7;
const MAX = 10;
$notas[0] = 5;
$x == 5;
?>
```
```
Error 200. Line 0005. Invalid identifier '$1variable': it starts with the digit '1' after '$'; an identifier must start with a letter (a-z, A-Z) or underscore (_).
Error 201. Line 0006. Invalid identifier '$-data': character '-' after '$' is not allowed; an identifier must start with a letter (a-z, A-Z) or underscore (_).
Error 204. Line 0007. Invalid identifier '$if': 'if' is a reserved word and cannot be used as an identifier.
Error 204. Line 0008. Invalid identifier '$else': 'else' is a reserved word and cannot be used as an identifier.
Error 200. Line 0009. Invalid identifier '$1a': it starts with the digit '1' after '$'; an identifier must start with a letter (a-z, A-Z) or underscore (_).
Error 204. Line 0009. Invalid identifier '$for': 'for' is a reserved word and cannot be used as an identifier.
Error 202. Line 0011. Invalid identifier '$na@me': character '@' at position 4 is not allowed; only letters, digits (0-9) and underscores (_) may follow the first character.
Error 203. Line 0012. Invalid identifier 'total': the assignment target does not start with '$'; variables must start with the dollar sign.
```

### 8.2 `comments.php` (no trailing newline)

```
<?php
$var = 10; // if while echo
# $echo another valid comment
$url = "http://example.com # not a comment";
$a = 1; // comment
continues here
$b = 2;
stray words
$c = 3; / bad comment
/* block
comment */
$d = 4; /* ok on one line */
?>
```
Expected errors: 300 on line 0006, 301 on line 0008, 302 on line 0009, 303 on line 0010 (spans 0010 to 0011). The tokens `if`, `while`, `echo` and `$echo` must not appear in the token dump.

## 9. Acceptance criteria / traceability

| Requirement | Verified by |
|-------------|-------------|
| Reads only the exact `.php` extension; rejects others | `PhpFileLocator` + manual CMD test with `x.txt` / `x.PHP` |
| Keeps all whitespace and blank lines | Byte-comparison in `FixtureRunner`: strip the prefix from each log line, then compare with the source |
| Any file name, no fixed path | Rename fixture, move the jar folder (including a path with spaces), re-run |
| `<name>-errors.log` beside the input | `FixtureRunner` |
| 4-digit prefix + identical copy | `FixtureRunner` + `valid_codigo*.php` |
| Specific errors at the end, one per problem, several per line | `identifiers.php` line 0009 |
| Identifier start, case sensitivity, reserved words | §8.1 + symbol table shows `$total` ≠ `$Total` |
| Opening/closing tag rules | `open_*` / `close_*` fixtures |
| Comment recognition, delimitation, independence | `comments.php` + token dump |
| OOP stages visible | Package layout §6 |
| No third-party libs, no regex in lexer | `grep -r "java.util.regex\|import org\.\|import com\." src` returns nothing |
| Runs from CMD | `cd dist && java -jar <Project>.jar` |
| `INFORME.txt` in Spanish | File at project root and copied into `dist/` |

## 10. Out of scope

- Heredoc/nowdoc.
- Interpolation inside double-quoted strings. The string is a single token.
- Syntax and semantic analysis (for example, detecting undeclared variables).
- Multiple PHP blocks interleaved with HTML (reported through 105/116 only).
- Unicode identifiers.
