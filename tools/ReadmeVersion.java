import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;

import static java.lang.System.err;

public static final String COMMENT_START = "<!--";
public static final String COMMENT_END = "-->";
public static final String REPLACE_START = "VERSION-REPLACE";
public static final String REPLACE_END = "/VERSION-REPLACE";

void main(final String... args) throws IOException {
    if (args.length == 0 || Set.of("help", "--help", "-h", "?", "/?", "-?").contains(args[0].toLowerCase(Locale.ROOT))) {
        err.print("""
            ReadmeVersion - script for replacing <!-- VERSION-REPLACE --> comments in README.md
            
            Usage: java ReadmeVersion.java <new version> [target file]
            
            Examples (Bourne shell):
              java ReadmeVersion.java 27.1.1 README.md
              java ReadmeVersion.java 26.1.2
              java ReadmeVersion.java "this is a version" ../OtherProject/README.md
            """);
        return;
    }

    final String newVersion = args[0];
    final Path target = Path.of("").relativize(Path.of(args.length > 1 ? args[1] : "README.md"));
    err.printf("Replacing blocks with %s in %s%n", newVersion, target);

    final String file = Files.readString(target);
    final StringWriter writer = new StringWriter();
    final Deque<String> replacementStack = new ArrayDeque<>();

    final Function<String, String> replacer = sub -> {
        String result = sub;
        for (final String toReplace : replacementStack) result = result.replace(toReplace, newVersion);
        return result;
    };

    int lineNumber = 1, readCursor = -1, writeCursor = 0;
    while ((readCursor = file.indexOf(COMMENT_START, readCursor + 1)) != -1) {
        final String toWrite = file.substring(writeCursor, readCursor);
        lineNumber += toWrite.split("\n", -1).length - 1;

        writer.write(toWrite.transform(replacer));
        writeCursor = readCursor;

        final int commentEnd = file.indexOf(COMMENT_END, readCursor);
        if (commentEnd == -1) break;

        final int replaceEnder = file.indexOf(REPLACE_END, readCursor, commentEnd);
        if (replaceEnder != -1) {
            replacementStack.pop();

            err.printf("  ..   ending replacement block on line %d%n", lineNumber);
            continue;
        }

        final int replaceStarter = file.indexOf(REPLACE_START, readCursor, commentEnd);
        if (replaceStarter != -1) {
            final String oldVersion = file.substring(replaceStarter + REPLACE_START.length(), commentEnd).trim();
            replacementStack.push(oldVersion);

            err.printf("  .. starting replacement block on line %d for %s -> %s%n", lineNumber, oldVersion, newVersion);
        }
    }
    writer.write(file.substring(writeCursor).transform(replacer));
    Files.writeString(target, writer.toString());
}
