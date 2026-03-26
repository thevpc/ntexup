/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.engine.document;

import java.util.*;

import net.thevpc.ntexup.api.document.NTxDocumentClass;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.engine.util.NTxUtilsPages;
import net.thevpc.nuts.util.NOptional;

/**
 * @author vpc
 */
public class DefaultNTxDocument implements NTxDocument, Cloneable {

    private NTxDocumentClass documentClass;
    private Properties properties = new Properties();
    private DefaultNTxNode root;
    private final NTxSource source;

    public static class NamedPart {
        public String name;
        public Object value;

        public NamedPart(String name, Object value) {
            this.name = name;
            this.value = value;
        }
    }

    public DefaultNTxDocument(NTxSource source) {
        this.source = source;
        root = new DefaultNTxNode(NTxNodeType.PAGE_GROUP, source);
    }


    @Override
    public NTxSource source() {
        return source;
    }



    @Override
    public NTxDocumentClass documentClass() {
        return documentClass;
    }

    @Override
    public void setDocumentClass(NTxDocumentClass documentClass) {
        this.documentClass = documentClass;
    }

    @Override
    public NTxNode root() {
        return root;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public NOptional<String> name() {
        return getProperty("name");
    }

    @Override
    public NTxDocument setProperty(String name, String value) {
        if (value == null) {
            properties.remove(name);
        } else {
            properties.setProperty(name, value);
        }
        return this;
    }

    @Override
    public NTxDocument add(NTxNode part) {
        root().addChild(part);
        return this;
    }

    @Override
    public NOptional<String> getProperty(String name) {
        return NOptional.ofNamed(properties.getProperty(name), name);
    }

    @Override
    public void mergeDocument(NTxDocument other) {
        if (other != null) {
            if (documentClass == null) {
                documentClass = other.documentClass();
            } else {
                //ignore!
            }
            for (Map.Entry<Object, Object> e : other.getProperties().entrySet()) {
                if (!properties.containsKey(e.getKey())) {
                    properties.setProperty((String) e.getKey(), (String) e.getValue());
                } else {
                    //ignore
                }
            }
            this.root().mergeNode(other.root());
        }
    }

    @Override
    public NTxDocument copy() {
        DefaultNTxDocument cloned;
        try {
            cloned = (DefaultNTxDocument) this.clone();
        } catch (CloneNotSupportedException ex) {
            throw new IllegalArgumentException(ex);
        }
        if (cloned.root != null) {
            cloned.root = (DefaultNTxNode) cloned.root.copy();
        }
        if (cloned.properties != null) {
            cloned.properties = new Properties();
            cloned.properties.putAll(properties);
        }
        return cloned;
    }

    @Override
    public List<NTxNode> pages() {
        return NTxUtilsPages.resolvePages(this);
    }

}
