package net.thevpc.ntexup.extension.plantuml;

import net.sourceforge.plantuml.SourceStringReader;
import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NStringBuilder;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import net.thevpc.nuts.elem.NElement;


public abstract class PlantUmlBuilderBase implements NTxNodeBuilder {
    private static final String DIAGRAM_CACHE_KEY = "ntexup-plantuml-diagram";

    private final String id;
    private String mode;
    private final String[] aliases;
    private final NTxProperties defaultStyles = new NTxProperties();

    public PlantUmlBuilderBase(String id) {
        this("plantuml-" + id, id);
    }

    public PlantUmlBuilderBase(String id, String... aliases) {
        this(id, id, aliases);
    }

    public PlantUmlBuilderBase(String id, String mode, String[] aliases) {
        this.id = id;
        this.mode = mode;
        this.aliases = aliases;
        this.mode = id;
    }

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext.id(id)
                .alias(aliases)
                .parseParam().matchesNamedPair(NTxPropName.VALUE, NTxPropName.FILE).then()
                .parseParam().matchesAnyNonPair().storeFirstMissingName(NTxPropName.VALUE).then()
                .renderComponent(this::renderMain)
        ;
    }


    public void renderMain(NTxRendererContext rendererContext) {
        NTxNode node = rendererContext.node();
        rendererContext = rendererContext.withDefaultStyles(defaultStyles);
        String txt = NTxValue.of(node.getPropertyValue(NTxPropName.VALUE).orNull()).asStringOrName().orNull();
        if (NBlankable.isBlank(txt)) {
            return;
        }
        String mode = NTxUtils.uid(this.mode);
        NTxGraphics g = rendererContext.graphics();
        NTxBounds2D b = rendererContext.selfBounds2D();
        double x = b.minX();
        double y = b.minY();
        BufferedImage image = null;
        String plantUMLText = null;
        if (mode.startsWith("plantuml-")) {
            mode = mode.substring("plantuml-".length());
        }
        switch (mode) {
            case "": {
                plantUMLText = txt;
                break;
            }
            case "uml":
            case "json":
            case "yaml":
            case "ebnf":
            case "regex":
            case "ditaa":
            case "gantt":
            case "chronology":
            case "mindmap":
            case "wbs":
            case "chen":
            case "math":
            case "latex": {
                plantUMLText = prepare(mode, txt, b, rendererContext);
                break;
            }
            case "nwdiag": {
                plantUMLText = prepare("uml",
                        "nwdiag {\n"
                                + txt
                                + "\n}"
                        , b, rendererContext);
                break;
            }
            case "salt":
            case "wireframe": {
                plantUMLText = prepare("salt", txt, b, rendererContext);
                break;
            }
        }
        if (plantUMLText != null) {
            image = renderDiagramImage(plantUMLText, node,
                    NTxUtils.intOf(b.widthX()), NTxUtils.intOf(b.widthY()), rendererContext);
        }
        if (image != null) {

            if (!rendererContext.isDry()) {
                if (rendererContext.applyBackgroundColor()) {
                    g.fillRect((int) x, (int) y, NTxUtils.intOf(b.widthX()), NTxUtils.intOf(b.widthY()));
                }

                rendererContext.applyForeground(false);
                if (image != null) {
                    // fit inside the box, keep the diagram's aspect ratio
                    int w = NTxUtils.intOf(b.widthX());
                    int h = NTxUtils.intOf(b.widthY());
                    if (w > 0 && h > 0) {
                        int iw = Math.max(1, image.getWidth());
                        int ih = Math.max(1, image.getHeight());
                        double scale = Math.min((double) w / iw, (double) h / ih);
                        int dw = Math.max(1, (int) Math.round(iw * scale));
                        int dh = Math.max(1, (int) Math.round(ih * scale));
                        BufferedImage resized = (dw == iw && dh == ih)
                                ? image
                                : rendererContext.engine().tools().resizeBufferedImage(image, dw, dh);
                        int dx = (int) Math.round(x + (w - dw) / 2.0);
                        int dy = (int) Math.round(y + (h - dh) / 2.0);
                        g.drawImage(resized, dx, dy, null);
                    }
                }

            }
            rendererContext.drawContour();
        }
    }

    static BufferedImage renderDiagramImage(String source, NTxNode node, int width, int height,
                                            NTxRendererContext rendererContext) {
        Object cacheValue = node.getRenderCache(DIAGRAM_CACHE_KEY).orNull();
        DiagramCacheEntry cached = cacheValue instanceof SoftReference
                ? (DiagramCacheEntry) ((SoftReference<?>) cacheValue).get() : null;
        if (cached != null && cached.matches(source, width, height)) {
            return cached.image;
        }
        SourceStringReader reader = new SourceStringReader(source);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            net.sourceforge.plantuml.core.DiagramDescription desc = reader.outputImage(bos);
            if (desc != null && desc.getDescription() != null
                    && desc.getDescription().toLowerCase().contains("error")) {
                // PlantUML still returns an image (its red/green error page): report it,
                // otherwise check/pdf exit cleanly with a broken diagram in the output
                NTxSource src = NTxUtils.sourceOf(node);
                rendererContext.log(NMsg.ofC("%s: PlantUML reports a syntax error in the diagram source",
                        node.type()).asSevere(), src);
            }
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bos.toByteArray()));
            if (image != null) {
                node.setRenderCache(DIAGRAM_CACHE_KEY,
                        new SoftReference<>(new DiagramCacheEntry(source, width, height, image)));
            }
            return image;
        } catch (Exception ex) {
            NTxSource src = NTxUtils.sourceOf(node);
            rendererContext.log(NMsg.ofC("Unable to evaluate UML : %s", ex).asSevere(), src);
            return null;
        }
    }

    private static final class DiagramCacheEntry {
        private final String source;
        private final int width;
        private final int height;
        private final BufferedImage image;

        private DiagramCacheEntry(String source, int width, int height, BufferedImage image) {
            this.source = source;
            this.width = width;
            this.height = height;
            this.image = image;
        }

        private boolean matches(String candidateSource, int candidateWidth, int candidateHeight) {
            return source.equals(candidateSource) && width == candidateWidth && height == candidateHeight;
        }
    }

    private boolean isDarkTheme(NTxRendererContext rendererContext) {
        if (rendererContext == null) {
            return false;
        }
        NElement bgVal = rendererContext.getVarValue("documentBg").orNull();
        if (bgVal != null && bgVal.isString()) {
            String s = bgVal.asStringValue().get().trim();
            if (s.startsWith("#") && (s.length() == 7 || s.length() == 9)) {
                try {
                    Color c = Color.decode(s.substring(0, 7));
                    double lum = 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
                    return lum < 128;
                } catch (Exception ignore) {
                }
            }
        }
        NElement txtVal = rendererContext.getVarValue("documentTextPrimary").orNull();
        if (txtVal != null && txtVal.isString()) {
            String s = txtVal.asStringValue().get().trim();
            if (s.startsWith("#") && (s.length() == 7 || s.length() == 9)) {
                try {
                    Color c = Color.decode(s.substring(0, 7));
                    double lum = 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
                    return lum > 128;
                } catch (Exception ignore) {
                }
            }
        }
        return false;
    }

    private void appendDarkSkinparams(NStringBuilder out) {
        out.println("skinparam defaultFontColor #e6edf3");
        out.println("skinparam ArrowColor #b4bdd0");
        out.println("skinparam ActivityBorderColor #b4bdd0");
        out.println("skinparam ActivityBackgroundColor #21262d");
        out.println("skinparam ActivityFontColor #e6edf3");
        out.println("skinparam ActivityDiamondBorderColor #b4bdd0");
        out.println("skinparam ActivityDiamondBackgroundColor #21262d");
        out.println("skinparam ActivityDiamondFontColor #e6edf3");
        out.println("skinparam UsecaseBorderColor #b4bdd0");
        out.println("skinparam UsecaseBackgroundColor #21262d");
        out.println("skinparam UsecaseFontColor #e6edf3");
        out.println("skinparam ActorBorderColor #b4bdd0");
        out.println("skinparam ActorBackgroundColor #21262d");
        out.println("skinparam ActorFontColor #e6edf3");
        out.println("skinparam ClassBorderColor #b4bdd0");
        out.println("skinparam ClassBackgroundColor #21262d");
        out.println("skinparam ClassFontColor #e6edf3");
        out.println("skinparam ClassHeaderBackgroundColor #30363d");
        out.println("skinparam ComponentBorderColor #b4bdd0");
        out.println("skinparam ComponentBackgroundColor #21262d");
        out.println("skinparam ComponentFontColor #e6edf3");
        out.println("skinparam InterfaceBorderColor #b4bdd0");
        out.println("skinparam InterfaceBackgroundColor #21262d");
        out.println("skinparam InterfaceFontColor #e6edf3");
        out.println("skinparam NodeBorderColor #b4bdd0");
        out.println("skinparam NodeBackgroundColor #21262d");
        out.println("skinparam NodeFontColor #e6edf3");
        out.println("skinparam PackageBorderColor #b4bdd0");
        out.println("skinparam PackageBackgroundColor #161b22");
        out.println("skinparam PackageFontColor #e6edf3");
        out.println("skinparam RectangleBorderColor #b4bdd0");
        out.println("skinparam RectangleBackgroundColor #21262d");
        out.println("skinparam RectangleFontColor #e6edf3");
        out.println("skinparam SequenceLifeLineBorderColor #b4bdd0");
        out.println("skinparam SequenceLifeLineBackgroundColor #21262d");
        out.println("skinparam SequenceGroupBorderColor #b4bdd0");
        out.println("skinparam SequenceGroupBackgroundColor #161b22");
        out.println("skinparam SequenceGroupFontColor #e6edf3");
        out.println("skinparam ParticipantBorderColor #b4bdd0");
        out.println("skinparam ParticipantBackgroundColor #21262d");
        out.println("skinparam ParticipantFontColor #e6edf3");
        out.println("skinparam StateBorderColor #b4bdd0");
        out.println("skinparam StateBackgroundColor #21262d");
        out.println("skinparam StateFontColor #e6edf3");
        out.println("skinparam ObjectBorderColor #b4bdd0");
        out.println("skinparam ObjectBackgroundColor #21262d");
        out.println("skinparam ObjectFontColor #e6edf3");
        out.println("skinparam DatabaseBorderColor #b4bdd0");
        out.println("skinparam DatabaseBackgroundColor #21262d");
        out.println("skinparam DatabaseFontColor #e6edf3");
        out.println("skinparam EntityBorderColor #b4bdd0");
        out.println("skinparam EntityBackgroundColor #21262d");
        out.println("skinparam EntityFontColor #e6edf3");
        out.println("skinparam AgentBorderColor #b4bdd0");
        out.println("skinparam AgentBackgroundColor #21262d");
        out.println("skinparam AgentFontColor #e6edf3");
        out.println("skinparam CardBorderColor #b4bdd0");
        out.println("skinparam CardBackgroundColor #21262d");
        out.println("skinparam CardFontColor #e6edf3");
        out.println("skinparam FileBorderColor #b4bdd0");
        out.println("skinparam FileBackgroundColor #21262d");
        out.println("skinparam FileFontColor #e6edf3");
        out.println("skinparam FolderBorderColor #b4bdd0");
        out.println("skinparam FolderBackgroundColor #161b22");
        out.println("skinparam FolderFontColor #e6edf3");
        out.println("skinparam FrameBorderColor #b4bdd0");
        out.println("skinparam FrameBackgroundColor #161b22");
        out.println("skinparam FrameFontColor #e6edf3");
        out.println("skinparam CloudBorderColor #b4bdd0");
        out.println("skinparam CloudBackgroundColor #21262d");
        out.println("skinparam CloudFontColor #e6edf3");
    }

    private String prepare(String type, String txt, NTxBounds2D b, NTxRendererContext rendererContext) {
        NStringBuilder out = NStringBuilder.of();
        out.println("@start" + type);
        if ("uml".equals(type)) {
            String layout = "smetana";
            NElement layoutVal = rendererContext == null ? null : rendererContext.getVarValue("plantumlLayout").orNull();
            if (layoutVal != null && layoutVal.isString()) {
                String s = layoutVal.asStringValue().get().trim();
                if (!s.isEmpty()) {
                    layout = s;
                }
            }
            out.println("!pragma layout " + layout);
        }
        out.println("scale " + (b.widthX().intValue()) + "*" + (b.widthY().intValue()));
        out.println("skinparam backgroundcolor transparent");
        if (isDarkTheme(rendererContext)) {
            appendDarkSkinparams(out);
        }
        out.println(txt);
        out.println("@end" + type);
        return out.build();
    }
}
