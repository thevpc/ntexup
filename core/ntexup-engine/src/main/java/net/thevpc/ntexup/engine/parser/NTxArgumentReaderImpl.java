package net.thevpc.ntexup.engine.parser;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.parser.NTxAllArgumentReader;
import net.thevpc.ntexup.api.parser.NTxArgumentReader;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.elem.NElement;

import java.util.*;

public class NTxArgumentReaderImpl implements NTxArgumentReader, NTxAllArgumentReader {
    private String id;
    private String uid;
    private NElement element;
    private NTxNode node;
    private NElement[] arguments;
    private List<NElement> availableArguments;
    private NTxDocumentFactory f;
    private NTxResolutionContext context;
    private Map<String, Object> props = new HashMap<>();
    private int currentIndex = 0;

    @Override
    public NTxSource source() {
        return parseContext().source();
    }

    @Override
    public String getId() {
        return id;
    }

    public NTxArgumentReader setId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public String getUid() {
        return uid;
    }

    public NTxArgumentReader setUid(String uid) {
        this.uid = uid;
        return this;
    }

    @Override
    public NElement element() {
        return element;
    }

    public NTxArgumentReader setElement(NElement tsonElement) {
        this.element = tsonElement;
        return this;
    }

    @Override
    public NTxNode node() {
        return node;
    }

    public NTxArgumentReader setNode(NTxNode node) {
        this.node = node;
        return this;
    }

    @Override
    public NElement[] allArguments() {
        return Arrays.copyOf(arguments, arguments.length);
    }

    @Override
    public int availableCount() {
        return availableArguments.size();
    }

    public List<NElement> availableArguments() {
        return Collections.unmodifiableList(availableArguments);
    }

    public NTxArgumentReader setArguments(NElement[] arguments) {
        this.arguments = arguments;
        this.availableArguments = new ArrayList<>(Arrays.asList(arguments));
        return this;
    }

    @Override
    public boolean isEmpty() {
        return availableArguments.isEmpty();
    }

    @Override
    public NElement peek() {
        if (isEmpty()) {
            return null;
        }
        return availableArguments.get(0);
    }

    @Override
    public NElement read() {
        if (isEmpty()) {
            return null;
        }
        NElement n = availableArguments.remove(0);
        currentIndex++;
        return n;
    }

    @Override
    public NTxDocumentFactory documentFactory() {
        return f;
    }

    public NTxArgumentReader setF(NTxDocumentFactory f) {
        this.f = f;
        return this;
    }

    @Override
    public NTxResolutionContext parseContext() {
        return context;
    }

    public NTxArgumentReader setContext(NTxResolutionContext context) {
        this.context = context;
        return this;
    }

    @Override
    public Map<String, Object> props() {
        return props;
    }

    public NTxArgumentReader setProps(Map<String, Object> props) {
        this.props = props;
        return this;
    }

    @Override
    public int currentIndex() {
        return currentIndex;
    }
}
