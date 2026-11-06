package net.thevpc.ntexup.extension.scoreboard.render;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NListContainerElement;
import net.thevpc.nuts.util.NLiteral;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * All customizable knobs of the scoreboard, resolved from node properties.
 *
 * <p>Only this class knows about property names: the renderer works with a
 * plain immutable-ish style object, which keeps the drawing code readable.</p>
 */
public final class ScoreboardStyle {

    public static final Color[] DARK_PALETTE = {
            new Color(0x6366F1), // indigo
            new Color(0x8B5CF6), // violet
            new Color(0x0EA5E9), // sky
            new Color(0x10B981), // emerald
            new Color(0xF59E0B), // amber
            new Color(0xEF4444), // red
            new Color(0xEC4899), // pink
            new Color(0x14B8A6), // teal
    };
    public static final Color[] LIGHT_PALETTE = {
            new Color(0x4F46E5),
            new Color(0x7C3AED),
            new Color(0x0284C7),
            new Color(0x059669),
            new Color(0xD97706),
            new Color(0xDC2626),
            new Color(0xDB2777),
            new Color(0x0D9488),
    };
    public static final Color[] NEON_PALETTE = {
            new Color(0x22D3EE),
            new Color(0xA78BFA),
            new Color(0xF472B6),
            new Color(0x34D399),
            new Color(0xFBBF24),
            new Color(0xFB7185),
            new Color(0x60A5FA),
            new Color(0x2DD4BF),
    };

    public String theme = "dark";
    public String fontFamily = Font.SANS_SERIF;
    public Color backgroundColor;
    public Color background2Color;
    public Color titleColor;
    public Color subtitleColor;
    public Color nameColor;
    public Color scoreColor;
    public Color trackColor;
    public Color onTrackColor;
    public Color rankColor;
    public Color mutedColor;
    public Color accentColor;
    public Color[] palette;
    public Color barColor;
    public String iconPath;
    public String unit = "";
    public String scoreFormat;
    public boolean showTitle = true;
    public boolean showSubtitle = true;
    public boolean showRank = true;
    public boolean showScore = true;
    public boolean showUnit;
    public boolean sort = true;
    public boolean pulse;
    public double duration = 2.6;
    public double stagger = 0.07;
    public double pulseDuration = 2.5;
    public double max = -1;
    public double gapRatio = 0.16;
    public double cornerRatio = 0.34;
    public int limit = -1;
    public String dataFormat;

    public static ScoreboardStyle read(NTxNode node) {
        ScoreboardStyle s = new ScoreboardStyle();
        s.theme = firstString(node, "theme", "variant").orElse("dark").toLowerCase();
        s.fontFamily = firstString(node, NTxPropName.FONT_FAMILY, "font", "font-name").orElse(Font.SANS_SERIF);
        s.iconPath = firstString(node, "icon", "logo", "image").orElse(null);
        s.unit = firstString(node, "unit", "suffix").orElse("");
        s.scoreFormat = firstString(node, "score-format", "format-score", "number-format").orElse(null);
        s.dataFormat = firstString(node, "format", "data-format", "dataformat").orElse(null);
        s.showTitle = firstBool(node, true, "show-title", "title-visible");
        s.showSubtitle = firstBool(node, true, "show-subtitle", "subtitle-visible");
        s.showRank = firstBool(node, true, "show-rank", "rank");
        s.showScore = firstBool(node, true, "show-score", "show-value", "show-values");
        s.showUnit = firstBool(node, !s.unit.isEmpty(), "show-unit");
        s.sort = firstBool(node, true, "sort", "sorted");
        s.pulse = firstBool(node, false, "pulse", "glow");
        s.duration = firstDouble(node, "duration", "animation-duration", "reveal-duration").orElse(2.6);
        s.stagger = firstDouble(node, "stagger", "row-stagger", "delay").orElse(0.07);
        s.pulseDuration = firstDouble(node, "pulse-duration", "glow-duration").orElse(2.5);
        s.max = firstDouble(node, "max", "max-score", "maximum").orElse(-1.0);
        s.gapRatio = firstDouble(node, "gap", "row-gap").orElse(0.16);
        s.cornerRatio = firstDouble(node, "radius", "corner-radius", "row-radius").orElse(0.34);
        s.limit = firstInt(node, "limit", "top", "max-rows").orElse(-1);
        if (s.duration < 0.05) {
            s.duration = 0.05;
        }
        if (s.stagger < 0) {
            s.stagger = 0;
        }
        if (s.pulseDuration < 0) {
            s.pulseDuration = 0;
        }
        if (s.gapRatio < 0) {
            s.gapRatio = 0;
        }
        if (s.gapRatio > 0.8) {
            s.gapRatio = 0.8;
        }
        if (s.cornerRatio < 0) {
            s.cornerRatio = 0;
        }

        applyTheme(s);
        applyOverrides(node, s);
        return s;
    }

    private static void applyTheme(ScoreboardStyle s) {
        switch (s.theme) {
            case "light":
                s.backgroundColor = new Color(0xF8FAFC);
                s.background2Color = new Color(0xE2E8F0);
                s.titleColor = new Color(0x0F172A);
                s.subtitleColor = new Color(0x64748B);
                s.nameColor = Color.WHITE;
                s.scoreColor = Color.WHITE;
                s.trackColor = new Color(0xE2E8F0);
                s.onTrackColor = new Color(0x0F172A);
                s.rankColor = new Color(0x0F172A);
                s.mutedColor = new Color(0x64748B);
                s.palette = LIGHT_PALETTE;
                break;
            case "neon":
                s.backgroundColor = new Color(0x050510);
                s.background2Color = new Color(0x0F0F2D);
                s.titleColor = new Color(0xF8FAFC);
                s.subtitleColor = new Color(0xA5B4FC);
                s.nameColor = Color.WHITE;
                s.scoreColor = Color.WHITE;
                s.trackColor = new Color(0x181836);
                s.onTrackColor = new Color(0xE2E8F0);
                s.rankColor = new Color(0xF8FAFC);
                s.mutedColor = new Color(0xA5B4FC);
                s.palette = NEON_PALETTE;
                break;
            case "minimal":
                s.backgroundColor = new Color(0xFFFFFF);
                s.background2Color = new Color(0xF1F5F9);
                s.titleColor = new Color(0x111827);
                s.subtitleColor = new Color(0x6B7280);
                s.nameColor = Color.WHITE;
                s.scoreColor = Color.WHITE;
                s.trackColor = new Color(0xE5E7EB);
                s.onTrackColor = new Color(0x111827);
                s.rankColor = new Color(0x111827);
                s.mutedColor = new Color(0x6B7280);
                s.palette = LIGHT_PALETTE;
                break;
            case "dark":
            default:
                s.theme = "dark";
                s.backgroundColor = new Color(0x0B1120);
                s.background2Color = new Color(0x1E293B);
                s.titleColor = new Color(0xF8FAFC);
                s.subtitleColor = new Color(0x94A3B8);
                s.nameColor = Color.WHITE;
                s.scoreColor = Color.WHITE;
                s.trackColor = new Color(0x1B2331);
                s.onTrackColor = new Color(0xE2E8F0);
                s.rankColor = new Color(0xF8FAFC);
                s.mutedColor = new Color(0x94A3B8);
                s.palette = DARK_PALETTE;
                break;
        }
    }

    private static void applyOverrides(NTxNode node, ScoreboardStyle s) {
        Color bg = firstColor(node, NTxPropName.BACKGROUND_COLOR, "background", "bg-color");
        if (bg != null) {
            s.backgroundColor = bg;
            s.background2Color = mix(bg, bg.darker(), 0.35);
        }
        Color fg = firstColor(node, NTxPropName.COLOR, NTxPropName.FOREGROUND_COLOR, "foreground");
        if (fg != null) {
            s.titleColor = fg;
            s.subtitleColor = withAlpha(fg, 180);
        }
        Color accent = firstColor(node, "accent", "accent-color");
        if (accent != null) {
            s.accentColor = accent;
        }
        Color bar = firstColor(node, "bar-color", "barcolor");
        if (bar != null) {
            s.barColor = bar;
        }
        s.barColor = s.barColor == null ? s.accentColor : s.barColor;
        Color track = firstColor(node, "track-color", "trackcolor", "bar-background", "bar-background-color");
        if (track != null) {
            s.trackColor = track;
        }
        Color title = firstColor(node, "title-color", "titlecolor");
        if (title != null) {
            s.titleColor = title;
        }
        Color subtitle = firstColor(node, "subtitle-color", "subtitlecolor");
        if (subtitle != null) {
            s.subtitleColor = subtitle;
        }
        Color name = firstColor(node, "name-color", "namecolor");
        if (name != null) {
            s.nameColor = name;
        }
        Color score = firstColor(node, "score-color", "scorecolor");
        if (score != null) {
            s.scoreColor = score;
        }
        Color rank = firstColor(node, "rank-color", "rankcolor");
        if (rank != null) {
            s.rankColor = rank;
        }
        Color[] palette = readPalette(node, "palette", "colors", "row-colors", "bar-colors");
        if (palette != null && palette.length > 0) {
            s.palette = palette;
        }
        if (s.barColor != null) {
            // a single bar color still benefits from the vertical gradient shift
            Color[] p = new Color[s.palette.length];
            for (int i = 0; i < p.length; i++) {
                p[i] = s.barColor;
            }
            s.palette = p;
        }
    }

    public Color colorForRank(int rank, Color explicit) {
        if (explicit != null) {
            return explicit;
        }
        if (palette == null || palette.length == 0) {
            return Color.BLUE;
        }
        return palette[Math.floorMod(rank, palette.length)];
    }

    /**
     * Picks a readable text colour on top of a coloured bar: white for dark
     * bars and the dark theme ink for bright bars (gold, yellow, light cyan).
     * An explicitly chosen non-white colour is preserved.
     */
    public static Color textOnBar(Color bar, Color preferred) {
        if (bar == null) {
            return preferred;
        }
        double lum = (0.299 * bar.getRed() + 0.587 * bar.getGreen() + 0.114 * bar.getBlue()) / 255.0;
        if (preferred == null || preferred.equals(Color.WHITE)) {
            if (lum > 0.62) {
                return new Color(0x0B1120);
            }
            return Color.WHITE;
        }
        return preferred;
    }

    public static Color medalColor(int rank) {
        switch (rank) {
            case 0:
                return new Color(0xFFD166); // gold
            case 1:
                return new Color(0xC7CEDB); // silver
            case 2:
                return new Color(0xE29A5B); // bronze
            default:
                return null;
        }
    }

    public static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), clamp255(alpha));
    }

    public static Color mix(Color a, Color b, double ratio) {
        double r = Math.max(0, Math.min(1, ratio));
        return new Color(
                (int) Math.round(a.getRed() * (1 - r) + b.getRed() * r),
                (int) Math.round(a.getGreen() * (1 - r) + b.getGreen() * r),
                (int) Math.round(a.getBlue() * (1 - r) + b.getBlue() * r)
        );
    }

    public static Color brighten(Color c, double amount) {
        int r = (int) Math.round(c.getRed() + (255 - c.getRed()) * amount);
        int g = (int) Math.round(c.getGreen() + (255 - c.getGreen()) * amount);
        int b = (int) Math.round(c.getBlue() + (255 - c.getBlue()) * amount);
        return new Color(clamp255(r), clamp255(g), clamp255(b), c.getAlpha());
    }

    public static Color darken(Color c, double amount) {
        int r = (int) Math.round(c.getRed() * (1 - amount));
        int g = (int) Math.round(c.getGreen() * (1 - amount));
        int b = (int) Math.round(c.getBlue() * (1 - amount));
        return new Color(clamp255(r), clamp255(g), clamp255(b), c.getAlpha());
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static Color firstColor(NTxNode node, String... names) {
        for (String name : names) {
            NElement e = NTxValue.ofProp(node, name).asElement().orElse(null);
            if (e != null && !e.isNull()) {
                Color c = NTxValue.of(e).asColor().orElse(null);
                if (c != null) {
                    return c;
                }
            }
        }
        return null;
    }

    private static Color[] readPalette(NTxNode node, String... names) {
        for (String name : names) {
            NElement e = NTxValue.ofProp(node, name).asElement().orElse(null);
            if (e == null || e.isNull()) {
                continue;
            }
            List<Color> colors = new ArrayList<>();
            NListContainerElement list = e.toListContainer().orElse(null);
            if (list != null) {
                for (NElement child : list.children()) {
                    Color c = NTxValue.of(child).asColor().orElse(null);
                    if (c != null) {
                        colors.add(c);
                    }
                }
            } else if (e.isAnyString()) {
                for (String token : e.asStringValue().orElse("").split("[,;\\s]+")) {
                    if (token.trim().isEmpty()) {
                        continue;
                    }
                    Color c = NTxValue.of(NElement.ofString(token.trim())).asColor().orElse(null);
                    if (c != null) {
                        colors.add(c);
                    }
                }
            }
            if (!colors.isEmpty()) {
                return colors.toArray(Color[]::new);
            }
        }
        return null;
    }

    private static java.util.Optional<String> firstString(NTxNode node, String... names) {
        for (String name : names) {
            NElement e = NTxValue.ofProp(node, name).asElement().orElse(null);
            if (e != null && !e.isNull()) {
                if (e.isAnyString()) {
                    String s = e.asStringValue().orElse("");
                    if (!s.isEmpty()) {
                        return java.util.Optional.of(s);
                    }
                } else if (e.isName() || e.isNumber()) {
                    String s = e.toString();
                    if (!s.isEmpty()) {
                        return java.util.Optional.of(s);
                    }
                }
            }
        }
        return java.util.Optional.empty();
    }

    private static java.util.Optional<Double> firstDouble(NTxNode node, String... names) {
        for (String name : names) {
            NElement e = NTxValue.ofProp(node, name).asElement().orElse(null);
            if (e != null && !e.isNull()) {
                Double d = e.asDoubleValue().orElse(null);
                if (d == null && e.isAnyString()) {
                    d = NLiteral.of(e.asStringValue().orElse("")).asDouble().orElse(null);
                }
                if (d != null) {
                    return java.util.Optional.of(d);
                }
            }
        }
        return java.util.Optional.empty();
    }

    private static java.util.Optional<Integer> firstInt(NTxNode node, String... names) {
        java.util.Optional<Double> d = firstDouble(node, names);
        return d.map(Double::intValue);
    }

    private static boolean firstBool(NTxNode node, boolean defaultValue, String... names) {
        for (String name : names) {
            NElement e = NTxValue.ofProp(node, name).asElement().orElse(null);
            if (e != null && !e.isNull()) {
                Boolean b = e.asBooleanValue().orElse(null);
                if (b != null) {
                    return b;
                }
            }
        }
        return defaultValue;
    }
}
