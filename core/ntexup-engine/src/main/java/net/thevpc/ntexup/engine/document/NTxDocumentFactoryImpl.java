package net.thevpc.ntexup.engine.document;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.document.style.NTxProps;
import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.NAssert;


public class NTxDocumentFactoryImpl implements NTxDocumentFactory {
    private DefaultNTxEngine engine;

    public NTxDocumentFactoryImpl(DefaultNTxEngine engine) {
        this.engine = engine;
    }

    @Override
    public NTxDocument ofDocument(NTxSource source) {
        DefaultNTxDocument d = new DefaultNTxDocument(source);
        d.root().addRules(NTxDocumentRootRules.DEFAULT);
        return d;
    }

    @Override
    public NTxNode of(String type) {
        NAssert.requireNamedNonNull(type, "type");
        return engine.nodeTypeParser(type)
                .get().newNode();
    }

    @Override
    public NTxNode ofPage() {
        return of(NTxNodeType.PAGE);
    }

    @Override
    public NTxNode ofPageGroup() {
        return of(NTxNodeType.PAGE_GROUP);
    }


    @Override
    public NTxNode ofVoid() {
        return of(NTxNodeType.VOID);
    }

    @Override
    public NTxNode ofGlue() {
        return of(NTxNodeType.FILLER)
                .setProperty(NTxProps.maxX(100))
                .setProperty(NTxProps.maxY(100))
                ;
    }

    @Override
    public NTxNode ofGlueV() {
        return of(NTxNodeType.FILLER)
                .setProperty(NTxProps.maxX(0))
                .setProperty(NTxProps.maxY(100))
                ;
    }

    @Override
    public NTxNode ofGlueH() {
        return of(NTxNodeType.FILLER)
                .setProperty(NTxProps.maxX(100))
                .setProperty(NTxProps.maxY(0))
                ;
    }

    @Override
    public NTxNode ofStrutV(double w) {
        return of(NTxNodeType.FILLER)
                .setProperty(NTxProps.size(0, 100))
                ;
    }

    @Override
    public NTxNode ofStrutH(double w) {
        return of(NTxNodeType.FILLER)
                .setProperty(NTxProps.size(100, 0))
                ;
    }

    @Override
    public NTxNode ofStrut(double w, double h) {
        return of(NTxNodeType.FILLER)
                .setProperty(NTxProps.size(w, 0))
                ;
    }

    @Override
    public NTxNode ofArc(double from, double to) {
        return of(NTxNodeType.ARC)
                .setProperty(NTxProp.ofDouble(NTxPropName.FROM, from))
                .setProperty(NTxProp.ofDouble(NTxPropName.FROM, to))
                ;
    }

    @Override
    public NTxNode ofArc() {
        return of(NTxNodeType.ARC);
    }

    @Override
    public NTxNode ofAssign() {
        return of(NTxNodeType.CTRL_ASSIGN);
    }

    @Override
    public NTxNode ofAssign(String name, NElement value) {
        return ofAssign()
                .setProperty(NTxPropName.NAME, NElement.ofString(name))
                .setProperty(NTxPropName.VALUE, value)
                ;
    }

    @Override
    public NTxNode ofFlow() {
        return of(NTxNodeType.FLOW);
    }

    @Override
    public NTxNode ofGroup() {
        return of(NTxNodeType.GROUP);
    }

    @Override
    public NTxNode ofUnorderedList() {
        return of(NTxNodeType.UNORDERED_LIST);
    }

    @Override
    public NTxNode ofOrderedList() {
        return of(NTxNodeType.ORDERED_LIST);
    }

    @Override
    public NTxNode ofGrid(int cols, int rows) {
        return ofGrid()
                .setProperty(NTxPropName.COLUMNS, NElement.ofInt(cols))
                .setProperty(NTxPropName.ROWS, NElement.ofInt(rows))
                ;
    }

    public NTxNode ofGrid() {
        return of(NTxNodeType.GRID);
    }

    @Override
    public NTxNode ofGridV() {
        return ofGrid()
                .setProperty(NTxProps.columns(1))
                .setProperty(NTxProps.rows(-1))
                ;
    }

    @Override
    public NTxNode ofGridH() {
        return ofGrid()
                .setProperty(NTxProps.columns(-1))
                .setProperty(NTxProps.rows(1))
                ;
    }

    @Override
    public NTxNode ofPlain(String text) {
        return ofPlain().setProperty(NTxPropName.VALUE, NElement.ofString(text));
    }

    @Override
    public NTxNode ofText(String text) {
        return ofText().setProperty(NTxPropName.VALUE, NElement.ofString(text));
    }

    @Override
    public NTxNode ofText(NElement text) {
        return ofText().setProperty(NTxPropName.VALUE, text==null?NElement.ofString(""):text);
    }

    @Override
    public NTxNode ofPlain() {
        return of(NTxNodeType.PLAIN);
    }

    @Override
    public NTxNode ofRectangle() {
        return of(NTxNodeType.RECTANGLE);
    }

    @Override
    public NTxNode ofSphere() {
        return of(NTxNodeType.SPHERE);
    }

    @Override
    public NTxNode ofEllipsoid() {
        return of(NTxNodeType.ELLIPSOID);
    }

    @Override
    public NTxNode ofSquare() {
        return of(NTxNodeType.SQUARE);
    }


    @Override
    public NTxNode ofEllipse() {
        return of(NTxNodeType.ELLIPSE);
    }

    @Override
    public NTxNode ofSource() {
        return of(NTxNodeType.SOURCE);
    }


    @Override
    public NTxNode ofCircle() {
        return of(NTxNodeType.CIRCLE);
    }

    @Override
    public NTxNode ofTriangle() {
        return of(NTxNodeType.TRIANGLE);
    }

    @Override
    public NTxNode ofHexagon() {
        return of(NTxNodeType.HEXAGON);
    }

    @Override
    public NTxNode ofOctagon() {
        return of(NTxNodeType.OCTAGON);
    }

    @Override
    public NTxNode ofPentagon() {
        return of(NTxNodeType.PENTAGON);
    }

    public NTxNode ofPolygon() {
        return of(NTxNodeType.POLYGON);
    }
//    @Override
//    public NTxNode ofPolygon(int edges) {
//        if(edges<=2){
//            throw new IllegalArgumentException("invalid edges "+edges+". must be >2");
//        }
//        switch (edges){
//            case 3:{
//                return ofPolygon(
//                        new Double2(0,0),
//                        new Double2(0,100),
//                        new Double2(50,0)
//                );
//            }
//            case 4:{
//                return ofPolygon(
//                        new Double2(0,0),
//                        new Double2(0,100),
//                        new Double2(100,100),
//                        new Double2(100,0)
//                );
//            }
//            default:{
//                java.util.List<Double2> all=new ArrayList<>();
//                    for (int i = 0; i <edges ; i++) {
//                        all.add(
//                                new Double2(
//                                        50 + Math.sin(i*Math.PI*2/edges)*50,
//                                        50 + Math.cos(i*Math.PI*2/edges)*50
//                                )
//                        );
//                    }
//                    return ofPolygon(all.toArray(new Double2[0]));
//                }
//        }
//    }

    @Override
    public NTxNode ofPolyline() {
        return of(NTxNodeType.POLYLINE);
    }

    @Override
    public NTxNode ofLine() {
        return of(NTxNodeType.LINE);
    }

    @Override
    public NTxNode ofImage() {
        return of(NTxNodeType.IMAGE);
    }

    @Override
    public NTxNode ofEquation() {
        return of(NTxNodeType.EQUATION);
    }

    @Override
    public NTxNode ofEquation(String value) {
        return ofEquation().setProperty(NTxPropName.VALUE, NElement.ofString(value));
    }

    @Override
    public NTxNode ofText() {
        return of(NTxNodeType.TEXT);
    }
}
