package net.thevpc.ntexup.api.document.node;

import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.elem2d.NTxAlign;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxStyleRule;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NToElement;
import net.thevpc.nuts.util.NOptional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public interface NTxNode extends NTxItem {
    String uuid();

    NTxNode setUuid(String uuid);

    String[] getStyleClasses();

    NTxItem setStyleClasses(String[] classNames);

    boolean append(NTxItem a);

    NTxSource source();

    NTxNode setSource(NTxSource source);

    boolean isDisabled();

    NTxNode setDisabled(boolean enabled);

    String name();

    String type();

    List<NTxProp> props();

    NOptional<NElement> getVar(String property);

    Map<String, NElement> getVars();

    NTxNode setVar(String name, NElement value);

    NOptional<NElement> getPropertyValue(String... propertyNames);

    NOptional<NTxProp> getProperty(String... propertyNames);

    List<NTxProp> getProperties();


    NTxNode setName(String name);

    NTxNode setProperty(NTxProp s);

    NTxNode setProperty(String name, NElement value);

    NTxNode setProperty(String name, NToElement value);

    NTxNode setProperties(NTxProp... props);

    NTxNode addStyleClass(String className);

    NTxNode addStyleClasses(String... classNames);

    NTxNode removeClass(String className);

    boolean hasClass(String className);

    Set<String> styleClasses();

    NTxNode unsetProperty(String s);

    NTxNode setParent(NTxItem parent);

    NTxNode mergeNode(NTxItem other);

    NTxNode add(NTxItem other);

    NTxNode addAll(NTxItem... other);

    NTxNode mergeNodes(NTxItem... other);

    NTxNode setPosition(NTxAlign align);

    NTxNode setPosition(Number x, Number y);

    NTxNode setPosition(NTxDouble2 d);

    NTxNode setOrigin(NTxAlign align);

    NTxNode setOrigin(Number x, Number y);

    NTxNode setOrigin(NTxDouble2 d);

    NTxNode at(NTxAlign align);

    NTxNode at(Number x, Number y);

    NTxNode at(NTxDouble2 d);

    NTxNode setSize(Number size);

    NTxNode setSize(NTxDouble2 size);

    NTxNode setSize(Number w, Number h);

    NTxNode setFontSize(Number w);

    NTxNode setFontFamily(String w);

    NTxNode setFontBold(Boolean w);

    NTxNode setFontItalic(Boolean w);

    NTxNode setFontUnderlined(Boolean w);

    NTxNode setFontStrike(Boolean w);

    NTxNode setForegroundColor(String w);

    NTxNode setBackgroundColor(String w);

//    NTxNode setLineColor(String w);

    NTxNode setGridColor(String w);

    List<NTxNode> children();

    NTxNode addChild(NTxNode a);

    NTxNode addChildren(NTxNode... a);

    NTxNode setChildren(NTxNode... a);


    NTxNode addRule(NTxStyleRule s);

    NTxNode removeRule(NTxStyleRule s);

    NTxNode addRules(NTxStyleRule... s);

    NTxNode clearChildren();

    NTxNode clearDefinitions();

    NTxNode clearRules();

    NTxStyleRule[] rules();

    NTxNode copyTo(NTxNode o);

    NTxNode copyFrom(NTxNode o);

    NTxNode copy();

    String getName();

    String getComponentName();

    NTxNode setRules(NTxStyleRule[] rules);

    void setChildAt(int i, NTxNode c);

    NTxNodeDef[] definitions();

    NTxNode addDefinition(NTxNodeDef s);

    NTxNode addDefinitions(NTxNodeDef... definitions);

    NTxNode removeNodeDefinition(NTxNodeDef s);

    NTxFunction[] nodeFunctions();

    NTxNode addNodeFunction(NTxFunction s);

    NTxNode addNodeFunctions(NTxFunction... definitions);

    NTxNode removeNodeFunction(String name);

    NTxNode reset();

    NTxNode addHierarchy(NTxNode n);

    NTxNode removeHierarchy(NTxNode n);

    List<NTxNode> hierarchy();

    NTxNode setTemplateDefinition(NTxNodeDef n);

    NTxNodeDef templateDefinition();

    NTxNode setUserObject(String name, Object value);

    NOptional<Object> getUserObject(String property);

    <T> NOptional<T> getAndSetUserObject(String property, boolean force, Supplier<T> defaultValue);

    <T> NOptional<T> getAndSetUserObject(Class<T> property, boolean force, Supplier<T> defaultValue);

    NTxNode setRenderCache(String name, Object value);

    NOptional<Object> getRenderCache(String property);

    <T> NOptional<T> getAndSetRenderCache(String property, boolean force, Supplier<T> defaultValue);

    <T> NOptional<T> getAndSetRenderCache(Class<T> property, boolean force, Supplier<T> defaultValue);

    void invalidateRenderCache();
}
