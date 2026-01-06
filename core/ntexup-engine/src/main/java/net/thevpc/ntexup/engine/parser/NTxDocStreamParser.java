package net.thevpc.ntexup.engine.parser;

import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NBooleanRef;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;
import net.thevpc.nuts.util.NRef;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class NTxDocStreamParser {
    private NTxEngine engine;

    public NTxDocStreamParser(NTxEngine engine) {
        this.engine = engine;
    }

    class CondBody {
        List<NElement> cond = null;
        List<NElement> trueBody = null;
    }

    class IfInfo {
        CondBody base = null;
        List<CondBody> elseIfs = new ArrayList<>();
        NElement elseBody;

        NElement toElement() {
            NObjectElementBuilder i = NElement.ofObjectBuilder("if");
            if (base.cond.size() == 0) {
                i.addParam("$condition", true);
            } else if (base.cond.size() == 1) {
                i.addParam("$condition", base.cond.get(0));
            } else {
                i.addParam("$condition", NElement.ofNamedUplet("and", base.cond.toArray(new NElement[0])));
            }
            i.addAll(base.trueBody);
            NObjectElementBuilder goodElse = null;
            NObjectElementBuilder deepElse = null;
            for (CondBody b : elseIfs) {
                NObjectElementBuilder i2 = NElement.ofObjectBuilder("if");
                if (b.cond.size() == 0) {
                    i2.set("$condition", true);
                } else if (b.cond.size() == 1) {
                    i2.set("$condition", b.cond.get(0));
                } else {
                    i2.set("$condition", NElement.ofNamedUplet("and", b.cond.toArray(new NElement[0])));
                }
                i2.addAll(base.trueBody);
                if (goodElse == null) {
                    goodElse = i2;
                    deepElse = i2;
                } else {
                    deepElse.set("$else", i2.build());
                }
            }
            if (elseBody != null) {
                if (deepElse != null) {
                    deepElse.set("$else", elseBody);
                } else {
                    goodElse = NElement.ofObjectBuilder();
                    if (elseBody.isObject()) {
                        goodElse.addAll(elseBody.asObject().get().children());
                    } else if (elseBody.isArray()) {
                        goodElse.addAll(elseBody.asArray().get().children());
                    } else {
                        goodElse.add(elseBody);
                    }
                }
            }
            if (goodElse != null) {
                deepElse.set("$else", elseBody);
            }
            return i.build();
        }
    }


    private List<NElement> processControlElements(List<NElement> children, NBooleanRef someChanges) {
        List<NElement> res = new ArrayList<>();
        IfInfo ifInfo = null;
        boolean cc = false;
        if (children == null) {
            return null;
        }
        List<NElement> children2 = new ArrayList<>(children);
        while (!children2.isEmpty()) {
            NElement c = children2.remove(0);
            if (c.isNamedParametrizedObject("if")) {
                if (ifInfo != null) {
                    res.add(ifInfo.toElement());
                    ifInfo = null;
                }
                ifInfo = new IfInfo();
                NObjectElement o = c.asObject().get();
                ifInfo.base = new CondBody();
                ifInfo.base.cond = o.params().get();
                ifInfo.base.trueBody = o.children();
                cc = true;
            } else if (c.isNamedUplet("if")) {
                if (ifInfo != null) {
                    res.add(ifInfo.toElement());
                }
                ifInfo = new IfInfo();
                NObjectElement o = c.asObject().get();
                ifInfo.base = new CondBody();
                ifInfo.base.cond = o.params().get();
                ifInfo.base.trueBody = o.children();
                engine.log().log(NMsg.ofC("if expression is missing brackets : %s", c).asError());
                cc = true;
            } else if (c.isNamedParametrizedObject("elseif") && ifInfo != null) {
                NObjectElement o = c.asObject().get();
                CondBody b = new CondBody();
                b.cond = o.params().get();
                b.trueBody = o.children();
                ifInfo.elseIfs.add(b);
                cc = true;
            } else if (c.isNamedObject("else") && ifInfo != null) {
                NObjectElement o = c.asObject().get();
                ifInfo.elseBody = o.builder().name(null).build();
                cc = true;
            } else {
                if (ifInfo != null) {
                    res.add(ifInfo.toElement());
                    ifInfo = null;
                }
                res.add(c);
            }
        }
        if (ifInfo != null) {
            res.add(ifInfo.toElement());
            ifInfo = null;
        }
        if (cc) {
            someChanges.set(true);
            return res;
        }
        return children;
    }

    private NElementAnnotation processControlElementsAnnotation(NElementAnnotation child, NBooleanRef someChanges) {
        NBooleanRef u = NBooleanRef.of(false);
        List<NElement> np = processControlElements(child.params(), u);
        if (u.get()) {
            someChanges.set(true);
            return NElement.ofAnnotation(child.name(), np.toArray(new NElement[0]));
        }
        return child;
    }

    private NElement processControlElements(NElement child, NBooleanRef someChanges) {
        List<NElementAnnotation> annotations = new ArrayList<>();
        boolean changesInAnnotation = false;
        for (NElementAnnotation annotation : child.annotations()) {
            NBooleanRef r = NBooleanRef.of(false);
            annotations.add(processControlElementsAnnotation(annotation, r));
            if (r.get()) {
                changesInAnnotation = true;
            }
        }
        switch (child.type().typeGroup()) {
            case NULL:
            case NUMBER:
            case CUSTOM:
            case TEMPORAL:
            case STREAM:
            case STRING:
            case BOOLEAN:
            case OTHER: {
                if (changesInAnnotation) {
                    child = child.builder().clearAnnotations().addAnnotations(annotations).build();
                    someChanges.set(true);
                }
                return child;
            }
            case OPERATOR: {
                NOperatorElement op = child.asOperator().get();
                NOperatorElementBuilder opb = op.builder();
                NElement first = opb.first().orNull();
                boolean cc = false;
                if (first != null) {
                    NBooleanRef r = NBooleanRef.of(false);
                    NElement u = processControlElements(first, r);
                    if (r.get()) {
                        opb.first(u);
                        cc = true;
                    }
                }
                NElement second = opb.first().orNull();
                if (second != null) {
                    NBooleanRef r = NBooleanRef.of(false);
                    NElement u = processControlElements(second, r);
                    if (r.get()) {
                        opb.second(u);
                        cc = true;
                    }
                }
                if (changesInAnnotation) {
                    opb.clearAnnotations().addAnnotations(annotations);
                    cc = true;
                }
                if (cc) {
                    someChanges.set(true);
                    return opb.build();
                }
                return op;
            }
        }
        switch (child.type()) {
            case OBJECT:
            case NAMED_OBJECT:
            case PARAMETRIZED_OBJECT:
            case NAMED_PARAMETRIZED_OBJECT: {
                NObjectElement p = child.asObject().get();
                List<NElement> i = p.params().orNull();
                NObjectElementBuilder builder = p.builder();
                boolean anyChange = false;
                if (i != null) {
                    NBooleanRef u = NRef.ofBoolean(false);
                    List<NElement> i2 = processControlElements(i, u);
                    if (u.get()) {
                        builder.setParams(i2);
                        anyChange = true;
                    }
                }

                i = p.children();
                if (i != null) {
                    NBooleanRef u = NRef.ofBoolean(false);
                    List<NElement> i2 = processControlElements(i, u);
                    if (u.get()) {
                        builder.setChildren(i2);
                        anyChange = true;
                    }
                }
                if (changesInAnnotation) {
                    builder.clearAnnotations().addAnnotations(annotations);
                    anyChange = true;
                }
                if (anyChange) {
                    someChanges.set(true);
                    p = builder.build();
                }
                return p;
            }
            case ARRAY:
            case NAMED_ARRAY:
            case PARAMETRIZED_ARRAY:
            case NAMED_PARAMETRIZED_ARRAY: {
                NArrayElement p = child.asArray().get();
                List<NElement> i = p.params().orNull();
                NArrayElementBuilder builder = p.builder();
                boolean anyChange = false;
                if (i != null) {
                    NBooleanRef u = NRef.ofBoolean(false);
                    List<NElement> i2 = processControlElements(i, u);
                    if (u.get()) {
                        builder.setParams(i2);
                        anyChange = true;
                    }
                }

                i = p.children();
                if (i != null) {
                    NBooleanRef u = NRef.ofBoolean(false);
                    List<NElement> i2 = processControlElements(i, u);
                    if (u.get()) {
                        builder.setChildren(i2);
                        anyChange = true;
                    }
                }
                if (changesInAnnotation) {
                    builder.clearAnnotations().addAnnotations(annotations);
                    anyChange = true;
                }
                if (anyChange) {
                    someChanges.set(true);
                    p = builder.build();
                }
                return p;
            }
            case UPLET:
            case NAMED_UPLET: {
                NUpletElement p = child.asUplet().get();
                List<NElement> i = p.params();
                NUpletElementBuilder builder = p.builder();
                boolean anyChange = false;
                if (i != null) {
                    NBooleanRef u = NRef.ofBoolean(false);
                    List<NElement> i2 = processControlElements(i, u);
                    if (u.get()) {
                        builder.setParams(i2);
                        anyChange = true;
                    }
                }
                if (changesInAnnotation) {
                    builder.clearAnnotations().addAnnotations(annotations);
                    anyChange = true;
                }
                if (anyChange) {
                    someChanges.set(true);
                    p = builder.build();
                }
                return p;
            }
        }
        return child;
    }


    public NOptional<NElement> parseElements(NElement rawElements) {
        try {
            rawElements = processControlElements(rawElements, NBooleanRef.of(false));
            return NOptional.of(rawElements);
        } catch (Exception ex) {
            engine.log().log(NMsg.ofC("error parsing tson document %s", rawElements).asError());
            return NOptional.ofNamedError("error parsing tson document", ex);
        }

    }

    public NOptional<NElement> parseInputStream(InputStream is, NTxSource source) {
        NElement u;
        try {
            u = NElementReader.ofTson().read(is);
            u = NTxUtils.addCompilerDeclarationPathAnnotations(u, source.path().map(NPath::toString).orNull());
        } catch (Exception ex) {
            engine.log().log(NMsg.ofC("error loading tson document %s", is).asError(),source);
            return NOptional.ofNamedError("error loading tson document", ex);
        }
        return parseElements(u);
    }

    public NOptional<NElement> parsePath(NPath path, NTxSource source) {
        NElement u;
        try {
            u = NElementReader.ofTson().read(path);
            u = NTxUtils.addCompilerDeclarationPathAnnotations(u, source.path().map(NPath::toString).orNull());
        } catch (Exception ex) {
            engine.log().log(NMsg.ofC("error loading tson document %s : %s", path,ex).asError(),source);
            return NOptional.ofNamedError(NMsg.ofC("error loading tson document %s : %s", path, ex), ex);
        }
        return parseElements(u);
    }
}
