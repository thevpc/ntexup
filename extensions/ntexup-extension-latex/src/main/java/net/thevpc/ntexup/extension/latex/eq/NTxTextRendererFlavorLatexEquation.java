package net.thevpc.ntexup.extension.latex.eq;

import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.renderer.*;
import net.thevpc.ntexup.api.renderer.text.*;
import net.thevpc.ntexup.api.util.NTxColors;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.text.NMsg;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.List;

public class NTxTextRendererFlavorLatexEquation implements NTxTextRendererFlavor {
    @Override
    public String type() {
        return "latex-equation";
    }


    @Override
    public void buildText(String text, NTxTextOptions options, NTxNode node, NTxNodeRendererContext ctx, NTxTextRendererBuilder builder) {
        if (!text.isEmpty()) {
            NTxRichTextToken r = new NTxRichTextToken(
                    NTxRichTextTokenType.IMAGE_PAINTER,
                    text.toString()
            );
            double fontSize = NTxValueByName.getFontSize(node, ctx);
            r.imagePainter = this.createLatex(text, fontSize, options, node, ctx);
            NTxDouble2 size = r.imagePainter.size();
            r.bounds = new Rectangle2D.Double(0, 0, size.getX(), size.getX());
            builder.currRow().addToken(r);
        }
    }

    @Override
    public List<String> getParsePrefixes() {
        return Arrays.asList(
                "[[eq:",
                "[[equation:",
                "[[latex-equation:",
                "\\("
        );
    }

    @Override
    public List<NTxTextToken> parseTokens(NTxTextRendererFlavorParseContext ctx) {
        return ctx.parseDefault(new String[]{
                "eq",
                "equation",
                "latex-equation"
        }, new String[]{
                "\\(", "\\)"
        }, s -> {
            NExtendedLatexMathBuilder eb = new NExtendedLatexMathBuilder();
            eb.append(s);
            eb.flush();
            return eb.toString();
        });
    }


    public NTxTextRendererBuilder.ImagePainter createLatex(String tex, double fontSize, NTxTextOptions options, NTxNode node, NTxNodeRendererContext ctx) {
        TeXFormula formula;
        boolean error = false;
        try {
            formula = new TeXFormula(tex);
        } catch (Exception ex) {
            error = true;
            formula = new TeXFormula("?error?");
            ctx.engine().log().log(NMsg.ofC("error evaluating latex formula %s : %s", tex, ex), NTxUtils.sourceOf(node));
        }
        float size = (float) (fontSize / 2.0);
        TeXIcon icon = formula.createTeXIcon(TeXConstants.STYLE_DISPLAY, size);

        // insert a border
        icon.setInsets(new Insets(0, 0, 0, 0));
//        if (error) {
//            return null;
//        }
        Color foregroundColor = null;
        if (options.foregroundColorIndex != null) {
            foregroundColor = NTxColors.resolveDefaultColorByIndex(options.foregroundColorIndex, null, node, ctx);
        } else if (options.foregroundColor instanceof Color) {
            foregroundColor = (Color) options.foregroundColor;
        }
        Color fg = NTxUtils.paintAsColor(NTxUtils.resolveForegroundColor(options, node, ctx));
        if (fg == null) {
            fg = NTxUtils.paintAsColor(ctx.getForegroundColor(node, true));
        }
        if (fg == null) {
            fg = Color.BLACK;
        }
        Color finalForegroundColor = fg;
        return new NTxTextRendererBuilder.ImagePainter() {
            @Override
            public void paint(NTxGraphics g, double x, double y) {
                Font plainFont = g.getFont().deriveFont(g.getFont().getSize() / 2f);
                g.setFont(plainFont);
                FontMetrics fontMetrics = g.getFontMetrics(plainFont);
                double xx = x;
                double yy = y;//+ascent-descent;
                icon.setForeground(finalForegroundColor);
                icon.paintIcon(null, g.graphics2D(), (int) x, (int) y /*- icon.getIconHeight()*/);
                //g.drawRect(xx, yy, icon.getIconWidth(), icon.getIconHeight());
            }

            public NTxDouble2 size() {
                return new NTxDouble2(icon.getIconWidth(), icon.getIconHeight());
            }
        };
    }

}
