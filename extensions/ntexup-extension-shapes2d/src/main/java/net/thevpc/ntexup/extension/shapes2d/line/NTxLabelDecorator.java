package net.thevpc.ntexup.extension.shapes2d.line;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.elem2d.NTxSize;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.eval.NTxValueByType;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.renderer.text.NTxTextOptions;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NPairElement;
import net.thevpc.nuts.elem.NParamOrChild;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Paint;
import java.util.List;

public class NTxLabelDecorator {
    public static void draw(NTxRendererContext rendererContext, NTxGraphics g, NTxRoutedPath path) {
        NTxNode node = rendererContext.node();
        NElement labelElem = node.getPropertyValue("label").orNull();
        if (labelElem == null) {
            labelElem = node.getPropertyValue("text").orNull();
        }
        if (labelElem == null) {
            return;
        }
        labelElem = rendererContext.evalExpression(labelElem).orElse(labelElem);
        String text = null;
        double position = 50.0;
        Double offsetPerp = null;
        NTxSize offsetPerpSize = null;
        NTxPoint2D offset2d = null;
        NTxSize fontSize = null;
        Paint fg = null;
        Boolean bold = null;
        Boolean italic = null;
        String fontFamily = null;

        if (labelElem.isAnyString()) {
            text = labelElem.asStringValue().orNull();
        } else if (labelElem.isListOrParametrizedContainer()) {
            for (NParamOrChild c : labelElem.asListOrParametrizedContainer().get().paramsOrChildren()) {
                NElement ce = c.element();
                if (rendererContext != null) {
                    ce = rendererContext.evalExpression(ce).orElse(ce);
                }
                if (ce.isAnyString()) {
                    text = ce.asStringValue().orNull();
                } else if (ce.isNamedPair()) {
                    NPairElement p = ce.asNamedPair().get();
                    String key = NTxUtils.uid(p.key().asStringValue().orNull());
                    NElement pval = p.value();
                    if (rendererContext != null) {
                        pval = rendererContext.evalExpression(pval).orElse(pval);
                    }
                    switch (key) {
                        case "text":
                        case "value":
                            text = pval.asStringValue().orNull();
                            break;
                        case "position":
                        case "pos":
                            position = pval.asDoubleValue().orElse(50.0);
                            break;
                        case "offset": {
                            if (pval.isNumber()) {
                                try {
                                    offsetPerpSize = NTxSize.ofElement(pval);
                                } catch (Exception ignored) {
                                    offsetPerp = pval.asDoubleValue().orElse(0.0);
                                }
                            } else if (pval.isListOrParametrizedContainer()) {
                                List<NParamOrChild> items = pval.asListOrParametrizedContainer().get().paramsOrChildren();
                                if (items.size() == 1) {
                                    try {
                                        offsetPerpSize = NTxSize.ofElement(items.get(0).element());
                                    } catch (Exception ignored) {
                                        offsetPerp = items.get(0).element().asDoubleValue().orElse(0.0);
                                    }
                                } else if (items.size() >= 2) {
                                    NElement e0 = items.get(0).element();
                                    NElement e1 = items.get(1).element();
                                    if (e0.isNumber()) {
                                        position = e0.asDoubleValue().orElse(50.0);
                                    }
                                    try {
                                        offsetPerpSize = NTxSize.ofElement(e1);
                                    } catch (Exception ignored) {
                                        offsetPerp = e1.asDoubleValue().orElse(0.0);
                                    }
                                }
                            }
                            break;
                        }
                        case "color":
                        case "fg":
                        case "foreground":
                            fg = NTxValue.of(pval).asPaint().orNull();
                            break;
                        case "font-size":
                        case "size":
                        case "fs":
                            try {
                                fontSize = NTxSize.ofElement(pval);
                            } catch (Exception e) {
                                if (pval.isNumber()) {
                                    fontSize = NTxSize.ofPage(pval.asDoubleValue().orElse(2.0));
                                }
                            }
                            break;
                        case "bold":
                            bold = pval.asBooleanValue().orNull();
                            break;
                        case "italic":
                            italic = pval.asBooleanValue().orNull();
                            break;
                        case "font-family":
                            fontFamily = pval.asStringValue().orNull();
                            break;
                    }
                }
            }
        }

        // fix: resolve line-label-position before computing anchor; keep the
        // old precedence so an explicit label position wins
        if (position == 50.0 && rendererContext != null) {
            NElement pe = rendererContext.computePropertyValue("line-label-position", "label-position", "label-pos").orNull();
            if (pe != null && pe.isNumber()) {
                position = pe.asDoubleValue().orElse(position);
            }
        }
        if (offsetPerpSize == null && offset2d == null && offsetPerp == null && rendererContext != null) {
            NElement oe = rendererContext.computePropertyValue("line-label-offset", "label-offset").orNull();
            if (oe != null && oe.isNumber()) {
                try {
                    offsetPerpSize = NTxSize.ofElement(oe);
                } catch (Exception ignored) {
                    offsetPerp = oe.asDoubleValue().orElse(0.0);
                }
            }
        }
        if (fontSize == null && rendererContext != null) {
            NElement fse = rendererContext.computePropertyValue("line-label-font-size", "line-label-size", "line-label-fs", "label-font-size", "label-size", "label-fs").orNull();
            if (fse == null) {
                fse = rendererContext.computePropertyValue("font-size", "size", "fs").orNull();
            }
            if (fse != null) {
                try {
                    fontSize = NTxSize.ofElement(fse);
                } catch (Exception ignored) {
                    if (fse.isNumber()) {
                        fontSize = NTxSize.ofPage(fse.asDoubleValue().orElse(2.0));
                    }
                }
            }
        }
        if (bold == null && rendererContext != null) {
            bold = NTxValueByType.getBoolean(rendererContext, "line-label-bold", "line-label-font-bold", "label-bold", "label-font-bold", "bold", "font-bold").orNull();
        }
        if (italic == null && rendererContext != null) {
            italic = NTxValueByType.getBoolean(rendererContext, "line-label-italic", "line-label-font-italic", "label-italic", "label-font-italic", "italic", "font-italic").orNull();
        }
        if (fontFamily == null && rendererContext != null) {
            fontFamily = NTxValueByType.getStringOrName(rendererContext, "line-label-font-family", "label-font-family", "font-family").orNull();
        }
        if (fg == null && rendererContext != null) {
            fg = NTxValueByType.getPaint(rendererContext, "line-label-color", "line-label-fg", "label-color", "label-fg").orNull();
        }

        if (text == null || text.trim().isEmpty()) {
            return;
        }

        double len = path.length();
        double t = position / 100.0;
        double s = t * len;
        NTxPoint2D pt = path.pointAtExtended(s);
        NTxPoint2D nrm = path.normalAt(s);
        double lx = pt.x;
        double ly = pt.y;

        NTxTextOptions textOptions = new NTxTextOptions();
        textOptions.defaultFont = NTxValueByName.getFontInfo(rendererContext);
        textOptions.sr = rendererContext.sizeRef();
        if (fontSize != null) {
            textOptions.fontSize = fontSize;
        } else if (textOptions.defaultFont != null && textOptions.defaultFont.size != null) {
            textOptions.fontSize = textOptions.defaultFont.size;
        } else {
            textOptions.fontSize = NTxSize.ofPage(2.0);
        }
        textOptions.bold = !Boolean.FALSE.equals(bold);
        if (Boolean.TRUE.equals(italic)) {
            textOptions.italic = true;
        }
        if (fontFamily != null && !fontFamily.isEmpty()) {
            textOptions.fontFamily = fontFamily;
        }
        Paint linePaint = rendererContext.getLineColor(true);
        textOptions.foregroundColor = fg != null ? fg : (linePaint != null ? linePaint : Color.BLACK);

        Font f = textOptions.resolveFont(g, true);
        if (f == null) {
            f = new Font("SansSerif", Font.BOLD, 14);
        }
        g.setFont(f);

        double dPerp = 0;
        boolean hasOffsetPerp = false;
        if (offsetPerpSize != null) {
            hasOffsetPerp = true;
            switch (offsetPerpSize.type()) {
                case PARENT: {
                    dPerp = (offsetPerpSize.value() / 100.0) * len;
                    break;
                }
                case PAGE: {
                    double pageSize = rendererContext != null ? rendererContext.globalBounds2D().widthX() : 800;
                    dPerp = (offsetPerpSize.value() / 100.0) * pageSize;
                    break;
                }
                case PX:
                case REM: {
                    dPerp = offsetPerpSize.size();
                    break;
                }
                case BOUNDS: {
                    dPerp = offsetPerpSize.width();
                    break;
                }
            }
        } else if (offsetPerp != null) {
            hasOffsetPerp = true;
            dPerp = offsetPerp;
        }

        if (offset2d != null) {
            lx += offset2d.x;
            ly += offset2d.y;
        } else if (hasOffsetPerp) {
            lx += nrm.x * dPerp;
            ly += nrm.y * dPerp;
        } else {
            lx += nrm.x * (f.getSize2D() * 0.7);
            ly += nrm.y * (f.getSize2D() * 0.7);
        }

        FontMetrics fm = g.getFontMetrics(f);
        java.awt.geom.Rectangle2D bounds = fm.getStringBounds(text, g.graphics2D());
        double sw = bounds.getWidth();
        double sh = bounds.getHeight();
        double tx = lx - sw / 2.0;
        double ty = ly + fm.getAscent() - sh / 2.0;

        g.drawString(text, tx, ty, textOptions);
    }
}