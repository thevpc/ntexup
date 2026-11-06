package net.thevpc.ntexup.extension.scoreboard.data;

import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.extension.scoreboard.model.ScoreEntry;
import net.thevpc.ntexup.extension.scoreboard.model.ScoreboardModel;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NListContainerElement;
import net.thevpc.nuts.elem.NObjectElement;
import net.thevpc.nuts.elem.NPairElement;
import net.thevpc.nuts.util.NLiteral;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts the many convenient ways of describing scores into a
 * {@link ScoreboardModel}.
 *
 * <p>Supported shapes:</p>
 * <ul>
 *     <li>TSON list of objects: {@code [{name:"A", score:12}, ...]}</li>
 *     <li>TSON object with metadata: {@code {title:"Cup", scores:[...]}}</li>
 *     <li>TSON list of positional rows: {@code [["A", 12], ["B", "Team B", 9]]}</li>
 *     <li>TSON pair map: {@code {A:12, B:9}}</li>
 *     <li>CSV text (comma, semicolon or tab separated, quoted cells supported)</li>
 * </ul>
 */
public final class ScoreboardDataParser {

    private static final String[] NESTED_KEYS = {
            "scores", "score-list", "scorelist", "data", "entries", "items", "rows", "results", "ranking"
    };
    private static final String[] NAME_KEYS = {"name", "team", "label", "competitor", "player"};
    private static final String[] SCORE_KEYS = {"score", "points", "value", "total", "result"};
    private static final String[] SUBNAME_KEYS = {
            "sub-name", "subname", "sub", "description", "desc", "group", "category"
    };
    private static final String[] DISCLOSED_KEYS = {
            "disclosed-name", "disclosedname", "disclosed", "revealed-name", "revealedname", "reveal"
    };
    private static final String[] COLOR_KEYS = {"color", "colour", "bar-color", "barcolor"};
    private static final String[] UNIT_KEYS = {"unit", "suffix"};

    private ScoreboardDataParser() {
    }

    public static ScoreboardModel parseElement(NElement root) {
        ScoreboardModel model = new ScoreboardModel();
        parseInto(root, model);
        return model.reindex();
    }

    public static ScoreboardModel parseCsv(String csv) {
        ScoreboardModel model = new ScoreboardModel();
        parseCsvInto(csv, model);
        return model.reindex();
    }

    private static void parseInto(NElement e, ScoreboardModel model) {
        if (e == null || e.isNull()) {
            return;
        }
        if (e.isAnyString()) {
            // A bare string is interpreted as CSV content. External files are
            // resolved by the builder before reaching this parser.
            parseCsvInto(e.asStringValue().orElse(""), model);
            return;
        }
        if (e.isNamedPair()) {
            NPairElement pair = e.asNamedPair().orElse(null);
            if (pair != null) {
                addPair(pair, model);
                return;
            }
        }
        if (e.isObject()) {
            NObjectElement object = e.asObject().orElse(null);
            if (object == null) {
                return;
            }
            NElement nested = firstObjectValue(object, NESTED_KEYS);
            if (nested != null) {
                model.setTitleIfAbsent(firstString(object, "title", "name"));
                model.setSubtitleIfAbsent(firstString(object, "subtitle", "sub-title", "caption"));
                parseInto(nested, model);
                return;
            }
            ScoreEntry entry = objectEntry(object);
            if (entry != null) {
                model.getEntries().add(entry);
            } else {
                // A plain name -> score map: {Alice: 42, Bob: 30}
                for (NElement child : object.children()) {
                    if (child.isNamedPair()) {
                        NPairElement pair = child.asNamedPair().orElse(null);
                        if (pair != null) {
                            addPair(pair, model);
                        }
                    }
                }
            }
            return;
        }
        if (!e.isListContainer()) {
            return;
        }
        NListContainerElement list = e.toListContainer().orElse(null);
        if (list == null) {
            return;
        }
        if (isPositionalRow(list)) {
            List<String> cells = new ArrayList<>();
            for (NElement child : list.children()) {
                cells.add(stringOf(child));
            }
            model.getEntries().add(positionalEntry(cells));
            return;
        }
        for (NElement child : list.children()) {
            parseInto(child, model);
        }
    }

    /**
     * A list whose direct children are all scalar cells is one positional row
     * ({@code ["Alice", "Team A", 42]}), not a container of entries. Checking
     * <em>direct</em> children also stops the parser from mistaking a number
     * for an endless list container.
     */
    private static boolean isPositionalRow(NListContainerElement list) {
        boolean any = false;
        for (NElement child : list.children()) {
            if (child.isNull()) {
                continue;
            }
            any = true;
            if (child.isObject() || child.isPair() || child.isListContainer()) {
                return false;
            }
        }
        return any;
    }

    private static void addPair(NPairElement pair, ScoreboardModel model) {
        String name = stringOf(pair.key());
        NElement value = pair.value();
        if (value == null || value.isNull()) {
            return;
        }
        if (value.isNumber() || value.isAnyString()) {
            ScoreEntry entry = new ScoreEntry().setName(name);
            Double score = doubleOf(value);
            if (score != null) {
                entry.setScore(score);
            } else {
                entry.setSubName(stringOf(value));
            }
            model.getEntries().add(entry);
            return;
        }
        if (value.isObject()) {
            NObjectElement object = value.asObject().orElse(null);
            if (object != null) {
                ScoreEntry entry = objectEntry(object);
                if (entry != null) {
                    if (entry.getName().isEmpty()) {
                        entry.setName(name);
                    }
                    model.getEntries().add(entry);
                }
            }
            return;
        }
        NListContainerElement list = value.toListContainer().orElse(null);
        if (list != null) {
            List<String> cells = new ArrayList<>();
            cells.add(name);
            for (NElement child : list.children()) {
                cells.add(stringOf(child));
            }
            model.getEntries().add(positionalEntry(cells));
        }
    }

    private static ScoreEntry objectEntry(NObjectElement object) {
        String name = firstString(object, NAME_KEYS);
        Double score = firstDouble(object, SCORE_KEYS);
        if (name == null && score == null && object.size() == 1) {
            // singleton map { "Alice": 42 }
            for (NElement child : object.children()) {
                if (child.isNamedPair()) {
                    NPairElement pair = child.asNamedPair().orElse(null);
                    if (pair != null) {
                        ScoreEntry entry = new ScoreEntry().setName(stringOf(pair.key()));
                        Double v = doubleOf(pair.value());
                        entry.setScore(v == null ? 0 : v);
                        return entry;
                    }
                }
            }
        }
        if (name == null && score == null) {
            return null;
        }
        ScoreEntry entry = new ScoreEntry()
                .setName(name == null ? "" : name)
                .setScore(score == null ? 0 : score);
        String subName = firstString(object, SUBNAME_KEYS);
        if (subName != null) {
            entry.setSubName(subName);
        }
        String disclosedName = firstString(object, DISCLOSED_KEYS);
        if (disclosedName != null) {
            entry.setDisclosedName(disclosedName);
        }
        NElement colorElement = firstObjectValue(object, COLOR_KEYS);
        if (colorElement != null) {
            entry.setColor(colorOf(colorElement));
        }
        String unit = firstString(object, UNIT_KEYS);
        if (unit != null) {
            entry.setUnit(unit);
        }
        return entry;
    }

    private static ScoreEntry positionalEntry(List<String> cells) {
        if (cells.size() >= 4) {
            ScoreEntry entry = new ScoreEntry()
                    .setName(cells.get(0).trim())
                    .setSubName(cells.get(1).trim())
                    .setDisclosedName(cells.get(2).trim());
            Double score = doubleOfString(cells.get(3));
            if (score == null) {
                score = lastNumber(cells);
            }
            entry.setScore(score == null ? 0 : score);
            return entry;
        }
        if (cells.size() == 3) {
            ScoreEntry entry = new ScoreEntry().setName(cells.get(0).trim());
            Double score = doubleOfString(cells.get(2));
            if (score != null) {
                entry.setSubName(cells.get(1).trim()).setScore(score);
            } else {
                entry.setSubName(cells.get(1).trim()).setDisclosedName(cells.get(2).trim());
            }
            return entry;
        }
        if (cells.size() == 2) {
            ScoreEntry entry = new ScoreEntry().setName(cells.get(0).trim());
            Double score = doubleOfString(cells.get(1));
            if (score != null) {
                entry.setScore(score);
            } else {
                entry.setSubName(cells.get(1).trim());
            }
            return entry;
        }
        return new ScoreEntry().setName(cells.isEmpty() ? "" : cells.get(0).trim());
    }

    private static Double lastNumber(List<String> cells) {
        for (int i = cells.size() - 1; i >= 0; i--) {
            Double d = doubleOfString(cells.get(i));
            if (d != null) {
                return d;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // CSV
    // ------------------------------------------------------------------

    private static void parseCsvInto(String csv, ScoreboardModel model) {
        if (csv == null) {
            return;
        }
        String text = csv.replace("\r\n", "\n").replace('\r', '\n');
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\n", -1)) {
            if (!line.trim().isEmpty()) {
                lines.add(line);
            }
        }
        if (lines.isEmpty()) {
            return;
        }
        char delimiter = detectDelimiter(lines.get(0));
        int start = 0;
        CsvHeader header = null;
        List<String> firstCells = parseCsvLine(lines.get(0), delimiter);
        if (looksLikeHeader(firstCells)) {
            header = CsvHeader.of(firstCells);
            start = 1;
        }
        for (int i = start; i < lines.size(); i++) {
            List<String> cells = parseCsvLine(lines.get(i), delimiter);
            ScoreEntry entry = header != null ? header.entry(cells) : positionalEntry(cells);
            if (entry != null && !entry.getName().isEmpty()) {
                model.getEntries().add(entry);
            }
        }
    }

    private static char detectDelimiter(String line) {
        int commas = countOutsideQuotes(line, ',');
        int semis = countOutsideQuotes(line, ';');
        int tabs = countOutsideQuotes(line, '\t');
        if (tabs > commas && tabs >= semis) {
            return '\t';
        }
        if (semis > commas) {
            return ';';
        }
        return ',';
    }

    private static int countOutsideQuotes(String line, char c) {
        int count = 0;
        boolean quotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    i++;
                } else {
                    quotes = !quotes;
                }
            } else if (ch == c && !quotes) {
                count++;
            }
        }
        return count;
    }

    private static List<String> parseCsvLine(String line, char delimiter) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    quotes = !quotes;
                }
            } else if (c == delimiter && !quotes) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    private static boolean looksLikeHeader(List<String> cells) {
        boolean hasScore = false;
        boolean hasName = false;
        for (String cell : cells) {
            String n = normalizeHeader(cell);
            if ("score".equals(n) || "points".equals(n) || "value".equals(n) || "total".equals(n)) {
                hasScore = true;
            }
            if ("name".equals(n) || "team".equals(n) || "label".equals(n) || "competitor".equals(n)) {
                hasName = true;
            }
        }
        return hasScore && hasName;
    }

    private static String normalizeHeader(String s) {
        return s.replace("\uFEFF", "").trim().toLowerCase().replace("_", "").replace("-", "").replace(" ", "");
    }

    private static final class CsvHeader {
        int name = 0;
        int score = -1;
        int subName = -1;
        int disclosed = -1;
        int color = -1;
        int unit = -1;

        static CsvHeader of(List<String> cells) {
            CsvHeader h = new CsvHeader();
            for (int i = 0; i < cells.size(); i++) {
                String n = normalizeHeader(cells.get(i));
                if ("name".equals(n) || "team".equals(n) || "label".equals(n) || "competitor".equals(n)) {
                    h.name = i;
                } else if ("score".equals(n) || "points".equals(n) || "value".equals(n) || "total".equals(n)) {
                    h.score = i;
                } else if ("subname".equals(n) || "sub".equals(n) || "description".equals(n)
                        || "desc".equals(n) || "group".equals(n) || "category".equals(n)) {
                    h.subName = i;
                } else if ("disclosedname".equals(n) || "disclosed".equals(n)
                        || "revealedname".equals(n) || "reveal".equals(n)) {
                    h.disclosed = i;
                } else if ("color".equals(n) || "colour".equals(n) || "barcolor".equals(n)) {
                    h.color = i;
                } else if ("unit".equals(n) || "suffix".equals(n)) {
                    h.unit = i;
                }
            }
            return h;
        }

        ScoreEntry entry(List<String> cells) {
            String nameValue = cell(cells, this.name);
            if (nameValue.isEmpty()) {
                return null;
            }
            ScoreEntry e = new ScoreEntry().setName(nameValue);
            Double scoreValue = doubleOfString(cell(cells, this.score));
            e.setScore(scoreValue == null ? 0 : scoreValue);
            String sub = cell(cells, subName);
            if (!sub.isEmpty()) {
                e.setSubName(sub);
            }
            String disclosedValue = cell(cells, disclosed);
            if (!disclosedValue.isEmpty()) {
                e.setDisclosedName(disclosedValue);
            }
            String colorValue = cell(cells, color);
            if (!colorValue.isEmpty()) {
                e.setColor(colorOfString(colorValue));
            }
            String unitValue = cell(cells, unit);
            if (!unitValue.isEmpty()) {
                e.setUnit(unitValue);
            }
            return e;
        }

        private static String cell(List<String> cells, int index) {
            if (index < 0 || index >= cells.size()) {
                return "";
            }
            return cells.get(index).trim();
        }
    }

    // ------------------------------------------------------------------
    // primitive helpers
    // ------------------------------------------------------------------

    private static NElement firstObjectValue(NObjectElement object, String... keys) {
        for (String key : keys) {
            NElement e = objectValue(object, key);
            if (e != null && !e.isNull()) {
                return e;
            }
        }
        return null;
    }

    /**
     * Direct object-field lookup. {@code NObjectElement.resolve(String)} performs
     * a recursive path search (and may even format elements for diagnostics);
     * a scoreboard lookup is a plain field lookup, so walking the pairs is both
     * faster and predictable.
     */
    private static NElement objectValue(NObjectElement object, String key) {
        if (object == null || key == null) {
            return null;
        }
        for (NElement child : object.children()) {
            if (child.isNamedPair()) {
                NPairElement pair = child.asNamedPair().orElse(null);
                if (pair != null) {
                    String k = stringOf(pair.key());
                    if (key.equals(k)) {
                        return pair.value();
                    }
                }
            }
        }
        return null;
    }

    private static String firstString(NObjectElement object, String... keys) {
        for (String key : keys) {
            NElement e = objectValue(object, key);
            String s = stringOf(e);
            if (!s.isEmpty()) {
                return s;
            }
        }
        return null;
    }

    private static Double firstDouble(NObjectElement object, String... keys) {
        for (String key : keys) {
            Double d = doubleOf(objectValue(object, key));
            if (d != null) {
                return d;
            }
        }
        return null;
    }

    private static String stringOf(NElement e) {
        if (e == null || e.isNull()) {
            return "";
        }
        if (e.isAnyString()) {
            return e.asStringValue().orElse("");
        }
        if (e.isNumber()) {
            return String.valueOf(e.asNumberValue().orElse(null));
        }
        if (e.isBoolean()) {
            return String.valueOf(e.asBooleanValue().orElse(false));
        }
        return e.toString();
    }

    private static Double doubleOf(NElement e) {
        if (e == null || e.isNull()) {
            return null;
        }
        if (e.isNumber()) {
            return e.asDoubleValue().orElse(null);
        }
        if (e.isAnyString()) {
            return doubleOfString(e.asStringValue().orElse(""));
        }
        return null;
    }

    private static Double doubleOfString(String s) {
        if (s == null) {
            return null;
        }
        String cleaned = s.trim().replace("%", "").replace("\u00A0", "").replace(" ", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        return NLiteral.of(cleaned).asDouble().orElse(null);
    }

    private static Color colorOf(NElement e) {
        Color c = NTxValue.of(e).asColor().orElse(null);
        if (c != null) {
            return c;
        }
        return colorOfString(stringOf(e));
    }

    private static Color colorOfString(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        Color c = NTxValue.of(NElement.ofString(s.trim())).asColor().orElse(null);
        if (c != null) {
            return c;
        }
        try {
            return Color.decode(s.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
