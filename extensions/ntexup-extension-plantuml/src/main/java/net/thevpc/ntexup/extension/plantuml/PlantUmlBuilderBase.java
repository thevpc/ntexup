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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import net.thevpc.nuts.elem.NElement;


public abstract class PlantUmlBuilderBase implements NTxNodeBuilder {
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
            SourceStringReader reader = new SourceStringReader(plantUMLText);
            // Write the first image to "png"
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                reader.outputImage(bos);
                image = ImageIO.read(new ByteArrayInputStream(bos.toByteArray()));
            } catch (Exception ex) {
                NTxSource src = NTxUtils.sourceOf(node);
                rendererContext.log(NMsg.ofC("Unable to evaluate UML : %s", ex).asSevere(), src);
            }
        }
        if (image != null) {

            if (!rendererContext.isDry()) {
                if (rendererContext.applyBackgroundColor()) {
                    g.fillRect((int) x, (int) y, NTxUtils.intOf(b.widthX()), NTxUtils.intOf(b.widthY()));
                }

                rendererContext.applyForeground(false);
                if (image != null) {
                    // would resize?
                    int w = NTxUtils.intOf(b.widthX());
                    int h = NTxUtils.intOf(b.widthY());
                    if (w > 0 && h > 0) {
                        BufferedImage resized = rendererContext.engine().tools().resizeBufferedImage(image, w, h);
                        g.drawImage(resized, (int) x, (int) y, null);
                    }
                }
            }
            rendererContext.drawContour();
        }
    }

    private boolean isDarkTheme(NTxRendererContext rendererContext) {
        if (rendererContext == null) {
            return false;
        }
        NElement bgVal = rendererContext.getVarValue("documentBg").orNull();
        if (bgVal != null && bgVal.isString()) {
            String s = bgVal.asStringValue().get().trim();
            if (s.startsWith("#") && (s.length() == 7 || s.length() == 4)) {
                try {
                    Color c = Color.decode(s);
                    double lum = 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
                    return lum < 128;
                } catch (Exception ignore) {
                }
            }
        }
        NElement txtVal = rendererContext.getVarValue("documentTextPrimary").orNull();
        if (txtVal != null && txtVal.isString()) {
            String s = txtVal.asStringValue().get().trim();
            if (s.startsWith("#") && (s.length() == 7 || s.length() == 4)) {
                try {
                    Color c = Color.decode(s);
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
        out.println("skinparam ArrowColor #8b949e");
        out.println("skinparam ActivityBorderColor #8b949e");
        out.println("skinparam ActivityBackgroundColor #21262d");
        out.println("skinparam ActivityFontColor #e6edf3");
        out.println("skinparam ActivityDiamondBorderColor #8b949e");
        out.println("skinparam ActivityDiamondBackgroundColor #21262d");
        out.println("skinparam ActivityDiamondFontColor #e6edf3");
        out.println("skinparam UsecaseBorderColor #8b949e");
        out.println("skinparam UsecaseBackgroundColor #21262d");
        out.println("skinparam UsecaseFontColor #e6edf3");
        out.println("skinparam ActorBorderColor #8b949e");
        out.println("skinparam ActorBackgroundColor #21262d");
        out.println("skinparam ActorFontColor #e6edf3");
        out.println("skinparam ClassBorderColor #8b949e");
        out.println("skinparam ClassBackgroundColor #21262d");
        out.println("skinparam ClassFontColor #e6edf3");
        out.println("skinparam ClassHeaderBackgroundColor #30363d");
        out.println("skinparam ComponentBorderColor #8b949e");
        out.println("skinparam ComponentBackgroundColor #21262d");
        out.println("skinparam ComponentFontColor #e6edf3");
        out.println("skinparam InterfaceBorderColor #8b949e");
        out.println("skinparam InterfaceBackgroundColor #21262d");
        out.println("skinparam InterfaceFontColor #e6edf3");
        out.println("skinparam NodeBorderColor #8b949e");
        out.println("skinparam NodeBackgroundColor #21262d");
        out.println("skinparam NodeFontColor #e6edf3");
        out.println("skinparam PackageBorderColor #8b949e");
        out.println("skinparam PackageBackgroundColor #161b22");
        out.println("skinparam PackageFontColor #e6edf3");
        out.println("skinparam RectangleBorderColor #8b949e");
        out.println("skinparam RectangleBackgroundColor #21262d");
        out.println("skinparam RectangleFontColor #e6edf3");
        out.println("skinparam SequenceLifeLineBorderColor #8b949e");
        out.println("skinparam SequenceLifeLineBackgroundColor #21262d");
        out.println("skinparam SequenceGroupBorderColor #8b949e");
        out.println("skinparam SequenceGroupBackgroundColor #161b22");
        out.println("skinparam SequenceGroupFontColor #e6edf3");
        out.println("skinparam ParticipantBorderColor #8b949e");
        out.println("skinparam ParticipantBackgroundColor #21262d");
        out.println("skinparam ParticipantFontColor #e6edf3");
        out.println("skinparam StateBorderColor #8b949e");
        out.println("skinparam StateBackgroundColor #21262d");
        out.println("skinparam StateFontColor #e6edf3");
        out.println("skinparam ObjectBorderColor #8b949e");
        out.println("skinparam ObjectBackgroundColor #21262d");
        out.println("skinparam ObjectFontColor #e6edf3");
        out.println("skinparam DatabaseBorderColor #8b949e");
        out.println("skinparam DatabaseBackgroundColor #21262d");
        out.println("skinparam DatabaseFontColor #e6edf3");
        out.println("skinparam EntityBorderColor #8b949e");
        out.println("skinparam EntityBackgroundColor #21262d");
        out.println("skinparam EntityFontColor #e6edf3");
        out.println("skinparam AgentBorderColor #8b949e");
        out.println("skinparam AgentBackgroundColor #21262d");
        out.println("skinparam AgentFontColor #e6edf3");
        out.println("skinparam CardBorderColor #8b949e");
        out.println("skinparam CardBackgroundColor #21262d");
        out.println("skinparam CardFontColor #e6edf3");
        out.println("skinparam FileBorderColor #8b949e");
        out.println("skinparam FileBackgroundColor #21262d");
        out.println("skinparam FileFontColor #e6edf3");
        out.println("skinparam FolderBorderColor #8b949e");
        out.println("skinparam FolderBackgroundColor #161b22");
        out.println("skinparam FolderFontColor #e6edf3");
        out.println("skinparam FrameBorderColor #8b949e");
        out.println("skinparam FrameBackgroundColor #161b22");
        out.println("skinparam FrameFontColor #e6edf3");
        out.println("skinparam CloudBorderColor #8b949e");
        out.println("skinparam CloudBackgroundColor #21262d");
        out.println("skinparam CloudFontColor #e6edf3");
    }

    private String prepare(String type, String txt, NTxBounds2D b, NTxRendererContext rendererContext) {
        NStringBuilder out = NStringBuilder.of();
        out.println("@start" + type);
        if ("uml".equals(type)) {
            out.println("!pragma layout smetana");
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
