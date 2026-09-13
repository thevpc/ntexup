package net.thevpc.ntexup.engine.renderer.text;

import net.thevpc.ntexup.api.document.elem2d.*;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.renderer.text.NTxTextRendererFlavor;
import net.thevpc.ntexup.api.renderer.text.*;
import net.thevpc.ntexup.api.util.NTxSizeRef;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.api.util.NtxFontInfo;
import net.thevpc.ntexup.engine.util.NTx2DUtils0;
import net.thevpc.ntexup.engine.util.NTxNodeRendererUtils;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.engine.renderer.elem2d.text.util.NTxTextUtils;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NPairElement;
import net.thevpc.nuts.elem.NTupleElement;
import net.thevpc.nuts.text.NText;
import net.thevpc.nuts.text.NTextStyle;
import net.thevpc.nuts.text.NTextStyles;
import net.thevpc.nuts.util.NOptional;
import net.thevpc.nuts.util.NStringUtils;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NTxTextRendererBuilderImpl implements NTxTextRendererBuilder {

    public String lang;
    public String code;
    public List<NTxRichTextRow> rows = new ArrayList<>();
    private List<NTxRichTextRow> rawRows;
    public Rectangle2D.Double bounds;
    private Paint defaultColor;
    private NTxEngine engine;
    private NtxFontInfo defaultFont;

    public NTxTextRendererBuilderImpl(NTxEngine engine, Paint defaultColor, NtxFontInfo defaultFont) {
        this.defaultColor = defaultColor;
        this.engine = engine;
        this.defaultFont = defaultFont;
    }

    public void appendNText(String lang, String rawText, NText text, NTxRendererContext ctx) {
        NTxHighlighterMapper.highlightNutsText(lang, rawText, text, ctx, this);
    }

    @Override
    public void appendText(String rawText, NTxTextOptions options, NTxRendererContext ctx) {
        if (rawText == null || rawText.isEmpty()) {
            return;
        }
        rawRows = null;
        while (rawText.startsWith("\n")) {
            this.nextLine();
            rawText = rawText.substring(1);
        }
        int end = 0;
        while (rawText.endsWith("\n")) {
            rawText = rawText.substring(0, rawText.length() - 1);
            end++;
        }

        List<String> a = NStringUtils.split(rawText, "\n", false, false);

        NTxGraphics g = ctx.graphics();
        for (int j = 0; j < a.size(); j++) {
            if (j > 0) {
                this.nextLine();
            }
            List<String> chunks = NTxTextUtils.splitWordsAndSpaces(a.get(j));
            for (String chunk : chunks) {
                NTxRichTextToken c = new NTxRichTextToken(NTxRichTextTokenType.PLAIN, chunk);
                if (options != null) {
                    c.textOptions.copyNonNullFrom(options);
                }
                c.textOptions.defaultFont = defaultFont;
                c.textOptions.sr = ctx.sizeRef();
                g.setFont(c.textOptions.resolveFont(ctx.graphics(), true));
                c.bounds = g.getStringBounds(c.text);
                this.currRow().addToken(c);
            }
        }
        for (int i = 0; i < end; i++) {
            this.nextLine();
        }
    }

    @Override
    public void appendCustom(String lang, String rawText, NTxTextOptions options, NTxRendererContext ctx) {
        rawRows = null;
        if (rawText == null || rawText.isEmpty()) {
            return;
        }
        NTxTextRendererFlavor hTextRendererFlavor = engine.textRendererFlavor(lang).orElse(null);
        if (hTextRendererFlavor == null) {
            hTextRendererFlavor = engine.textRendererFlavor("").orElse(null);
        }
        if (hTextRendererFlavor != null) {
            hTextRendererFlavor.buildText(rawText, options, ctx, this);
        }
    }

    public void appendPlain(String text, NTxRendererContext ctx) {
        appendText(text, null, ctx);
    }


    public NTxRichTextRow nextLine() {
        rawRows = null;
        rows.add(new NTxRichTextRow());
        return rows.get(rows.size() - 1);
    }

    public NTxRichTextRow currRow() {
        if (rows.isEmpty()) {
            NTxRichTextRow e = new NTxRichTextRow();
            rows.add(e);
            return e;
        }
        return rows.get(rows.size() - 1);
    }

    public NTxBounds2D computeBound(NTxRendererContext ctx) {
        NTxGraphics g = ctx.graphics();
        Font oldFont = g.getFont();
        NtxFontInfo fontInfo = defaultFont;
        if (fontInfo == null) {
            fontInfo = NTxValueByName.getFontInfo(ctx);
        } else {
            NtxFontInfo fi = NTxValueByName.getFontInfo(ctx);
            if (fi != null) {
                fontInfo = fi.copy().applyDefaults(defaultFont);
            }
        }
        NTxTextOptions textOptions = new NTxTextOptions();
        textOptions.sr = ctx.sizeRef();

        if (rawRows == null) {
            rawRows = new ArrayList<>();
            for (NTxRichTextRow r : this.rows) {
                NTxRichTextRow cloneR = new NTxRichTextRow();
                cloneR.tokens.addAll(r.tokens);
                rawRows.add(cloneR);
            }
        }

        // Measure all tokens in rawRows
        for (NTxRichTextRow row : rawRows) {
            for (NTxRichTextToken c : row.tokens) {
                if (c.type == NTxRichTextTokenType.IMAGE_PAINTER && c.imagePainter != null) {
                    double bl = c.imagePainter.baseline();
                    c.ascent = bl;
                    c.descent = Math.max(0, c.imagePainter.size().getY() - bl);
                    if (c.bounds == null) {
                        NTxDouble2 size = c.imagePainter.size();
                        c.bounds = new Rectangle2D.Double(0, 0, size.getX(), size.getY());
                    }
                } else {
                    NTxTextOptions opt = textOptions.copy().copyNonNullFrom(c.textOptions);
                    opt.defaultFont = fontInfo;
                    opt.sr = ctx.sizeRef();
                    Font f = opt.resolveFont(ctx.graphics(), true);
                    FontMetrics fm = g.getFontMetrics(f);
                    c.ascent = fm.getAscent();
                    c.descent = fm.getDescent();
                    g.setFont(f);
                    c.bounds = g.getStringBounds(c.text);
                }
            }
        }

        NTxMargin padding = NTxValueByName.getPadding(ctx);
        double padW = padding == null ? 0 : (padding.getLeft() + padding.getRight());
        NTxMargin margin = NTxValueByName.getMargin(ctx);
        double marW = margin == null ? 0 : (margin.getLeft() + margin.getRight());

        double availableWidth = -1;
        NElement widthElem = ctx.node().getPropertyValue(NTxPropName.WIDTH).orNull();
        NElement sizeElem = ctx.node().getPropertyValue(NTxPropName.SIZE).orNull();
        if (widthElem != null) {
            availableWidth = ctx.sizeRef().x(widthElem).orElse(-1.0) - padW;
        } else if (sizeElem != null) {
            NOptional<NTxElemNumber2> s2 = NTxValue.of(sizeElem).asNNumberElement2Or1OrHAlign();
            if (s2.isPresent()) {
                availableWidth = ctx.sizeRef().x(s2.get().getX()).orElse(-1.0) - padW;
            }
        }
        NTxTextPath tp0 = parseNTxTextPath(ctx.computePropertyValue("text-path").orNull());
        if (tp0 != null && widthElem == null && sizeElem == null && tp0.curves.length > 0) {
            NTxSizeRef sr = ctx.sizeRef();
            List<Point2D> pts = Arrays.stream(tp0.curves[0].points)
                    .map(pp -> (Point2D) new Point2D.Double(sr.x(pp.x).get(), sr.y(pp.y).get()))
                    .collect(Collectors.toList());
            if (pts.size() >= 2) {
                List<Point2D> dense = NTx2DUtils0.interpolatePoints(pts.toArray(new Point2D[0]), 2.0);
                List<Double> lens = NTx2DUtils0.computeSegmentLengths(dense);
                double clen = lens.stream().mapToDouble(Double::doubleValue).sum();
                if (clen > 0) {
                    availableWidth = clen;
                }
            }
        }
        if (availableWidth <= 0) {
            availableWidth = ctx.parentBounds2D().widthX() - marW - padW;
        }
        if (availableWidth <= 0) {
            availableWidth = 10000;
        }

        NTxTextWrap textWrap = NTxValueByName.getTextWrap(ctx);
        NTxTextAlign textAlign = NTxValueByName.getTextAlign(ctx);

        List<NTxRichTextRow> finalRows = new ArrayList<>();
        if (textWrap == NTxTextWrap.WRAP) {
            for (NTxRichTextRow rawRow : rawRows) {
                if (rawRow.tokens.isEmpty()) {
                    NTxRichTextRow emptyRow = new NTxRichTextRow();
                    finalRows.add(emptyRow);
                    continue;
                }
                List<NTxRichTextRow> paragraphRows = new ArrayList<>();
                NTxRichTextRow curRow = new NTxRichTextRow();
                double curWidth = 0;

                for (int j = 0; j < rawRow.tokens.size(); j++) {
                    NTxRichTextToken tok = rawRow.tokens.get(j);
                    double tw = tok.bounds.getWidth();

                    if (curRow.tokens.isEmpty() && tok.isWhitespace()) {
                        continue;
                    }

                    if (curRow.tokens.isEmpty()) {
                        curRow.tokens.add(tok);
                        curWidth += tw;
                    } else if (curWidth + tw <= availableWidth) {
                        curRow.tokens.add(tok);
                        curWidth += tw;
                    } else {
                        // Overflow, wrap to new line
                        trimTrailingWhitespace(curRow);
                        paragraphRows.add(curRow);

                        curRow = new NTxRichTextRow();
                        curWidth = 0;
                        if (!tok.isWhitespace()) {
                            curRow.tokens.add(tok);
                            curWidth += tw;
                        }
                    }
                }
                if (!curRow.tokens.isEmpty()) {
                    trimTrailingWhitespace(curRow);
                    paragraphRows.add(curRow);
                }

                for (int pr = 0; pr < paragraphRows.size(); pr++) {
                    NTxRichTextRow r = paragraphRows.get(pr);
                    boolean isLastInParagraph = (pr == paragraphRows.size() - 1);
                    layoutRow(r, availableWidth, textAlign, isLastInParagraph);
                    finalRows.add(r);
                }
            }
        } else {
            // VERBATIM
            double maxRowWidth = 0;
            for (NTxRichTextRow r : rawRows) {
                trimTrailingWhitespace(r);
                double rw = 0;
                for (NTxRichTextToken tok : r.tokens) {
                    rw += tok.bounds.getWidth();
                }
                if (rw > maxRowWidth) {
                    maxRowWidth = rw;
                }
            }
            double targetW = (widthElem != null || sizeElem != null) ? availableWidth : maxRowWidth;
            for (int i = 0; i < rawRows.size(); i++) {
                NTxRichTextRow r = new NTxRichTextRow();
                r.tokens.addAll(rawRows.get(i).tokens);
                boolean isLastInParagraph = (rawRows.size() > 1 && i == rawRows.size() - 1);
                layoutRow(r, targetW, textAlign, isLastInParagraph);
                finalRows.add(r);
            }
        }
        this.rows = finalRows;

        bounds = new Rectangle2D.Double(0, 0, 0, 0);
        double maxxY = 0;
        FontMetrics defaultFm = g.getFontMetrics(oldFont);
        double defaultLineH = defaultFm.getAscent() + defaultFm.getDescent();

        for (int i = 0; i < rows.size(); i++) {
            NTxRichTextRow row = rows.get(i);
            if (row.tokens.isEmpty()) {
                row.maxAscent = defaultFm.getAscent();
                row.maxDescent = defaultFm.getDescent();
                row.textBounds = new Rectangle2D.Double(0, 0, 0, defaultLineH);
            }
            if (i == 0) {
                row.yOffset = 0;
            } else {
                row.yOffset = rows.get(i - 1).yOffset + rows.get(i - 1).textBounds.getHeight();
            }
            Rectangle2D.Double.union(bounds, row.textBounds, bounds);
            maxxY = row.yOffset + row.textBounds.getHeight();
        }
        g.setFont(oldFont);
        return NTxBounds2D.ofWidth(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), maxxY);
    }

    private void layoutRow(NTxRichTextRow row, double targetWidth, NTxTextAlign textAlign, boolean isLastInParagraph) {
        double maxAscent = 0;
        double maxDescent = 0;
        double lineWidth = 0;
        int spaceCount = 0;

        for (NTxRichTextToken tok : row.tokens) {
            maxAscent = Math.max(maxAscent, tok.ascent);
            maxDescent = Math.max(maxDescent, tok.descent);
            lineWidth += tok.bounds.getWidth();
            if (tok.isWhitespace()) {
                spaceCount++;
            }
        }
        row.maxAscent = maxAscent;
        row.maxDescent = maxDescent;

        double remaining = targetWidth - lineWidth;
        double startX = 0;
        double extraPerSpace = 0;

        if (textAlign == NTxTextAlign.JUSTIFY) {
            if (!isLastInParagraph && remaining > 0 && spaceCount > 0) {
                extraPerSpace = remaining / spaceCount;
                startX = 0;
            } else {
                startX = 0;
            }
        } else if (textAlign == NTxTextAlign.RIGHT) {
            if (remaining > 0) {
                startX = remaining;
            }
        } else if (textAlign == NTxTextAlign.CENTER) {
            if (remaining > 0) {
                startX = remaining / 2.0;
            }
        } else {
            startX = 0;
        }

        double curX = startX;
        for (NTxRichTextToken tok : row.tokens) {
            tok.xOffset = curX;
            double w = tok.bounds.getWidth();
            if (tok.isWhitespace()) {
                w += extraPerSpace;
            }
            curX += w;
        }

        double rowHeight = maxAscent + maxDescent;
        double actualRowWidth = (textAlign == NTxTextAlign.JUSTIFY && !isLastInParagraph) ? targetWidth : curX;
        row.textBounds = new Rectangle2D.Double(0, 0, actualRowWidth, rowHeight);
    }

    private void trimTrailingWhitespace(NTxRichTextRow row) {
        while (!row.tokens.isEmpty() && row.tokens.get(row.tokens.size() - 1).isWhitespace()) {
            row.tokens.remove(row.tokens.size() - 1);
        }
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public void setCode(String rawText) {
        this.code = rawText;
    }

    @Override
    public void addToken(NTxRichTextToken col) {
        rawRows = null;
        if ((col.type == NTxRichTextTokenType.PLAIN || col.type == NTxRichTextTokenType.STYLED)
                && col.text != null && (col.text.contains(" ") || col.text.contains("\t"))) {
            List<String> chunks = NTxTextUtils.splitWordsAndSpaces(col.text);
            if (chunks.size() > 1) {
                for (String chunk : chunks) {
                    NTxRichTextToken sub = new NTxRichTextToken(col.type, chunk);
                    sub.tok = col.tok;
                    sub.textOptions = col.textOptions;
                    sub.ascent = col.ascent;
                    sub.descent = col.descent;
                    sub.bounds = col.bounds;
                    currRow().tokens.add(sub);
                }
                return;
            }
        }
        currRow().tokens.add(col);
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    public void render(NTxNode p, NTxRendererContext rendererContext, NTxBounds2D bgBounds, NTxBounds2D selfBounds) {
        boolean debug = rendererContext.isDebug();
        double x = selfBounds.minX();
        double y = selfBounds.minY();
        NTxGraphics g0 = rendererContext.graphics();
        NtxFontInfo fontInfo = NTxValueByName.getFontInfo(rendererContext);
        if (fontInfo == null) {
            fontInfo = defaultFont == null ? new NtxFontInfo() : defaultFont.copy();
        } else {
            fontInfo = fontInfo.copy().applyDefaults(defaultFont);
        }
        NTxTextOptions textOptions = new NTxTextOptions()
//                .setFont(NTxValueByName.getFont(p, rendererContext))
                .setForegroundColor(NTxValueByName.getForegroundColor(rendererContext, true));


        NTxNodeRendererUtils.paintBackground(rendererContext, g0, bgBounds);
        NOptional<NTxShadow> shadowOptional = NTxValueByName.readStyleAsShadow(NTxPropName.SHADOW, rendererContext);
        if (shadowOptional.isPresent()) {
            NTxShadow shadow = shadowOptional.get().copy();
            textOptions.setShadow(shadow);
            if (shadow.getColor() == null) {
                if (textOptions.getForegroundColor() instanceof Color) {
                    shadow.setColor(((Color) textOptions.getForegroundColor()).darker());
                } else {
                    shadow.setColor(textOptions.getForegroundColor());
                }
            }
        }
        textOptions.sr = rendererContext.sizeRef();
        NTxTextPath tp = parseNTxTextPath(rendererContext.computePropertyValue("text-path").orNull());
        NTxMargin padding = NTxValueByName.getPadding(rendererContext);
        double padLeft = padding == null ? 0 : padding.getLeft();
        double padTop = padding == null ? 0 : padding.getTop();
        if (tp == null) {
            for (NTxRichTextRow row : this.rows) {
                double baselineY = (y + padTop + row.yOffset) + (row.maxAscent > 0 ? row.maxAscent : 0);
                for (NTxRichTextToken col : row.tokens) {
                    switch (col.type) {
                        case PLAIN:
                        case STYLED: {
                            NTxTextOptions options2 = textOptions.copy().copyNonNullFrom(col.textOptions);

                            options2.defaultFont = fontInfo;
                            options2.sr = textOptions.sr;
                            options2.resolveFont(rendererContext.graphics(), true);
                            int ascent = g0.getFontMetrics(options2.getComputedFont()).getAscent();
                            double tokenBaseline = row.maxAscent > 0 ? baselineY : ((y + padTop + row.yOffset) + ascent);
                            g0.drawString(
                                    col.text
                                    , x + padLeft + col.xOffset
                                    , tokenBaseline,
                                    options2
                            );
                            break;
                        }
                        case IMAGE_PAINTER: {
                            Rectangle2D b1 = col.bounds;
                            NTxDouble2 b2 = col.imagePainter.size();
                            double imgY = row.maxAscent > 0 ? (baselineY - col.ascent) : (y + padTop + row.yOffset);
                            col.imagePainter.paint(g0, (x + padLeft + col.xOffset), imgY);
                            if (debug) {
                                g0.drawRect(
                                        x + padLeft + col.xOffset,
                                        imgY,
                                        col.bounds.getWidth(),
                                        col.bounds.getHeight()
                                );
                            }
                            break;
                        }
                    }
                }
            }
        } else {
            NTxSizeRef sr = rendererContext.sizeRef();

            textOptions.defaultFont = fontInfo;
            textOptions.sr = rendererContext.sizeRef();
            Font font = textOptions.resolveFont(rendererContext.graphics(), true);
            FontMetrics fm = rendererContext.graphics().getFontMetrics(font);
            double lineHeight = fm.getAscent() + fm.getDescent() + fm.getLeading();
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                List<Point2D> baseCurve = null;
                int baseIndex = 0;
                if (rowIndex < tp.curves.length) {
                    baseCurve = Arrays.stream(tp.curves[rowIndex].points).map(pp -> {
                        double px = x + padLeft + sr.x(pp.x).orElse(0.0);
                        double py = y + padTop + sr.y(pp.y).orElse(0.0);
                        return (Point2D) new Point2D.Double(px, py);
                    }).collect(Collectors.toList());
                    baseIndex = 0;
                } else {
                    baseIndex = rowIndex - tp.curves.length + 1;
                    baseCurve = Arrays.stream(tp.curves[tp.curves.length - 1].points).map(pp -> {
                        double px = x + padLeft + sr.x(pp.x).orElse(0.0);
                        double py = y + padTop + sr.y(pp.y).orElse(0.0);
                        return (Point2D) new Point2D.Double(px, py);
                    }).collect(Collectors.toList());
                }

                NTxRichTextRow row = rows.get(rowIndex);
                // get curve for this row (translate, scale, zoom)

                List<Point2D> rowCurve = computeRowCurve(baseCurve, baseIndex, lineHeight, 1);
                List<Point2D> denseCurve = rowCurve.size() >= 2 ? NTx2DUtils0.interpolatePoints(rowCurve.toArray(new Point2D[0]), 2.0) : rowCurve;
                if(debug) {
                    rendererContext.graphics().setColor(Color.RED);
                    rendererContext.graphics().drawPolyline(
                            denseCurve.stream().mapToDouble(pp -> pp.getX()).toArray(),
                            denseCurve.stream().mapToDouble(pp -> pp.getY()).toArray(),
                            denseCurve.size()
                    );
                }

                List<Double> segmentLengths = NTx2DUtils0.computeSegmentLengths(denseCurve);

                // render tokens along this row curve
                renderRowAlongCurve(row, denseCurve, segmentLengths, textOptions, rendererContext, fontInfo, NTxValueByName.getTextAlign(rendererContext));
            }
        }
        rendererContext.drawContour();
    }

    public void renderRowAlongCurve(NTxRichTextRow row, List<Point2D> curvePoints, List<Double> segmentLengths, NTxTextOptions baseOptions, NTxRendererContext rendererContext, NtxFontInfo fontInfo, NTxTextAlign textAlign) {
        double curveLength = 0;
        for (double l : segmentLengths) {
            curveLength += l;
        }
        List<NTxRichTextToken> tokens = new ArrayList<>(row.tokens);
        while (!tokens.isEmpty() && tokens.get(tokens.size() - 1).isWhitespace()) {
            tokens.remove(tokens.size() - 1);
        }
        double rowWidth = 0;
        int spaceCount = 0;
        int charCount = 0;
        for (NTxRichTextToken token : tokens) {
            double tokenWidth = computeTokenWidth(token, rendererContext);
            rowWidth += tokenWidth;
            if (token.isWhitespace()) {
                spaceCount++;
            } else if (token.text != null) {
                charCount += token.text.length();
            } else {
                charCount++;
            }
        }

        double s = 0;
        double extraPerSpace = 0;
        double extraPerChar = 0;
        double remaining = curveLength - rowWidth;
        if (textAlign == NTxTextAlign.CENTER) {
            if (remaining > 0) {
                s = remaining / 2.0;
            }
        } else if (textAlign == NTxTextAlign.RIGHT) {
            if (remaining > 0) {
                s = remaining;
            }
        } else if (textAlign == NTxTextAlign.JUSTIFY) {
            if (remaining > 0) {
                if (spaceCount > 0) {
                    extraPerSpace = remaining / spaceCount;
                } else if (charCount > 1) {
                    extraPerChar = remaining / (charCount - 1);
                }
            }
        }

        for (NTxRichTextToken token : tokens) {
            if (token.isWhitespace()) {
                double tokenWidth = computeTokenWidth(token, rendererContext);
                s += tokenWidth + extraPerSpace;
                continue;
            }

            token.textOptions.defaultFont = fontInfo;
            token.textOptions.sr = rendererContext.sizeRef();
            token.textOptions.resolveFont(rendererContext.graphics(), true);

            double tokenWidth = computeTokenWidth(token, rendererContext);

            if (token.type == NTxRichTextTokenType.PLAIN || token.type == NTxRichTextTokenType.STYLED) {
                NTxTextOptions options2 = baseOptions.copy().copyNonNullFrom(token.textOptions);
                options2.defaultFont = fontInfo;
                options2.sr = rendererContext.sizeRef();
                options2.resolveFont(rendererContext.graphics(), true);
                drawGlyphVectorAlongCurve(token.text, curvePoints, segmentLengths, s, options2, rendererContext, extraPerChar);
                int nChars = token.text != null ? token.text.length() : 0;
                s += tokenWidth + (nChars > 0 ? (nChars * extraPerChar) : 0);
            } else if (token.type == NTxRichTextTokenType.IMAGE_PAINTER) {
                Point2D pos = NTx2DUtils0.getPointAtLength(curvePoints, segmentLengths, s + tokenWidth / 2);
                double angle = NTx2DUtils0.getTangentAngle(curvePoints, segmentLengths, s + tokenWidth / 2);

                AffineTransform at = new AffineTransform();
                at.translate(pos.getX(), pos.getY());
                at.rotate(angle);
                at.translate(-tokenWidth / 2, 0);

                NTxGraphics g = rendererContext.graphics();
                Graphics2D g2d = g.graphics2D();
                AffineTransform old = g2d.getTransform();
                g2d.transform(at);
                token.imagePainter.paint(g, 0, -token.ascent);
                g2d.setTransform(old);

                s += tokenWidth + extraPerChar;
            }
        }
    }

    private void drawGlyphVectorAlongCurve(
            String text,
            List<Point2D> curvePoints,
            List<Double> segmentLengths,
            double tokenStartS,
            NTxTextOptions options,
            NTxRendererContext rendererContext,
            double extraPerChar) {

        if (text == null || text.isEmpty()) return;

        Graphics2D g = rendererContext.graphics().graphics2D();
        Font font = options.getComputedFont();
        GlyphVector gv = font.createGlyphVector(g.getFontRenderContext(), text);

        Paint oldPaint = g.getPaint();
        Font oldFont = g.getFont();
        g.setFont(font);
        g.setPaint(options.getForegroundColor());

        int numGlyphs = gv.getNumGlyphs();
        for (int i = 0; i < numGlyphs; i++) {
            Shape glyph = gv.getGlyphOutline(i);
            Point2D glyphPos = gv.getGlyphPosition(i);
            double advance = gv.getGlyphMetrics(i).getAdvance();
            double sCenter = tokenStartS + glyphPos.getX() + (i * extraPerChar) + advance / 2.0;

            Point2D pos = NTx2DUtils0.getPointAtLength(curvePoints, segmentLengths, sCenter);
            double angle = NTx2DUtils0.getTangentAngle(curvePoints, segmentLengths, sCenter);

            AffineTransform at = new AffineTransform();
            at.translate(pos.getX(), pos.getY());
            at.rotate(angle);
            at.translate(-glyphPos.getX() - advance / 2.0, 0);
            g.fill(at.createTransformedShape(glyph));
        }

        g.setFont(oldFont);
        g.setPaint(oldPaint);
    }

    private double computeTokenWidth(NTxRichTextToken token, NTxRendererContext rendererContext) {
        if (token.bounds != null) {
            return token.bounds.getWidth();
        }
        if (token.type == NTxRichTextTokenType.PLAIN || token.type == NTxRichTextTokenType.STYLED) {
            Font font = token.textOptions.getComputedFont();
            if (font == null) {
                token.textOptions.resolveFont(rendererContext.graphics(), true);
                font = token.textOptions.getComputedFont();
            }
            FontMetrics fm = rendererContext.graphics().getFontMetrics(font);
            return fm.stringWidth(token.text);
        } else if (token.type == NTxRichTextTokenType.IMAGE_PAINTER) {
            if (token.imagePainter != null) {
                return token.imagePainter.size().getX();
            }
            return 0;
        } else {
            return 0;
        }
    }

    public List<Point2D> computeRowCurve(List<Point2D> baseCurve, int rowIndex, double lineHeight, double zoomFactor) {
        List<Point2D> rowCurve = new ArrayList<>();
        double scale = /*1.0 + rowIndex */ zoomFactor;

        for (Point2D p : baseCurve) {
            double x = p.getX();
            double y = p.getY() + rowIndex * lineHeight; // vertical offset
            x *= scale;
            y *= scale;
            rowCurve.add(new Point2D.Double(x, y));
        }
        return rowCurve;
    }

    private static NTxTextPath parseNTxTextPath(NElement e) {
        if (e == null) {
            return null;
        }
        if (e.isNumber()) {
            return null;
        }
        List<NTxTextPathCurve> curvesOk = new ArrayList<>();
        if (e.isNamedTuple()) {
            NOptional<NTxTextPathCurve> cc = parseCurve(e);
            if (cc.isPresent()) {
                curvesOk.add(cc.get());
            }
        } else if (e.isObject()) {
            NOptional<NTxPoint2D[]> uu = NTxValue.of(e).asPoint2DArray();
            boolean found = false;
            if (uu.isPresent()) {
                NTxPoint2D[] g = uu.get();
                if (g.length >= 2) {
                    found = true;
                    curvesOk.add(new NTxTextPathCurve(g));
                }
            }
            if (!found) {
                for (NElement child : e.asObject().get().children()) {
                    NOptional<NTxTextPathCurve> cc = parseCurve(child);
                    if (cc.isPresent()) {
                        curvesOk.add(cc.get());
                    }
                }
            }
        } else if (e.isArray()) {
            NOptional<NTxPoint2D[]> uu = NTxValue.of(e).asPoint2DArray();
            boolean found = false;
            if (uu.isPresent()) {
                NTxPoint2D[] g = uu.get();
                if (g.length >= 2) {
                    found = true;
                    curvesOk.add(new NTxTextPathCurve(g));
                }
            }
            if (!found) {
                for (NElement child : e.asArray().get().children()) {
                    NOptional<NTxTextPathCurve> cc = parseCurve(child);
                    if (cc.isPresent()) {
                        curvesOk.add(cc.get());
                    }
                }
            }
        } else {
            //error here
        }
        if (!curvesOk.isEmpty()) {
            NTxTextPath pp = new NTxTextPath();
            pp.curves = curvesOk.toArray(new NTxTextPathCurve[0]);
            return pp;
        }
        return null;
    }

    private static NOptional<NTxTextPathCurve> parseCurve(NElement child) {
        if (child.isNamedTuple()) {
            NTupleElement u = child.asTuple().get();
            String name = u.name().get();
            switch (name) {
                case "points":
                case "curve": {
                    NOptional<NTxPoint2D[]> uu = NTxValue.of(u).asPoint2DArray();
                    if (uu.isPresent()) {
                        NTxPoint2D[] g = uu.get();
                        if (g.length >= 2) {
                            return NOptional.of(new NTxTextPathCurve(g));
                        }
                    }
                    return NOptional.ofNamedEmpty("missing curve");
                }
                case "arc": {

                    Double cx = null;
                    Double cy = null;
                    Double rx = null;
                    Double ry = null;
                    Double startAngle = null;
                    Double endAngle = null;
                    Integer points = null;

                    for (NElement param : u.params()) {
                        if (param.isNamedPair()) {
                            NPairElement p = param.asPair().get();
                            switch (p.key().asStringValue().get()) {
                                case "cx":
                                case "x": {
                                    NOptional<Double> g = p.value().asDoubleValue();
                                    if (g.isPresent()) {
                                        cx = g.get();
                                    }
                                    break;
                                }
                                case "y":
                                case "cy": {
                                    NOptional<Double> g = p.value().asDoubleValue();
                                    if (g.isPresent()) {
                                        cy = g.get();
                                    }
                                    break;
                                }
                                case "rx": {
                                    NOptional<Double> g = p.value().asDoubleValue();
                                    if (g.isPresent()) {
                                        rx = g.get();
                                    }
                                    break;
                                }
                                case "ry": {
                                    NOptional<Double> g = p.value().asDoubleValue();
                                    if (g.isPresent()) {
                                        ry = g.get();
                                    }
                                    break;
                                }
                                case "startAngle":
                                case "from": {
                                    NOptional<Double> g = p.value().asDoubleValue();
                                    if (g.isPresent()) {
                                        startAngle = g.get();
                                    }
                                    break;
                                }
                                case "toAngle":
                                case "to": {
                                    NOptional<Double> g = p.value().asDoubleValue();
                                    if (g.isPresent()) {
                                        endAngle = g.get();
                                    }
                                    break;
                                }
                                case "count": {
                                    NOptional<Integer> g = p.value().asIntValue();
                                    if (g.isPresent()) {
                                        points = g.get();
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    if (cx == null && cy == null) {
                        return NOptional.ofNamedEmpty("missing arc");
                    }
                    if (rx == null && rx == null) {
                        return NOptional.ofNamedEmpty("missing arc");
                    }
                    if (cx == null) {
                        cx = cy;
                    }
                    if (cy == null) {
                        cy = cx;
                    }
                    if (rx == null) {
                        rx = ry;
                    }
                    if (ry == null) {
                        ry = rx;
                    }
                    if (startAngle == null) {
                        startAngle = 0.0;
                    }
                    if (endAngle == null) {
                        endAngle = 360.0;
                    }
                    if (points == null) {
                        points = 360;
                    }
                    if (points <= 2) {
                        points = 100;
                    }
                    List<NTxPoint2D> arc = NTxUtils.createArc2(cx, cy, rx, ry, startAngle, endAngle, points);
                    Collections.reverse(arc);
                    return NOptional.of(new NTxTextPathCurve(arc.toArray(new NTxPoint2D[0])));
                }
            }
        } else if (child.isArray() || child.isObject()) {
            NOptional<NTxPoint2D[]> uu = NTxValue.of(child).asPoint2DArray();
            if (uu.isPresent()) {
                NTxPoint2D[] g = uu.get();
                if (g.length >= 2) {
                    return NOptional.of(new NTxTextPathCurve(g));
                }
            }
            return NOptional.ofNamedEmpty("missing array");
        }
        return NOptional.ofNamedEmpty("missing curve");
    }

    private static class NTxTextPathCurve {
        private NTxPoint2D[] points;

        public NTxTextPathCurve(NTxPoint2D[] points) {
            this.points = points;
        }
    }

    private static class NTxTextPath {
        private NTxTextPathCurve[] curves;
    }
}
