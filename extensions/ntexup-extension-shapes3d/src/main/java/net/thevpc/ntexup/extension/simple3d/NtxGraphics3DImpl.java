package net.thevpc.ntexup.extension.simple3d;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.elem2d.primitives.NtxElement2DLine;
import net.thevpc.ntexup.api.document.elem3d.*;
import net.thevpc.ntexup.api.document.elem3d.primitives.*;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.util.NTxColors;

import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;

public class NtxGraphics3DImpl implements NtxGraphics3D {
    private NTxGraphics graphics;
    private NTxNodeRendererContext rendererContext;
    private NTxNodeBuilderContext buildContext;
    private NTxProjection3D projection3D = new NTxProjection3D(1000);
    private NTxMatrix3D transform3D = NTxMatrix3D.identity();
    private NTxLight3DImpl light3D = new NTxLight3DImpl();
    private NTxCamera3D camera = NTxCamera3DImpl.defaultCamera();
    private NTxRenderState3D state = new NTxRenderState3D() {
        @Override
        public NTxVector3D lightOrientation() {
            return light3D.orientation();
        }

        @Override
        public NtxElement3DPrimitive[] toPrimitives(NtxElement3D e) {
            return getElement3DUIFactory().toPrimitives(e, this);
        }
    };

    public NtxGraphics3DImpl(NTxGraphics graphics, NTxNodeRendererContext rendererContext, NTxNodeBuilderContext buildContext) {
        this.graphics = graphics;
        this.rendererContext = rendererContext;
        this.buildContext = buildContext;
    }

    @Override
    public void transform3D(NTxMatrix3D transform3D) {
        if (transform3D != null) {
            this.transform3D = this.transform3D.multiply(transform3D);
        }
    }

    @Override
    public void project3D(NTxProjection3D projection3D) {
        if (projection3D != null) {
            this.projection3D = projection3D;
        }
    }

    private static class DrawCommand {
        NTxMatrix3D transform;
        NtxElement3DPrimitive primitive;
        double depth;
    }

    @Override
    public void draw3D(NtxElement3D element3D, NTxPoint2D origin) {
        NtxElement3DPrimitive[] primitives = getElement3DUIFactory().toPrimitives(element3D, state);
        NTxMatrix3D old = getTransform3D() == null ? NTxMatrix3D.identity() : getTransform3D();
        DrawCommand[] commands= Arrays.stream(primitives).map(primitive->{
            NTxMatrix3D n = primitive.getTransform();
            DrawCommand c = new DrawCommand();
            c.primitive = primitive;
            c.transform = n == null ? old : old.multiply(n);
            //c.transform.multiply() <-- ??
            NTxPoint3D[] newPoints = Arrays.stream(primitive.points()).map(x -> {
                NTxPoint3D pWorld = c.transform.multiplyPoint(x);
                NTxPoint3D pCam = camera.getViewMatrix().multiplyPoint(pWorld);
                return pCam;
            }).toArray(NTxPoint3D[]::new);
            c.depth = Arrays.stream(newPoints)
                    .mapToDouble(p -> p.z)
                    .average()
                    .orElse(0);
            return c;
        })
                .sorted(Comparator.comparingDouble(c->c.depth))
                .sorted(Comparator.comparingDouble(c -> -c.depth))
                .toArray(DrawCommand[]::new);
        try {
            for (DrawCommand cmd : commands) {
                setTransform3D(cmd.transform);
                switch (cmd.primitive.type()) {
                    case LINE: {
                        draw3DElement3DLine((NtxElement3DLine) cmd.primitive, origin,cmd);
                        break;
                    }
                    case ARC: {
                        draw3DElement3DArc((NtxElement3DArc) cmd.primitive, origin,cmd);
                        break;
                    }
                    case POLYGON: {
                        draw3DElement3DPolygon((NtxElement3DPolygon) cmd.primitive, origin,cmd);
                        break;
                    }

                    case POLYLINE: {
                        draw3DElement3DPolyline((NtxElement3DPolyline) cmd.primitive, origin,cmd);
                        break;
                    }
                    case TRIANGLE: {
                        draw3DElement3DTriangle((NtxElement3DTriangle) cmd.primitive, origin,cmd);
                        break;
                    }
                }
            }
        } finally {
            setTransform3D(old);
        }
    }

    public NTxMatrix3D getTransform3D() {
        return transform3D;
    }

    public NtxGraphics3DImpl setTransform3D(NTxMatrix3D transform3D) {
        this.transform3D = transform3D;
        return this;
    }

    /// ////////////////////////////////////////////////////


    private void draw3DElement3DLine(NtxElement3DLine pr, NTxPoint2D origin,DrawCommand cmd) {
        NTxPoint3D p1 = pr.getFrom().transform(transform3D);
        NTxPoint3D p2 = pr.getTo().transform(transform3D);

        NTxPoint2D point1 = projection3D.project(p1).plus(origin);
        NTxPoint2D point2 = projection3D.project(p2).plus(origin);
        graphics.draw2D(
                new NtxElement2DLine(point1, point2)
                        .setStartArrow(pr.getStartArrow())
                        .setEndArrow(pr.getEndArrow())
                        .setComposite(pr.getComposite())
                        .setBackgroundPaint(pr.getBackgroundPaint())
                        .setLinePaint(pr.getLinePaint())
                        .setLineStroke(pr.getLineStroke())
        );
    }

    private void draw3DElement3DArc(NtxElement3DArc pr, NTxPoint2D origin,DrawCommand cmd) {
        double x = origin.x;
        double y = origin.y;
        NTxPoint3D p1 = pr.getFrom().transform(transform3D);
        NTxPoint3D p2 = pr.getTo().transform(transform3D);

        NTxPoint2D point1 = projection3D.project(p1);
        NTxPoint2D point2 = projection3D.project(p2);
        double xmin = Math.min(point1.x + x, point2.x + x);
        double w = Math.abs(point1.x - point2.x);
        double ymin = Math.min(point1.y + y, point2.y + y);
        double h = Math.abs(point1.y - point2.y);
        Paint oldPaint = graphics.graphics2D().getPaint();
        Graphics2D g = graphics.graphics2D();
        Stroke oldStroke = g.getStroke();
        if (pr.getLinePaint() != null) {
            graphics.setPaint(pr.getLinePaint());
        }
        if (pr.getLineStroke() != null) {
            graphics.setStroke(pr.getLineStroke());
        }
        graphics.drawArc(
                xmin, ymin,
                w,
                h,
                pr.getStartAngle(),
                pr.getEndAngle()
        );
        graphics.setPaint(oldPaint);
        graphics.setStroke(oldStroke);
    }

    private void draw3DElement3DPolygon(NtxElement3DPolygon pr, NTxPoint2D origin,DrawCommand cmd) {
        double x = origin.x;
        double y = origin.y;
        NTxPoint3D[] nodes = pr.getNodes();
        double[] xx = new double[nodes.length];
        double[] yy = new double[nodes.length];
        for (int i = 0; i < xx.length; i++) {
            NTxPoint3D p = nodes[i].transform(transform3D);
            NTxPoint2D pp = projection3D.project(p);
            xx[i] = (pp.x + x);
            yy[i] = (pp.y + y);
        }
        double d = NTxD3Utils.surfaceNormal(nodes[0], nodes[1], nodes[2]).dot(getLight3D().orientation());
        Graphics2D g = graphics.graphics2D();
        if (pr.isFill()) {
            Paint oldPaint = g.getPaint();
            Composite oldComposite = g.getComposite();

            if (pr.getLinePaint() != null) {
                graphics.setPaint(pr.getLinePaint());
            }
            if (pr.getLineStroke() != null) {
                graphics.setStroke(pr.getLineStroke());
            }

            Paint bg = pr.getBackgroundPaint();
            if (bg == null) {
                bg = graphics.getColor();
            }
            if (bg instanceof Color) {
                graphics.setColor(NTxColors.withB((Color) bg //                                        , Math.abs(1 - (float) d)
                        ,
                        Math.abs((float) d)
                        //, Math.abs(Math.abs((float) d))
                ));
            } else {
                graphics.setPaint(bg);
            }
            if (pr.getComposite() != null) {
                graphics.setComposite(pr.getComposite());
            }
            graphics.fillPolygon(xx, yy, xx.length);
            graphics.setPaint(oldPaint);
            graphics.setComposite(oldComposite);
        }
        if (pr.isContour()) {
            Paint oldPaint = g.getPaint();
            Stroke oldStroke = g.getStroke();
            if (pr.getContourPaint() != null) {
                graphics.setPaint(pr.getContourPaint());
            }
            if (pr.getContourStroke() != null) {
                graphics.setStroke(pr.getContourStroke());
            }
            graphics.drawPolygon(xx, yy, xx.length);
            graphics.setPaint(oldPaint);
            graphics.setStroke(oldStroke);
        }
    }

    private void draw3DElement3DPolyline(NtxElement3DPolyline pr, NTxPoint2D origin,DrawCommand cmd) {
        double x = origin.x;
        double y = origin.y;
        NTxPoint3D[] nodes = pr.getNodes();
        double[] xx = new double[nodes.length];
        double[] yy = new double[nodes.length];
        for (int i = 0; i < xx.length; i++) {
            NTxPoint3D p = nodes[i].transform(transform3D);
            NTxPoint2D pp = projection3D.project(p);
            xx[i] = (pp.x + x);
            yy[i] = (pp.y + y);
        }
        Graphics2D g = graphics.graphics2D();
        Paint oldPaint = g.getPaint();
        Stroke oldStroke = g.getStroke();
        if (pr.getLinePaint() != null) {
            graphics.setPaint(pr.getLinePaint());
        }
        if (pr.getLineStroke() != null) {
            graphics.setStroke(pr.getLineStroke());
        }
        graphics.drawPolyline(xx, yy, xx.length);
        graphics.setPaint(oldPaint);
        graphics.setStroke(oldStroke);
    }

    private void draw3DElement3DTriangle(NtxElement3DTriangle pr, NTxPoint2D origin,DrawCommand cmd) {
        double x = origin.x;
        double y = origin.y;
        double[] xx = new double[3];
        double[] yy = new double[3];

        NTxPoint3D p1 = pr.getP1();
        NTxPoint2D pp1 = projection3D.project(p1.transform(transform3D));
        xx[0] = (pp1.x + x);
        yy[0] = (pp1.y + y);

        NTxPoint3D p2 = pr.getP2();
        NTxPoint2D pp2 = projection3D.project(p2.transform(transform3D));
        xx[1] = (pp2.x + x);
        yy[1] = (pp2.y + y);

        NTxPoint3D p3 = pr.getP3();
        NTxPoint2D pp3 = projection3D.project(p3.transform(transform3D));
        xx[2] = (pp3.x + x);
        yy[2] = (pp3.y + y);

        double d = NTxD3Utils.surfaceNormal(p1, p2, p3).dot(getLight3D().orientation());
        Graphics2D g = graphics.graphics2D();
        if (true/*d < 0*/) {
            if (pr.isFill()) {
                Paint oldPaint = g.getPaint();
                Composite oldComposite = g.getComposite();

                if (pr.getLinePaint() != null) {
                    graphics.setPaint(pr.getLinePaint());
                }
                if (pr.getLineStroke() != null) {
                    graphics.setStroke(pr.getLineStroke());
                }

                Paint bg = pr.getBackgroundPaint();
                if (bg == null) {
                    bg = graphics.getColor();
                }
                if (bg instanceof Color) {
                    graphics.setColor(NTxColors.withB((Color) bg //                                        , Math.abs(1 - (float) d)
                            ,
                            Math.abs((float) d)
                            //, Math.abs(Math.abs((float) d))
                    ));
                } else {
                    graphics.setPaint(bg);
                }
                if (pr.getComposite() != null) {
                    graphics.setComposite(pr.getComposite());
                }
                graphics.fillPolygon(xx, yy, xx.length);
                graphics.setPaint(oldPaint);
                graphics.setComposite(oldComposite);
            }
            if (false && pr.isContour()) {
                Paint oldPaint = g.getPaint();
                Stroke oldStroke = g.getStroke();
                if (pr.getContourPaint() != null) {
                    graphics.setPaint(pr.getContourPaint());
                }
                if (pr.getContourStroke() != null) {
                    graphics.setStroke(pr.getContourStroke());
                }
                graphics.drawPolygon(xx, yy, xx.length);
                graphics.setPaint(oldPaint);
                graphics.setStroke(oldStroke);
            }
        }
    }

    private Element3DUIFactory getElement3DUIFactory() {
        return rendererContext.engine().computeIfAbsent(Element3DUIFactory.class.getName(), key -> new Element3DUIFactory(buildContext.engine())).get();
    }


    @Override
    public NTxLight3D getLight3D() {
        return light3D;
    }

    @Override
    public NTxLight3D setLight3D(NTxLight3D light3D) {
        return light3D;
    }
}
