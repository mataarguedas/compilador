# CLAUDE.md — PHP Lexical Analyzer

You are working on a **university compiler project**: a lexical analyzer for PHP, written in plain Java. **PRD.md is the source of truth**. Read it before any task, and follow its error catalog (§5) and message templates word for word.

## Project facts
- NetBeans **Java with Ant → Java Application**, already created. Do not convert it to Maven or Gradle, and do not rewrite `build.xml`. Its only customization is the `-post-jar` target, which copies `codigo.php` and `INFORME.txt` from the root into `dist/`.
- The main class is whatever `main.class` says in `nbproject/project.properties`. Keep that class as the entry point. It must only call `new Application().run(args)` and `System.exit(...)`. All other code lives in sub-packages of the main class's package (called `{base}` in PRD.md).
- Make sure `nbproject/project.properties` has `source.encoding=UTF-8`.
- The build output is `dist/<ProjectName>.jar`. `ant clean` **deletes `dist/`**; the `-post-jar` target copies the root `codigo.php` and `INFORME.txt` back in. Any other `.php` must be copied by hand.

## Non-negotiable rules
1. **JDK only.** Do not use JFlex, JavaCC, ANTLR, JLex, SableCC, Spoon, JUnit, Apache Commons, Guava, or any jar in `lib/`. Only `java.*` imports are allowed.
2. **No `java.util.regex`** anywhere in `lexer/`, `lexer/pattern/` or `validation/`. Patterns are hand-written character automata, and each one's Javadoc states its equivalent regex.
3. **Java 8 compatible code.** Do not use `var`, records, text blocks, switch expressions, `String.repeat`, `isBlank`, `strip`, `List.of`, `Map.of` or `Files.readString`.
4. **The source is never altered.** Keep each line's original terminator (`\r\n`, `\n`, `\r` or none). The log copy is `String.format("%04d", n) + LogFormat.NUMBER_SEPARATOR + content + terminator`.
5. **Error lines are exactly** `Error <code>. Line <NNNN>. <description>`. Use only the templates in PRD §5. Never write generic messages such as "Syntax error".
6. **Report every problem.** Several errors on one line are separate entries. Sort errors by line, then column, then code. Deduplicate using the token `reported` flag.
7. **One responsibility per class; methods preferably under ~40 lines.** The four lexical stages (acquisition, classification, pattern determination, validation) must stay in their own packages (PRD §6).
8. **No hard-coded file names or paths.** Files are found relative to the jar location (PRD §3.1).

## Code style
- Code identifiers, comments and Javadoc are in English. `INFORME.txt` is in Spanish.
- Every public class has a Javadoc stating **which lexical stage** it belongs to.
- Prefer small immutable value objects (`Token`, `SourceLine`, `LexicalError`) with `final` fields.
- Put magic strings and characters in constants: reserved words, operators, the log separator.
- Do not print to the console from anywhere except `report/ConsoleReporter` and `app/Application`.

## Commands (Windows CMD, from the project root)
```
ant clean jar                                   :: or NetBeans "Clean and Build"
java -jar dist\<ProjectName>.jar                :: -post-jar already put codigo.php in dist
java -jar dist\<ProjectName>.jar --tokens       :: optional token dump
type dist\codigo-errors.log
```
Running the fixtures (no JUnit):
```
javac -encoding UTF-8 -d build\fixtures -cp build\classes test\{base-path}\FixtureRunner.java
java -cp build\classes;build\fixtures {base}.FixtureRunner test-fixtures
```

## Definition of done for any change
- `ant clean jar` succeeds with no warnings introduced by your change.
- `FixtureRunner` passes all fixtures. This includes the byte-identity check and the invariant that concatenating every token lexeme equals the source text.
- The provided `codigo.php` (no trailing newline) produces `No lexical errors found.`
- `grep -rn "java.util.regex\|^import org\.\|^import com\." src` returns nothing.

## Gotchas
- A **compliant file has no newline after `?>`**. The log writer must add a separator before the error section when the last line has no terminator. Many editors, NetBeans included, add a final newline; that correctly triggers error 114.
- `?->` is the nullsafe operator, not a closing tag.
- `//` and `#` inside strings are not comments. `?>` inside a comment is ignored (a deliberate deviation from real PHP; PRD §5.4).
- `$` + `ñ` is invalid: identifiers are ASCII-only.
- The division `/` in `$a / $b` must not trigger error 302. Only a `/` at the start of a statement triggers it.
- Keep whitespace and newline tokens in the token list. Target extraction and comment checks depend on adjacency.
