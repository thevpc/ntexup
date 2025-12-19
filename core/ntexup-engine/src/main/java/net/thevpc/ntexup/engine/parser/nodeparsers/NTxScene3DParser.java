//package net.thevpc.ntexup.engine.parser.nodeparsers;
//
//import net.thevpc.ntexup.api.parser.NTxArgumentReader;
//import net.thevpc.ntexup.api.document.elem3d.NTxPoint3D;
//import net.thevpc.ntexup.api.document.node.NTxNodeType;
//import net.thevpc.ntexup.engine.parser.NTxNodeParserBase;
//import net.thevpc.ntexup.api.eval.NTxValue;
//import net.thevpc.nuts.elem.NElement;
//import net.thevpc.nuts.util.NOptional;
//
//public class NTxScene3DParser extends NTxNodeParserBase {
//    public NTxScene3DParser() {
//        super(false, NTxNodeType.SCENE3D);
//    }
//
//    @Override
//    protected boolean processArgument(NTxArgumentReader info) {
//        NElement currentArg = info.peek();
//        switch (currentArg.type()) {
//            case PAIR: {
//                NOptional<NTxValue.SimplePair> sp = NTxValue.of(currentArg).asSimplePair();
//                if (sp.isPresent()) {
//                    NTxValue.SimplePair spp = sp.get();
//                    NTxValue v = spp.getValue();
//                    switch (spp.getNameId()) {
//                        case "camera-orientation":
//                        case "light-orientation": {
//                            NOptional<NTxPoint3D> p2d = v.asHPoint3D();
//                            if (p2d.isPresent()) {
//                                info.node().setProperty(spp.getNameId(), p2d.get());
//                                info.read();
//                                return true;
//                            } else {
//                                return false;
//                            }
//                        }
//                    }
//                }
//                break;
//            }
//        }
//        return super.processArgument(info);
//    }
//
//}
