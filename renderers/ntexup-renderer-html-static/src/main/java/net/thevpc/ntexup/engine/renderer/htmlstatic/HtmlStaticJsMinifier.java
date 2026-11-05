package net.thevpc.ntexup.engine.renderer.htmlstatic;

/**
 * Strips comments and indentation from the bundled player.
 * <p>
 * This is deliberately not a real minifier: it never joins two lines, never
 * rewrites an identifier and never reorders a token, so the shipped file keeps a
 * line-for-line correspondence with the source the exporter was built from and a
 * browser stack trace still names a statement that exists. What it removes is the
 * part a reader does not need at runtime, which in a commented file like the
 * player is around a third of the bytes.
 * <p>
 * Strings, template literals and patterns are copied verbatim so a comment marker
 * inside one of them survives. A slash opens a pattern only where javascript can
 * syntactically expect one, which is why the many division sites in the player
 * are not mistaken for a pattern. The heuristic does not know about keywords, so
 * a pattern written directly after a word, as in {@code return /x/}, would be
 * read as a division; the player is written with every pattern in an argument
 * list, and {@link #minify} is only ever pointed at it.
 */
final class HtmlStaticJsMinifier {

    private HtmlStaticJsMinifier() {
    }

    static String minify(String source) {
        StringBuilder out = new StringBuilder(source.length());
        //last character copied that is not whitespace, which decides both whether a
        //slash divides and whether two tokens need a space between them
        char last = '\0';
        int i = 0;
        int n = source.length();
        while (i < n) {
            char c = source.charAt(i);
            if (c == '/' && i + 1 < n && source.charAt(i + 1) == '/') {
                //the line break that ends it is left for the whitespace branch
                i = endOfLine(source, i);
            } else if (c == '/' && i + 1 < n && source.charAt(i + 1) == '*') {
                int close = source.indexOf("*/", i + 2);
                if (close < 0) {
                    //an unterminated block comment runs to the end of the file
                    i = dropComment(source, i, n, out);
                } else {
                    i = dropComment(source, i, close + 2, out);
                }
            } else if (c == '/' && patternExpected(last)) {
                int end = endOfPattern(source, i);
                out.append(source, i, end);
                last = source.charAt(end - 1);
                i = end;
            } else if (c == '"' || c == '\'' || c == '`') {
                int end = endOfString(source, i, c);
                out.append(source, i, end);
                last = source.charAt(end - 1);
                i = end;
            } else if (c == '\n' || c == '\r' || c == '\t' || c == ' ') {
                int next = i;
                boolean newline = false;
                while (next < n) {
                    char d = source.charAt(next);
                    if (d == '\n') {
                        newline = true;
                        next++;
                    } else if (d == ' ' || d == '\t' || d == '\r') {
                        next++;
                    } else if (d == '/' && next + 1 < n && (source.charAt(next + 1) == '/'
                            || source.charAt(next + 1) == '*')) {
                        //a run of whitespace and a comment are one gap between tokens
                        int stop = source.charAt(next + 1) == '/'
                                ? endOfLine(source, next)
                                : commentEnd(source, next);
                        newline |= containsNewline(source, next, stop);
                        next = stop;
                    } else {
                        break;
                    }
                }
                if (newline) {
                    out.append('\n');
                    last = '\0';
                } else if (isWordChar(last) && isWordChar(next < n ? source.charAt(next) : '\0')) {
                    out.append(' ');
                }
                i = next;
            } else {
                out.append(c);
                last = c;
                i++;
            }
        }
        return trimLines(out.toString());
    }

    /**
     * Drops a comment, except that a comment spanning lines has to leave a line
     * break behind: the statements around it are separated by it in the source and
     * must stay separated.
     */
    private static int dropComment(String source, int from, int to, StringBuilder out) {
        if (containsNewline(source, from, to)) {
            out.append('\n');
        }
        return to;
    }

    private static boolean containsNewline(String source, int from, int to) {
        for (int i = from; i < to; i++) {
            if (source.charAt(i) == '\n') {
                return true;
            }
        }
        return false;
    }

    private static int endOfLine(String source, int from) {
        int n = source.indexOf('\n', from);
        return n < 0 ? source.length() : n;
    }

    private static int commentEnd(String source, int from) {
        int close = source.indexOf("*/", from + 2);
        return close < 0 ? source.length() : close + 2;
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    /**
     * A slash opens a pattern where an expression cannot have ended, that is after
     * an operator, a separator or the start of a line; after a word, a closing
     * bracket or a quote it divides.
     */
    private static boolean patternExpected(char last) {
        if (last == '\0') {
            return true;
        }
        return !(isWordChar(last) || last == ')' || last == ']' || last == '"' || last == '\''
                || last == '`' || last == '+' || last == '-');
    }

    /**
     * Consumes a pattern up to the slash that closes it, honouring escapes and the
     * bracket part, where an unescaped slash is only a class member.
     */
    private static int endOfPattern(String source, int from) {
        int n = source.length();
        boolean inClass = false;
        int i = from + 1;
        while (i < n) {
            char c = source.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == '\n') {
                //a pattern cannot span lines, so this slash was not one after all
                return from + 1;
            }
            if (c == '[') {
                inClass = true;
            } else if (c == ']') {
                inClass = false;
            } else if (c == '/' && !inClass) {
                i++;
                while (i < n && isWordChar(source.charAt(i))) {
                    i++;//flags
                }
                return i;
            }
            i++;
        }
        return n;
    }

    private static int endOfString(String source, int from, char quote) {
        int n = source.length();
        int i = from + 1;
        while (i < n) {
            char c = source.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == quote) {
                return i + 1;
            }
            if (c == '\n' && quote != '`') {
                return i;//a plain string cannot span lines
            }
            i++;
        }
        return n;
    }

    private static String trimLines(String stripped) {
        StringBuilder out = new StringBuilder(stripped.length());
        for (String line : stripped.split("\n", -1)) {
            int from = 0;
            int to = line.length();
            while (from < to && line.charAt(from) == ' ') {
                from++;
            }
            while (to > from && line.charAt(to - 1) == ' ') {
                to--;
            }
            if (to > from) {
                out.append(line, from, to).append('\n');
            }
        }
        return out.toString();
    }
}
