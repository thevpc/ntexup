package net.thevpc.ntexup.extension.plantuml;

import net.sourceforge.plantuml.SourceStringReader;
import net.thevpc.ntexup.api.document.elem2d.NTxBounds2;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.text.NMsg;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;


public abstract class PlantUmlBuilderBase implements NTxNodeBuilder {
    private String id;
    private String mode;
    private String[] aliases;
    private NTxProperties defaultStyles = new NTxProperties();

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
                .parseParam().matchesNamedPair(NTxPropName.VALUE,NTxPropName.FILE).then()
                .parseParam().matchesAnyNonPair().storeFirstMissingName(NTxPropName.VALUE).then()
                .renderComponent((ctx, builderContext1) -> renderMain(ctx, builderContext1))
        ;
    }


    public void renderMain(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext builderContext) {
        NTxNode node = rendererContext.node();
        rendererContext = rendererContext.withDefaultStyles(defaultStyles);
        String txt = NTxValue.of(node.getPropertyValue(NTxPropName.VALUE).orNull()).asStringOrName().orNull();
        if (NBlankable.isBlank(txt)) {
            return;
        }
        String mode = NTxUtils.uid(this.mode);
        NTxGraphics g = rendererContext.graphics();
        NTxBounds2 b = rendererContext.selfBounds();
        double x = b.getX();
        double y = b.getY();
        BufferedImage image = null;
        String plantUMLText = null;
        if(mode.startsWith("plantuml-")){
            mode=mode.substring("plantuml-".length());
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
                plantUMLText = prepare(mode, txt, b);
                break;
            }
            case "nwdiag": {
                plantUMLText = prepare("uml",
                        "nwdiag {\n"
                                + txt
                                + "\n}"
                        , b);
                break;
            }
            case "salt":
            case "wireframe": {
                plantUMLText = prepare("salt", txt, b);
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
                rendererContext.log().log(NMsg.ofC("Unable to evaluate UML : %s", ex).asSevere(), src);
            }
        }
        if (image != null) {

            if (!rendererContext.isDry()) {
                if (rendererContext.applyBackgroundColor(node)) {
                    g.fillRect((int) x, (int) y, NTxUtils.intOf(b.getWidth()), NTxUtils.intOf(b.getHeight()));
                }

                rendererContext.applyForeground(node, false);
                if (image != null) {
                    // would resize?
                    int w = NTxUtils.intOf(b.getWidth());
                    int h = NTxUtils.intOf(b.getHeight());
                    if (w > 0 && h > 0) {
                        BufferedImage resized = rendererContext.engine().tools().resizeBufferedImage(image, w, h);
                        g.drawImage(resized, (int) x, (int) y, null);
                    }
                }
            }
            rendererContext.drawContour();
        }
    }

    private String prepare(String type, String txt, NTxBounds2 b) {
        return "@start" + type + "\n"
                + "scale " + (b.getWidth().intValue()) + "*" + (b.getHeight().intValue()) + "\n"
                + "skinparam backgroundcolor transparent\n"
//                        + "skinparam dpi 300\n"
                + txt
                + "\n@end" + type + "\n";
    }
}
