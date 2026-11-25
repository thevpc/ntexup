package net.thevpc.ntexup.extension.latex.eq;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NExtendedLatexMathBuilder {

    /**
     * JLaTeXmath 1.0.7 maps {@code \Omega} to a glyph that is missing from the
     * bundled cmr10 font (code 173 is the empty "soft hyphen" slot), so the
     * symbol is silently rendered as a blank. The Greek alphabet registration
     * (jlatexmath-font-greek) renders the Unicode omega correctly, so route the
     * command through {@code \text{Ω}}. Remove this workaround once the
     * underlying font has been fixed.
     */
    private static final Pattern OMEGA = Pattern.compile("\\\\Omega(?![A-Za-z])");

    public static String normalizeTex(String tex) {
        if (tex == null) {
            return null;
        }
        return OMEGA.matcher(tex).replaceAll(Matcher.quoteReplacement("\\text{\u03A9}"));
    }
    private static final Map<Character, String> LATEX_MATH_MAP = new HashMap<>();
    private static final Map<Character, String> LATEX_SUPERSCRIPT = new HashMap<>();
    private static final Map<Character, String> LATEX_SUBSCRIPT = new HashMap<>();

    static {
        // Lowercase Greek letters
        LATEX_MATH_MAP.put('α', "\\alpha");
        LATEX_MATH_MAP.put('β', "\\beta");
        LATEX_MATH_MAP.put('γ', "\\gamma");
        LATEX_MATH_MAP.put('δ', "\\delta");
        LATEX_MATH_MAP.put('ε', "\\epsilon");
        LATEX_MATH_MAP.put('ζ', "\\zeta");
        LATEX_MATH_MAP.put('η', "\\eta");
        LATEX_MATH_MAP.put('θ', "\\theta");
        LATEX_MATH_MAP.put('ι', "\\iota");
        LATEX_MATH_MAP.put('κ', "\\kappa");
        LATEX_MATH_MAP.put('λ', "\\lambda");
        LATEX_MATH_MAP.put('μ', "\\mu");
        LATEX_MATH_MAP.put('ν', "\\nu");
        LATEX_MATH_MAP.put('ξ', "\\xi");
        LATEX_MATH_MAP.put('ο', "o");  // omicron often left as letter 'o'
        LATEX_MATH_MAP.put('π', "\\pi");
        LATEX_MATH_MAP.put('ρ', "\\rho");
        LATEX_MATH_MAP.put('σ', "\\sigma");
        LATEX_MATH_MAP.put('τ', "\\tau");
        LATEX_MATH_MAP.put('υ', "\\upsilon");
        LATEX_MATH_MAP.put('φ', "\\phi");
        LATEX_MATH_MAP.put('χ', "\\chi");
        LATEX_MATH_MAP.put('ψ', "\\psi");
        LATEX_MATH_MAP.put('ω', "\\omega");

        // Uppercase Greek letters
        LATEX_MATH_MAP.put('Α', "A");
        LATEX_MATH_MAP.put('Β', "B");
        LATEX_MATH_MAP.put('Γ', "\\Gamma");
        LATEX_MATH_MAP.put('Δ', "\\Delta");
        LATEX_MATH_MAP.put('Ε', "E");
        LATEX_MATH_MAP.put('Ζ', "Z");
        LATEX_MATH_MAP.put('Η', "H");
        LATEX_MATH_MAP.put('Θ', "\\Theta");
        LATEX_MATH_MAP.put('Ι', "I");
        LATEX_MATH_MAP.put('Κ', "K");
        LATEX_MATH_MAP.put('Λ', "\\Lambda");
        LATEX_MATH_MAP.put('Μ', "M");
        LATEX_MATH_MAP.put('Ν', "N");
        LATEX_MATH_MAP.put('Ξ', "\\Xi");
        LATEX_MATH_MAP.put('Ο', "O");
        LATEX_MATH_MAP.put('Π', "\\Pi");
        LATEX_MATH_MAP.put('Ρ', "P");
        LATEX_MATH_MAP.put('Σ', "\\Sigma");
        LATEX_MATH_MAP.put('Τ', "T");
        LATEX_MATH_MAP.put('Υ', "\\Upsilon");
        LATEX_MATH_MAP.put('Φ', "\\Phi");
        LATEX_MATH_MAP.put('Χ', "X");
        LATEX_MATH_MAP.put('Ψ', "\\Psi");
        LATEX_MATH_MAP.put('Ω', "\\text{\u03A9}");

        // Other math symbols
        LATEX_MATH_MAP.put('∞', "\\infty");
        LATEX_MATH_MAP.put('√', "\\sqrt");
        LATEX_MATH_MAP.put('±', "\\pm");
        LATEX_MATH_MAP.put('×', "\\times");
        LATEX_MATH_MAP.put('÷', "\\div");
        LATEX_MATH_MAP.put('≈', "\\approx");
        LATEX_MATH_MAP.put('≠', "\\neq");
        LATEX_MATH_MAP.put('≤', "\\leq");
        LATEX_MATH_MAP.put('≥', "\\geq");
        LATEX_MATH_MAP.put('∈', "\\in");
        LATEX_MATH_MAP.put('∇', "\\nabla");
        // Add more as needed

        // Superscripts
        LATEX_SUPERSCRIPT.put('²', "2");
        LATEX_SUPERSCRIPT.put('³', "3");
        LATEX_SUPERSCRIPT.put('⁰', "0");
        LATEX_SUPERSCRIPT.put('¹', "1");
        LATEX_SUPERSCRIPT.put('⁴', "4");
        LATEX_SUPERSCRIPT.put('⁵', "5");
        LATEX_SUPERSCRIPT.put('⁶', "6");
        LATEX_SUPERSCRIPT.put('⁷', "7");
        LATEX_SUPERSCRIPT.put('⁸', "8");
        LATEX_SUPERSCRIPT.put('⁹', "9");
        LATEX_SUPERSCRIPT.put('ⁿ', "n");

        // Subscripts
        LATEX_SUBSCRIPT.put('₀', "0");
        LATEX_SUBSCRIPT.put('₁', "1");
        LATEX_SUBSCRIPT.put('₂', "2");
        LATEX_SUBSCRIPT.put('₃', "3");
        LATEX_SUBSCRIPT.put('₄', "4");
        LATEX_SUBSCRIPT.put('₅', "5");
        LATEX_SUBSCRIPT.put('₆', "6");
        LATEX_SUBSCRIPT.put('₇', "7");
        LATEX_SUBSCRIPT.put('₈', "8");
        LATEX_SUBSCRIPT.put('₉', "9");
    }

    private StringBuilder sb = new StringBuilder();

    private enum PendingType {
        SUPERSCRIPT,
        SUBSCRIPT,
    }

    private PendingType pendingType;
    private StringBuilder pending = new StringBuilder();


    public void append(char c) {
        {
            String a = LATEX_SUPERSCRIPT.get(c);
            if (a != null) {
                if (pendingType != PendingType.SUPERSCRIPT) {
                    flush();
                }
                pendingType = PendingType.SUPERSCRIPT;
                pending.append(a);
                return;
            }
        }
        {
            String a = LATEX_SUBSCRIPT.get(c);
            if (a != null) {
                if (pendingType != PendingType.SUBSCRIPT) {
                    flush();
                }
                pendingType = PendingType.SUBSCRIPT;
                pending.append(a);
                return;
            }
        }
        {
            String a = LATEX_MATH_MAP.get(c);
            if (a != null) {
                if (pendingType != null) {
                    flush();
                }
                pendingType = null;
                sb.append(a).append("{}");
                return;
            }
        }
        if (pendingType != null) {
            flush();
        }
        pendingType = null;
        sb.append(c);
    }

    public void flush() {
        if (pending.length() > 0 && pendingType != null) {
            switch (pendingType) {
                case SUBSCRIPT: {
                    sb.append("_{");
                    sb.append(pending);
                    sb.append("}");
                    pending.setLength(0);
                    pendingType = null;
                    break;
                }
                case SUPERSCRIPT: {
                    sb.append("^{");
                    sb.append(pending);
                    sb.append("}");
                    pending.setLength(0);
                    pendingType = null;
                    break;
                }
            }
        }
    }

    public NExtendedLatexMathBuilder append(String read) {
        for (char c : read.toCharArray()) {
            append(c);
        }
        return this;
    }

    @Override
    public String toString() {
        if (pendingType == null) {
            return sb.toString();
        }
        StringBuilder sb2 = new StringBuilder(sb);
        switch (pendingType) {
            case SUBSCRIPT: {
                sb2.append("_{");
                sb2.append(pending);
                sb2.append("}");
                break;
            }
            case SUPERSCRIPT: {
                sb2.append("^{");
                sb2.append(pending);
                sb2.append("}");
                break;
            }
        }
        return sb2.toString();
    }
}
