/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.extension.latex.eq;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2;
import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.document.NTxSizeRequirements;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.renderer.text.*;
import net.thevpc.ntexup.api.util.NTxColors;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NStringUtils;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * @author vpc
 */
public class NTxEquationBuilder implements NTxNodeBuilder {

    NTxProperties defaultStyles = new NTxProperties();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext.id(NTxNodeType.EQUATION)
                .alias("equation")
                .parseParam().matchesNamedPair(NTxPropName.VALUE,NTxPropName.FILE).then()
                .parseParam().matchesAnyNonPair().storeFirstMissingName(NTxPropName.VALUE).then()
                .renderComponent((rendererContext, builderContext1) -> renderMain(rendererContext, builderContext1))
                .renderText()
                .buildText(this::buildText)
                .parseTokens(this::parseTokens)
                .startSeparators("\\(")
                .end()
                .sizeRequirements(this::sizeRequirements)
                .selfBounds(this::selfBounds);
    }


    public NTxSizeRequirements sizeRequirements(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext builderContext) {
        NTxBounds2 s = rendererContext.selfBounds();
        NTxBounds2 bb = rendererContext.parentBounds();
        return new NTxSizeRequirements(
                s.getWidth(),
                Math.max(bb.getWidth(), s.getWidth()),
                s.getWidth(),
                s.getHeight(),
                Math.max(bb.getHeight(), s.getHeight()),
                s.getHeight()
        );
    }

    public NTxBounds2 selfBounds(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext builderContext) {
        NTxNode node = rendererContext.node();
        String message = NTxValue.ofProp(node, NTxPropName.VALUE).asStringOrName().orNull();
        if (message == null) {
            message = "";
        }
        NTxGraphics g = rendererContext.graphics();
        String tex = NStringUtils.trim(message);
        if (tex.isEmpty()) {
            return new NTxBounds2(rendererContext.parentBounds().getX(), rendererContext.parentBounds().getY(), 0.0, 0.0);
        } else {
            TeXFormula formula;
            try {
                formula = new TeXFormula(tex);
            } catch (Exception ex) {
                formula = new TeXFormula("?error?");
                rendererContext.engine().log().log(NMsg.ofC("error evaluating latex formula %s : %s", tex, ex), NTxUtils.sourceOf(node));
            }
            float size = (float) NTxValueByName.getFontSize(node, rendererContext);
            TeXIcon icon = formula.createTeXIcon(TeXConstants.STYLE_DISPLAY, size);

            // insert a border
            icon.setInsets(new Insets(0, 0, 0, 0));
            return new NTxBounds2(rendererContext.parentBounds().getX(), rendererContext.parentBounds().getY(), icon.getIconWidth(), icon.getIconHeight());
        }
    }


    public void renderMain(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext builderContext) {
        NTxNode node = rendererContext.node();
        NElement vElemExpr = node.getPropertyValue(NTxPropName.VALUE).orNull();
        NElement vElemValue = rendererContext.engine().evalExpression(vElemExpr, node, rendererContext.varProvider());
        String text = NTxValue.of(vElemValue).asStringOrName().orElse("");

        NTxGraphics g = rendererContext.graphics();


        String tex = NStringUtils.trim(rendererContext.engine().tools().trimBloc(text));
        if (tex.isEmpty()) {
            NTxBounds2 selfBounds = rendererContext.selfBounds();
            double x = selfBounds.getX();
            double y = selfBounds.getY();
            if (!rendererContext.isDry()) {
                if (rendererContext.applyBackgroundColor(node)) {
                    g.fillRect((int) x, (int) y, NTxUtils.intOf(selfBounds.getWidth()), NTxUtils.intOf(selfBounds.getHeight()));
                }
            }
        } else {
            TeXFormula formula;
            boolean error = false;
            try {
                formula = new TeXFormula(tex);
            } catch (Exception ex) {
                error = true;
                formula = new TeXFormula("?error?");
                builderContext.engine().log().log(NMsg.ofC("error evaluating latex formula %s : %s", tex, ex), NTxUtils.sourceOf(node));
            }

            NTxTextOptions oo=new NTxTextOptions();
            oo.defaultFont=NTxValueByName.getFontInfo(node, rendererContext);
            oo.sr=rendererContext.sizeRef();
            Font font = oo.resolveFont(g);


            TeXIcon icon = formula.createTeXIcon(TeXConstants.STYLE_DISPLAY, font.getSize());

            // insert a border
            icon.setInsets(new Insets(0, 0, 0, 0));

            NTxBounds2 selfBounds = rendererContext.selfBounds((NTxNode) node
                    , new NTxDouble2(icon.getIconWidth(), icon.getIconHeight())
                    , null
            );
            double x = selfBounds.getX();
            double y = selfBounds.getY();

            if (!rendererContext.isDry()) {
                rendererContext.paintBackground(node, selfBounds);
                if (error) {
                    g.setColor(Color.RED);
                    g.fillRect(selfBounds);
                }
                Paint fg = rendererContext.getForegroundColor(node, true);
                icon.setForeground(rendererContext.colorFromPaint(fg).orElse(Color.BLACK));
                icon.paintIcon(null, g.graphics2D(), (int) x, (int) y /*- icon.getIconHeight()*/);

                rendererContext.paintBorderLine(node, selfBounds);
            }
        }
    }

    private void buildText(String text, NTxTextOptions options, NTxNode p, NTxNodeRendererContext rendererContext, NTxTextRendererBuilder builder, NTxNodeBuilderContext buildContext) {
        if (!text.isEmpty()) {
            NTxRichTextToken r = new NTxRichTextToken(
                    NTxRichTextTokenType.IMAGE_PAINTER,
                    text
            );
            options=options.copy();
            if(options.defaultFont==null){
                options.defaultFont=NTxValueByName.getFontInfo(p, rendererContext);
            }
            if(options.sr==null){
                options.sr=rendererContext.sizeRef();
            }
            Font font = options.resolveFont(rendererContext.graphics());
            r.imagePainter = this.createLatex(text, font.getSize(), options, p, rendererContext);
            NTxDouble2 size = r.imagePainter.size();
            r.bounds = new Rectangle2D.Double(0, 0, size.getX(), size.getX());
            builder.currRow().addToken(r);
        }
    }

    private List<NTxTextToken> parseTokens(NTxTextRendererFlavorParseContext rendererContext, NTxNodeBuilderContext buildContext) {
        return rendererContext.parseDefault(buildContext.idAndAliases(), new String[]{
                "\\(", "\\)"
        }, s -> {
            NExtendedLatexMathBuilder sb = new NExtendedLatexMathBuilder();
            sb.append(s);
            sb.flush();
            return sb.toString();
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
        float size = (float) (fontSize);
        TeXIcon icon = formula.createTeXIcon(TeXConstants.STYLE_DISPLAY, size);
        icon.setInsets(new Insets(0, 0, 0, 0));
        Color foregroundColor = null;
        if (options.foregroundColorIndex != null) {
            foregroundColor = NTxColors.resolveDefaultColorByIndex(options.foregroundColorIndex, null,node,ctx);
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
                FontMetrics fm = g.getFontMetrics(g.getFont());
                int texDepth = icon.getIconDepth(); // depth below baseline
                int texAscent = icon.getIconHeight() - texDepth;     // ascent above baseline
                double baseline = y + fm.getAscent();
                icon.setForeground(finalForegroundColor);
                icon.paintIcon(null, g.graphics2D(), (int) x, (int) (baseline - texAscent));
            }

            public NTxDouble2 size() {
                return new NTxDouble2(icon.getIconWidth(), icon.getIconHeight());
            }
        };
    }
}
