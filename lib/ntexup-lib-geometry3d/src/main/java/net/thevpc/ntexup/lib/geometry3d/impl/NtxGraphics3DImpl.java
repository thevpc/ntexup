package net.thevpc.ntexup.lib.geometry3d.impl;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.elem2d.primitives.NtxElement2DLine;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.util.NTxColors;

import net.thevpc.ntexup.lib.geometry3d.*;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.*;
import net.thevpc.nuts.util.NUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class NtxGraphics3DImpl implements NtxGraphics3D {
    private NTxGraphics graphics;
    private NTxNodeRendererContext rendererContext;
    private NTxMatrix3D transform3D = NTxMatrix3D.identity();
    private NTxLight3DImpl light3D = new NTxLight3DImpl();
    private NTxCamera3D camera = NTxCamera3DImpl.defaultCamera();
    private NTx3DMesh mesh = new DefaultNTx3DMesh();
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

    public NtxGraphics3DImpl(NTxGraphics graphics, NTxNodeRendererContext rendererContext) {
        this.graphics = graphics;
        this.rendererContext = rendererContext;
    }

    public NTxCamera3D getCamera() {
        return camera;
    }

    public NtxGraphics3DImpl setCamera(NTxCamera3D camera) {
        this.camera = camera == null ? NTxCamera3DImpl.defaultCamera() : camera;
        return this;
    }

    public NTx3DMesh getMesh() {
        return mesh;
    }

    public NtxGraphics3DImpl setMesh(NTx3DMesh mesh) {
        this.mesh = mesh == null ? new DefaultNTx3DMesh() : mesh;
        return this;
    }

    @Override
    public void transform3D(NTxMatrix3D transform3D) {
        if (transform3D != null) {
            this.transform3D = this.transform3D.multiply(transform3D);
        }
    }

    public static class DrawCommand {
        NTxMatrix3D transform;
        NtxElement3DPrimitive primitive;
        double depth;
    }


    private NTxPoint3D[] applyTransform(NTxPoint3D[] points, DrawCommand c) {
        NTxPoint3D[] r = new NTxPoint3D[points.length];
        NTxMatrix3D nt = c.transform == null ? transform3D : transform3D.multiply(c.transform);
        for (int i = 0; i < r.length; i++) {
            r[i] = nt.multiplyPoint(points[i]);
        }
        return r;
    }

    private NTxPoint3D applyTransform(NTxPoint3D point, DrawCommand c) {
        NTxMatrix3D nt = c.transform == null ? transform3D : transform3D.multiply(c.transform);
        return nt.multiplyPoint(point);
    }

    private NtxElement3DPrimitive[] toPrimitives(NtxElement3D element3D) {
        NTx3DMesh m = mesh.configureElement(element3D);
        java.util.List<NtxElement3DPrimitive> result = new ArrayList<>();
        for (NtxElement3DPrimitive p : getElement3DUIFactory().toPrimitives(element3D, state)) {
            switch (p.type()) {
                case ARC:
                case POLYLINE:
                case LINE: {
                    result.add(p);
                    break;
                }
                case POLYGON: {
                    m.triangulatePolygon((NtxElement3DPolygon) p, result);
                    break;
                }
                case TRIANGLE: {
                    m.refineTriangle((NtxElement3DTriangle) p, result);
                    break;
                }
                default: {
                    result.add(p);
                }
            }
        }
        return result.toArray(new NtxElement3DPrimitive[0]);
    }

    @Override
    public void draw3D(NtxElement3D element3D, NTxPoint2D origin) {
        NTx3DMesh oldMesh = mesh;
        NtxElement3DPrimitive[] primitives = toPrimitives(element3D);
        //NTxMatrix3D old = getTransform3D() == null ? NTxMatrix3D.identity() : getTransform3D();
        DrawCommand[] commands = Arrays.stream(primitives).map(primitive -> {
                    DrawCommand c = new DrawCommand();
                    c.primitive = primitive;
                    c.transform = primitive.getTransform();
                    NTxMatrix3D t = c.transform == null ? transform3D : transform3D.multiply(c.transform);
                    NTxPoint3D[] newPoints = Arrays.stream(primitive.points()).map(x -> {
                        NTxPoint3D pWorld = t.multiplyPoint(x);
                        NTxPoint3D pCam = camera.getViewMatrix().multiplyPoint(pWorld);
                        return pCam;
                    }).toArray(NTxPoint3D[]::new);
                    c.depth = Arrays.stream(newPoints)
                            .mapToDouble(p -> p.z)
                            .average()
                            .orElse(0);
                    return c;
                })
                .sorted(Comparator.comparingDouble(c -> c.depth))
                .toArray(DrawCommand[]::new);
        for (DrawCommand cmd : commands) {
            switch (cmd.primitive.type()) {
                case LINE: {
                    draw3DElement3DLine((NtxElement3DLine) cmd.primitive, origin, cmd);
                    break;
                }
                case ARC: {
                    draw3DElement3DArc((NtxElement3DArc) cmd.primitive, origin, cmd);
                    break;
                }
                case POLYGON: {
                    draw3DElement3DPolygon((NtxElement3DPolygon) cmd.primitive, origin, cmd);
                    break;
                }

                case POLYLINE: {
                    draw3DElement3DPolyline((NtxElement3DPolyline) cmd.primitive, origin, cmd);
                    break;
                }
                case TRIANGLE: {
                    draw3DElement3DTriangle((NtxElement3DTriangle) cmd.primitive, origin, cmd);
                    break;
                }
            }
        }
        setMesh(oldMesh);
    }


    public NTxMatrix3D getTransform3D() {
        return transform3D;
    }

    public NtxGraphics3DImpl setTransform3D(NTxMatrix3D transform3D) {
        this.transform3D = transform3D;
        return this;
    }

    /// ////////////////////////////////////////////////////


    private void draw3DElement3DLine(NtxElement3DLine pr, NTxPoint2D origin, DrawCommand cmd) {
        NTxPoint3D[] pts3d = applyTransform(new NTxPoint3D[]{pr.getFrom(), pr.getTo()}, cmd);
        NTxPoint2D[] pts2d = camera.projectFromWorldToScreen(pts3d, origin);
        graphics.draw2D(
                new NtxElement2DLine(pts2d[0], pts2d[1])
                        .setStartArrow(pr.getStartArrow())
                        .setEndArrow(pr.getEndArrow())
                        .setComposite(pr.getComposite())
                        .setBackgroundPaint(pr.getBackgroundPaint())
                        .setLinePaint(NUtils.firstNonNull(pr.getLinePaint(),pr.getForegroundPaint()))
                        .setLineStroke(pr.getLineStroke())

        );
    }

    private void draw3DElement3DArc(NtxElement3DArc pr, NTxPoint2D origin, DrawCommand cmd) {
        double x = origin.x;
        double y = origin.y;
        NTxPoint3D[] pts3d = applyTransform(new NTxPoint3D[]{pr.getFrom(), pr.getTo()}, cmd);
        NTxPoint2D[] pts2d = camera.projectFromWorldToScreen(pts3d, origin);

        NTxPoint2D point1 = pts2d[0];
        NTxPoint2D point2 = pts2d[1];
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

    private void draw3DElement3DPolygon(NtxElement3DPolygon pr, NTxPoint2D origin, DrawCommand cmd) {
        NTx3DMesh oldMesh = mesh;
        setMesh(oldMesh.configureElement(pr));
        NTxPoint3D[] pts3d = applyTransform(pr.points(), cmd);
        NTxPoint2D[] pts2d = camera.projectFromWorldToScreen(pts3d, origin);
        double[] xx = new double[pts2d.length];
        double[] yy = new double[pts2d.length];
        for (int i = 0; i < xx.length; i++) {
            xx[i] = pts2d[i].x;
            yy[i] = pts2d[i].y;
        }
        NTxVector3D worldNormal = NTx3DUtils.surfaceNormal(pts3d[0], pts3d[1], pts3d[2]);
        NTxPoint3D centroid = NTx3DUtils.computeCentroid(pts3d);
        NTxVector3D viewDir = camera.getPosition().asVector().minus(centroid.asVector()).normalize();

        // Dot product: > 0 means face points toward camera
        boolean isFrontFacing = worldNormal.dot(viewDir) > 0;

        double d = worldNormal.dot(getLight3D().orientation());
        Graphics2D g = graphics.graphics2D();
        if (pr.isFill() && isFrontFacing) {
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
                graphics.setColor(NTxColors.withB((Color) bg, Math.abs((float) d)));
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
        mesh=oldMesh;
    }

    private void draw3DElement3DPolyline(NtxElement3DPolyline pr, NTxPoint2D origin, DrawCommand cmd) {
        NTxPoint3D[] pts3d = applyTransform(pr.points(), cmd);
        NTxPoint2D[] pts2d = camera.projectFromWorldToScreen(pts3d, origin);
        double[] xx = new double[pts3d.length];
        double[] yy = new double[pts3d.length];
        for (int i = 0; i < xx.length; i++) {
            NTxPoint2D pp = pts2d[i];
            xx[i] = pp.x;
            yy[i] = pp.y;
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

    private void draw3DElement3DTriangle(NtxElement3DTriangle pr, NTxPoint2D origin, DrawCommand cmd) {
        NTx3DMesh oldMesh = mesh;
        setMesh(oldMesh.configureElement(pr));
        NTxPoint3D[] pts3d = applyTransform(pr.points(), cmd);
        NTxPoint2D[] pts2d = camera.projectFromWorldToScreen(pts3d, origin);
        double[] xx = new double[3];
        double[] yy = new double[3];

        NTxPoint2D pp1 = pts2d[0];
        xx[0] = pp1.x;
        yy[0] = pp1.y;

        NTxPoint2D pp2 = pts2d[1];
        xx[1] = pp2.x;
        yy[1] = pp2.y;

        NTxPoint2D pp3 = pts2d[2];
        xx[2] = pp3.x;
        yy[2] = pp3.y;

        // i would here test for faces that are visible or not

        NTxVector3D worldNormal = NTx3DUtils.surfaceNormal(pts3d[0], pts3d[1], pts3d[2]);
        NTxPoint3D centroid = NTx3DUtils.computeCentroid(pts3d);
        NTxVector3D viewDir = camera.getPosition().asVector().minus(centroid.asVector()).normalize();

        // Dot product: > 0 means face points toward camera
        double dotProduct = worldNormal.dot(viewDir);
        boolean isFrontFacing = true;//dotProduct > 0;
        // SKIP ENTIRE TRIANGLE IF BACK-FACING
        // DEBUG: Print to see what's happening
//        if (centroid.z < 0) { // Triangles below z=0
//            System.out.println("Below-ground triangle: centroid=" + centroid +
//                    ", dotProduct=" + dotProduct +
//                    ", isFrontFacing=" + isFrontFacing +
//                    ", normal=" + worldNormal);
//        }
        if (!isFrontFacing) {
            return; // Don't draw at all
        }
        double d = 1;//worldNormal.dot(getLight3D().orientation());
        Graphics2D g = graphics.graphics2D();
        if (pr.isFill() && isFrontFacing) {
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
                graphics.setColor(NTxColors.withB((Color) bg,
                        Math.abs((float) d)
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
        mesh=oldMesh;
    }

    private Element3DUIFactory getElement3DUIFactory() {
        return rendererContext.engine().computeIfAbsent(Element3DUIFactory.class.getName(), key -> new Element3DUIFactory(rendererContext.engine())).get();
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
